package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionableAsset
import com.cordutch.states.TransactionAndStateId
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Flow to self-issue an auctionable asset with given description.
 */
@InitiatingFlow
@StartableByRPC
class IssueAssetFlow(private val description: String) : FlowLogic<TransactionAndStateId>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : TransactionAndStateId {
        val asset = AuctionableAsset(description, ourIdentity, ourIdentity)
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        Command(AuctionableAssetContract.Commands.Issue(), ourIdentity.owningKey),
                        StateAndContract(asset, AuctionableAssetContract.ID)
                )
        builder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)

        val stx = subFlow(FinalityFlow(signedTx, listOf()))
        return TransactionAndStateId(stx, asset.linearId)
    }
}
