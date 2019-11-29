package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionContract
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionResponse
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant


/**
 * Flow to create a dutch auction of a given [assetId] between a set of [bidders].
 * Locks the asset from being transferred or consumed for the duration of the auction.
 * The price starts at [price] and can be decreased by the auction owner.
 */
@InitiatingFlow
@StartableByRPC
class CreateAuctionFlow(
        private val assetId: UniqueIdentifier,
        private val price: Amount<IssuedTokenType>,
        private val bidders: List<Party>,
        private val decrement: Amount<TokenType>,
        private val period: Long,
        private val startTime: Instant = Instant.now()
) : FlowLogic<AuctionResponse>() {

    @Suspendable
    override fun call(): AuctionResponse {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(assetId))
        val queryStates = serviceHub.vaultService.queryBy<AuctionableAsset>(criteria).states
        if (queryStates.size != 1) throw IllegalArgumentException("Asset id does not uniquely refer to an existing asset")
        val oldAsset = queryStates.single()
        val lockedAsset = oldAsset.state.data.lock()
        val assetOwner = serviceHub.identityService.requireWellKnownPartyFromAnonymous(lockedAsset.owner)
        val otherParties = if (assetOwner == ourIdentity) bidders else bidders + assetOwner
        val otherSessions = otherParties.map { initiateFlow(it) }
        val identities = subFlow(SwapIdentitiesFlow(otherSessions))
        val ourAnonymousIdentity = identities[ourIdentity]!!

        val auction = AuctionState(
                lockedAsset.linearId,
                ourAnonymousIdentity,
                bidders.map { identities[it]!! },
                price,
                decrement,
                period,
                startTime
        )
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        oldAsset,
                        Command(AuctionableAssetContract.Commands.Lock(), lockedAsset.owner.owningKey),
                        StateAndContract(lockedAsset, AuctionableAssetContract.ID),
                        Command(AuctionContract.Commands.Create(), (auction.participants + auction.bidders).map { it.owningKey }),
                        StateAndContract(auction, AuctionContract.ID)
                )
        builder.verify(serviceHub)
        val initialTx = if (assetOwner == ourIdentity) {
            serviceHub.addSignature(serviceHub.signInitialTransaction(builder, ourAnonymousIdentity.owningKey), lockedAsset.owner.owningKey)
        } else {
            serviceHub.signInitialTransaction(builder, ourAnonymousIdentity.owningKey)
        }

        val myKeys = if (assetOwner == ourIdentity) listOf(ourAnonymousIdentity.owningKey, assetOwner.owningKey) else listOf(ourAnonymousIdentity.owningKey)
        val signedTx = subFlow(CollectSignaturesFlow(initialTx, otherSessions, myKeys))
        val finalisedTx = subFlow(FinalityFlow(signedTx, otherSessions))
        return AuctionResponse(finalisedTx, auction.linearId, otherParties)
    }
}

@InitiatedBy(CreateAuctionFlow::class)
class CreateAuctionResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        subFlow(SwapIdentitiesFlow(listOf(flowSession)))
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputs = stx.tx.outputsOfType<AuctionState>()
                "This must be an auction transaction" using (outputs.size == 1)
            }
        }
        val signedTx = subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, signedTx.id, StatesToRecord.ALL_VISIBLE))
    }
}