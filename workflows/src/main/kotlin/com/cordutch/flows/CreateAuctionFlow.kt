package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionContract
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


/**
 * Flow to create a dutch auction of a given [assetId] between a set of [bidders].
 * Locks the asset from being transferred or consumed for the duration of the auction.
 * The price starts at [price] and can be decreased by the auction owner.
 */
@InitiatingFlow
@StartableByRPC
class CreateAuctionFlow(private val assetId: UniqueIdentifier, private val price: Amount<Currency>, private val bidders: List<Party>)
    : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(assetId))
        val queryStates = serviceHub.vaultService.queryBy<AuctionableAsset>(criteria).states
        if (queryStates.size != 1) throw IllegalArgumentException("Asset id does not uniquely refer to an existing asset")
        val oldAsset = queryStates.single()
        val lockedAsset = oldAsset.state.data.lock()

        val auction = AuctionState(lockedAsset, ourIdentity, bidders, price)
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        oldAsset,
                        Command(AuctionableAssetContract.Commands.Lock(), lockedAsset.owner.owningKey),
                        StateAndContract(lockedAsset, AuctionableAssetContract.ID),
                        Command(AuctionContract.Commands.Create(), auction.participants.map { it.owningKey }),
                        StateAndContract(auction, AuctionContract.ID)
                )
        builder.verify(serviceHub)
        val initialTx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        val otherSessions = auction.bidders.map { initiateFlow(it) }
        val signedTx = subFlow(CollectSignaturesFlow(initialTx, otherSessions))
        return subFlow(FinalityFlow(signedTx, otherSessions))
    }
}

@InitiatedBy(CreateAuctionFlow::class)
class CreateAuctionResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputs = stx.tx.outputsOfType<AuctionState>()
                "This must be an auction transaction" using (outputs.size == 1)
                "We must be a bidder" using (ourIdentity in outputs.single().bidders)
            }
        }
        val signedTx = subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, signedTx.id, StatesToRecord.ALL_VISIBLE))
    }
}