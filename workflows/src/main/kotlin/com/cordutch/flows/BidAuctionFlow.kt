package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionContract
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountWithIssuerCriteria
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant


/**
 * Flow for a given bidder to bid on the [auctionId] and hence purchase the associated asset at the price of the auction at this time.
 * Also unlocks the asset and transfers the ownership to the initiator of this flow. This is a terminal operation of the auction.
 */
@InitiatingFlow
@StartableByRPC
class BidAuctionFlow(val auctionId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val auctionCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(auctionId))
        val auctionResults = serviceHub.vaultService.queryBy<AuctionState>(auctionCriteria).states
        if (auctionResults.size != 1) throw IllegalArgumentException("Auction id does not uniquely refer to an existing auction")
        val auction = auctionResults.single()
        val auctionState = auction.state.data
        val ourAnonymousIdentity = auctionState.bidders.first { serviceHub.identityService.wellKnownPartyFromAnonymous(it) == ourIdentity }

        val assetCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(auctionState.assetId))
        val asset = serviceHub.vaultService.queryBy<AuctionableAsset>(assetCriteria).states.single()

        val timeNow = Instant.now()
        val periods = (timeNow.toEpochMilli() - auctionState.startTime.toEpochMilli()) / auctionState.period
        val priceNow = auctionState.price.withoutIssuer() - (auctionState.decrement * periods)

        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        auction,
                        Command(AuctionContract.Commands.Bid(), ourAnonymousIdentity.owningKey),
                        asset,
                        Command(AuctionableAssetContract.Commands.Unlock(), ourAnonymousIdentity.owningKey),
                        StateAndContract(asset.state.data.unlock().withNewOwner(ourAnonymousIdentity), AuctionableAssetContract.ID),
                        TimeWindow.fromOnly(timeNow)
                )
        val tokenCriteria = tokenAmountWithIssuerCriteria(auctionState.price.token.tokenType, auctionState.price.token.issuer)
        val builderWithTokens = addMoveFungibleTokens(builder, serviceHub, priceNow, auctionState.owner, ourAnonymousIdentity, tokenCriteria)
        val ourSigningKeys = builderWithTokens.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub)
        builderWithTokens.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(builderWithTokens, ourSigningKeys)

        // The auction owner is responsible for informing the other bidders
        val otherSession = initiateFlow(serviceHub.identityService.requireWellKnownPartyFromAnonymous(auctionState.owner))
        return subFlow(FinalityFlow(signedTx, otherSession))
    }
}

@InitiatedBy(BidAuctionFlow::class)
class BidAuctionResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(flowSession))
    }
}