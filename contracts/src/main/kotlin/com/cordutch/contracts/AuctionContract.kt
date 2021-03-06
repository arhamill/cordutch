package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStatesOrZero
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * Contract governing the lifecycle of a dutch auction.
 * An auction is created then decreased in price until one bidder bids and purchases the auctioned asset at the current price.
 */
class AuctionContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cordutch.contracts.AuctionContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class End : TypeOnlyCommandData(), Commands
        class Bid : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when(command.value) {
            is Commands.Create -> requireThat {
                "No auction inputs should be consumed when creating an auction" using tx.inputsOfType<AuctionState>().isEmpty()
                "Only one auction output state should be created when creating an Auction." using (tx.outputsOfType<AuctionState>().size == 1)
                val output = tx.outputsOfType<AuctionState>().single()
                "The owner must not be a bidder" using (output.owner !in output.bidders)
                "There must be at least one bidder" using !output.bidders.isEmpty()
                "The start price should be greater than zero" using
                        (output.price > Amount(0, output.price.token))
                tx.commands.requireSingleCommand<AuctionableAssetContract.Commands.Lock>()
                val asset = tx.outputsOfType<AuctionableAsset>().singleOrNull()
                "Must reference correct asset" using (output.assetId == asset?.linearId)
                val requiredSigners = (output.participants + output.bidders).map { it.owningKey }.toSet()
                "All participants must sign" using (command.signers.toSet() == requiredSigners)
            }
            is Commands.End -> {
                requireThat {
                    "An end auction transaction must have one input state." using (tx.inputsOfType<AuctionState>().size == 1)
                    val input = tx.inputsOfType<AuctionState>().single()
                    "An end auction transaction must have no outputs" using tx.outputsOfType<AuctionState>().isEmpty()
                    "The owner must sign" using (listOf(input.owner.owningKey) == command.signers)
                    tx.commands.requireSingleCommand<AuctionableAssetContract.Commands.Unlock>()
                    val asset = tx.inputsOfType<AuctionableAsset>().singleOrNull()
                    "Must reference correct asset" using (input.assetId == asset?.linearId)
                }
            }
            is Commands.Bid -> {
                requireThat {
                    "A bid transaction must have one input state." using (tx.inputsOfType<AuctionState>().size == 1)
                    val input = tx.inputsOfType<AuctionState>().single()
                    "A bid transaction must have no outputs" using tx.outputsOfType<AuctionState>().isEmpty()
                    val tokens = tx.outputsOfType<FungibleToken>().filter { it.holder == input.owner }
                    val paid = tokens.sumTokenStatesOrZero(input.price.token)
                    val periods = (input.price - paid).withoutIssuer().quantity / input.decrement.quantity
                    val expectedTime = input.startTime.toEpochMilli() + (periods * input.period)
                    val timeWindow = tx.timeWindow
                    "The transaction must be time windowed" using (timeWindow?.fromTime != null)
                    "The time window must be correct" using (timeWindow!!.fromTime!!.toEpochMilli() >= expectedTime)
                    "The signer must be a bidder" using input.bidders.map { it.owningKey }.containsAll(command.signers)
                    tx.commands.requireSingleCommand<AuctionableAssetContract.Commands.Unlock>()
                    val asset = tx.inputsOfType<AuctionableAsset>().singleOrNull()
                    "Must reference correct asset" using (input.assetId == asset?.linearId)
                }
            }
        }
    }
}