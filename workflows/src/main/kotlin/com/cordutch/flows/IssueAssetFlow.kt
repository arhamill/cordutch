package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Flow to self-issue an auctionable asset with given description.
 */
@StartableByRPC
@StartableByService
class IssueAssetFlow(private val description: String) : FlowLogic<AssetResponse>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : AssetResponse {
        val asset = AuctionableAsset(description, ourIdentity, ourIdentity)
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        Command(AuctionableAssetContract.Commands.Issue(), ourIdentity.owningKey),
                        StateAndContract(asset, AuctionableAssetContract.ID)
                )
        builder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)

        // We are the only participant, no need to send to anyone else
        val finalTx = subFlow(FinalityFlow(signedTx, listOf()))
        return AssetResponse(finalTx, asset.linearId)
    }
}

@CordaSerializable
data class AssetResponse(val stx: SignedTransaction, val id: UniqueIdentifier)