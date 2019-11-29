package com.cordutch.services

import com.cordutch.contracts.AuctionContract
import com.cordutch.flows.InformTransactionFlow
import com.cordutch.states.AuctionState
import net.corda.core.node.AppServiceHub
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.WireTransaction
import rx.schedulers.Schedulers

@CordaService
class AuctionInformerService(val services: AppServiceHub) : SingletonSerializeAsToken() {

    init {
        services.validatedTransactions.track()
                .updates
                .observeOn(Schedulers.io())
                .subscribe()
                { stx ->
                    val auctionCommand = stx.tx.commands.firstOrNull { it.value is AuctionContract.Commands }
                    if (auctionCommand != null) {
                        when(auctionCommand.value as AuctionContract.Commands) {
                            is AuctionContract.Commands.Bid -> handleBid(stx.tx)
                            is AuctionContract.Commands.End -> handleBid(stx.tx)
                        }
                    }
                }
    }

    private fun handleBid(tx: WireTransaction) {
        val auction = services.loadStates(tx.inputs.toSet()).first { it.state.data is AuctionState }.state.data as AuctionState
        if (services.identityService.wellKnownPartyFromAnonymous(auction.owner) in services.myInfo.legalIdentities) {
            auction.bidders.forEach {
                val wellKnownBidder = services.identityService.requireWellKnownPartyFromAnonymous(it)
                services.startFlow(InformTransactionFlow(tx.id, wellKnownBidder, StatesToRecord.ONLY_RELEVANT.ordinal))
            }
        }
    }
}