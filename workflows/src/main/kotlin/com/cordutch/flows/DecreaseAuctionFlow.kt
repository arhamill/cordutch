package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import com.cordutch.contracts.AuctionContract
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

/**
 * Flow to decrease the price of a given [auctionId].
 * Once the price has been decreased, any bid at the new price will be accepted.
 */
@InitiatingFlow
@StartableByRPC
class DecreaseAuctionFlow(private val auctionId: UniqueIdentifier, private val newPrice: Amount<IssuedTokenType>)
    : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(auctionId))
        val queryStates = serviceHub.vaultService.queryBy<AuctionState>(criteria).states
        if (queryStates.size != 1) throw IllegalArgumentException("Auction id does not uniquely refer to an existing auction")
        val oldAuction = queryStates.single()

        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.single())
                .withItems(
                        oldAuction,
                        Command(AuctionContract.Commands.Decrease(), oldAuction.state.data.owner.owningKey),
                        StateAndContract(oldAuction.state.data.withNewPrice(newPrice), AuctionContract.ID)
                )
        builder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(builder, oldAuction.state.data.owner.owningKey)

        // Inform the bidders via a service later
        return subFlow(FinalityFlow(signedTx, listOf()))
    }
}