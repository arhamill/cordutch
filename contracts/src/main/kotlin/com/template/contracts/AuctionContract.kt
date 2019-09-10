package com.template.contracts

import com.template.states.AuctionState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class AuctionContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AuctionContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Decrease : TypeOnlyCommandData(), Commands
        class End : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand <Commands>()
        when(command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when creating an auction" using tx.inputs.isEmpty()
                "Only one output state should be created when creating an Auction." using (tx.outputs.size == 1)
                val output = tx.outputStates.single() as AuctionState
                "The asset must have a description" using !output.assetDescription.isEmpty()
                "The owner must not be a bidder" using (output.owner !in output.bidders)
                "There must be at least one bidder" using !output.bidders.isEmpty()
                "The start price should be greater than zero" using
                        (output.price > Amount(0, output.price.token))
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
                    "An end auction transaction must have one input state." using (tx.inputStates.size == 1)
                    val input = tx.inputStates.single() as AuctionState
                    "An end auction transaction must have no outputs" using tx.outputStates.isEmpty()
                    "The owner must sign" using (listOf(input.owner.owningKey) == command.signers)
                }
            }
        }
    }
}