package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionContract
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Flow for the auction owner to end the given [auctionId] and unlock the associated asset.
 * This is a terminal operation of the auction.
 */
@InitiatingFlow
@StartableByRPC
class EndAuctionFlow(val auctionId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val auctionCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(auctionId))
        val auctionResults = serviceHub.vaultService.queryBy<AuctionState>(auctionCriteria).states
        if (auctionResults.size != 1) throw IllegalArgumentException("Auction id does not uniquely refer to an existing auction")
        val auction = auctionResults.single()
        val auctionState = auction.state.data

        val assetCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(auctionState.assetId))
        val asset = serviceHub.vaultService.queryBy<AuctionableAsset>(assetCriteria).states.single()
        val assetOwner = asset.state.data.owner

        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        auction,
                        Command(AuctionContract.Commands.End(), auctionState.owner.owningKey),
                        asset,
                        Command(AuctionableAssetContract.Commands.Unlock(), assetOwner.owningKey),
                        StateAndContract(asset.state.data.unlock(), AuctionableAssetContract.ID)
                )
        val wellKnownAssetOwner = serviceHub.identityService.requireWellKnownPartyFromAnonymous(assetOwner)
        val initialTx = serviceHub.signInitialTransaction(builder, auctionState.owner.owningKey)
        return if (wellKnownAssetOwner == ourIdentity) {
            val signedTx = serviceHub.addSignature(initialTx, assetOwner.owningKey)
            subFlow(FinalityFlow(signedTx, listOf()))
        } else {
            val otherSession = initiateFlow(wellKnownAssetOwner)
            val signedTx = subFlow(CollectSignaturesFlow(initialTx, listOf(otherSession)))
            subFlow(FinalityFlow(signedTx, otherSession))
        }
    }
}

@InitiatedBy(EndAuctionFlow::class)
class EndAuctionFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // put something here
            }
        }
        val signedTx = subFlow(signTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, signedTx.id))
    }
}