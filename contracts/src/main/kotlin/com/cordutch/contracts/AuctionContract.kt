package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCashBy

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
        class Decrease : TypeOnlyCommandData(), Commands
        class End : TypeOnlyCommandData(), Commands
        class Bid : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand <Commands>()
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
                "Must reference correct asset" using (output.asset == asset)
                val requiredSigners = output.participants.map { it.owningKey }.toSet()
                "All participants must sign" using (command.signers.toSet() == requiredSigners)
            }
            is Commands.Decrease -> {
                requireThat {
                    "An auction decrease transaction must have one input state." using (tx.inputStates.size == 1)
                    "An auction decrease transaction must have one output state." using (tx.outputStates.size == 1)
                    val input = tx.inputStates.single() as AuctionState
                    val output = tx.outputStates.single() as AuctionState
                    "Only the price may change" using (output == input.withNewPrice(output.price))
                    "The price must decrease" using (output.price < input.price)
                    "The new price must be greater than zero" using (output.price > Amount(0, output.price.token))
                    "The owner must sign" using (listOf(output.owner.owningKey) == command.signers)
                }
            }
            is Commands.End -> {
                requireThat {
                    "An end auction transaction must have one input state." using (tx.inputsOfType<AuctionState>().size == 1)
                    val input = tx.inputsOfType<AuctionState>().single()
                    "An end auction transaction must have no outputs" using tx.outputsOfType<AuctionState>().isEmpty()
                    "The owner must sign" using (listOf(input.owner.owningKey) == command.signers)
                    tx.commands.requireSingleCommand<AuctionableAssetContract.Commands.Unlock>()
                    val asset = tx.inputsOfType<AuctionableAsset>().singleOrNull()
                    "Must reference correct asset" using (input.asset == asset)
                }
            }
            is Commands.Bid -> {
                requireThat {
                    "A bid transaction must have one input state." using (tx.inputsOfType<AuctionState>().size == 1)
                    val input = tx.inputsOfType<AuctionState>().single()
                    "A bid transaction must have no outputs" using tx.outputsOfType<AuctionState>().isEmpty()
                    val cashOutputs = tx.outputsOfType<Cash.State>()
                    "There must be output cash paid to the owner." using cashOutputs.any { it.owner == input.owner }
                    val cashPaid = tx.outputStates.sumCashBy(input.owner).withoutIssuer()
                    "The exact amount must be paid" using (input.price == cashPaid)
                    "The signer must be a bidder" using input.bidders.map { it.owningKey }.containsAll(command.signers)
                    tx.commands.requireSingleCommand<AuctionableAssetContract.Commands.Unlock>()
                    val asset = tx.inputsOfType<AuctionableAsset>().singleOrNull()
                    "Must reference correct asset" using (input.asset == asset)
                }
            }
        }
    }
}