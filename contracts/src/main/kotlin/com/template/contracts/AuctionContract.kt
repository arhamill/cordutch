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
                "The start price should be greater than the reserve" using (output.startPrice > output.reservePrice)
                "There should be at least one decrement" using
                        (output.decrement < (output.startPrice - output.reservePrice))
                val currency = output.startPrice.token
                "All prices should be in the same currency" using
                        (output.reservePrice.token == currency && output.decrement.token == currency)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
    }
}