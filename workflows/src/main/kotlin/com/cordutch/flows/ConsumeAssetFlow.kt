package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionableAssetContract
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ConsumeAssetFlow(private val assetId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(assetId))
        val queryStates = serviceHub.vaultService.queryBy<AuctionableAsset>(criteria).states
        if (queryStates.size != 1) throw IllegalArgumentException("Asset id does not uniquely refer to an existing asset")
        val asset = queryStates.single()
        val issuer = asset.state.data.issuer

        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        asset,
                        Command(AuctionableAssetContract.Commands.Consume(), listOf(ourIdentity.owningKey, issuer.owningKey))
                )
        builder.verify(serviceHub)
        val initialTx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        val otherSession = initiateFlow(issuer)

        val signedTx = subFlow(CollectSignaturesFlow(initialTx, listOf(otherSession)))
        return subFlow(FinalityFlow(signedTx, otherSession))
    }
}

@InitiatedBy(ConsumeAssetFlow::class)
class ConsumeAssetResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                "There must be no outputs" using stx.tx.outputs.isEmpty()
            }
        }
        val signedTx = subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, signedTx.id))
    }
}