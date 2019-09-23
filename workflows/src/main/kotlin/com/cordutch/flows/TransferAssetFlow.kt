package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class TransferAssetFlow(private val assetId: UniqueIdentifier, private val newOwner: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(assetId))
        val queryStates = serviceHub.vaultService.queryBy<AuctionableAsset>(criteria).states
        if (queryStates.size != 1) throw IllegalArgumentException("Asset id does not uniquely refer to an existing asset")
        val oldAsset = queryStates.single()

        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        oldAsset,
                        Command(AuctionableAssetContract.Commands.Transfer(), ourIdentity.owningKey),
                        StateAndContract(oldAsset.state.data.withNewOwner(newOwner), AuctionableAssetContract.ID)
                )
        builder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        val otherSession = initiateFlow(newOwner)

        // New owner doesn't need to sign
        return subFlow(FinalityFlow(signedTx, otherSession))
    }
}

@InitiatedBy(TransferAssetFlow::class)
class TransferAssetResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(flowSession))
    }
}