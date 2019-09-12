package com.template.contracts

import com.template.states.AuctionableAsset
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class AuctionableAssetContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AuctionableAssetContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Consume: TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand <Commands>()
        when(command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an asset." using tx.inputs.isEmpty()
                "Only one output state should be created when issuing an asset." using (tx.outputs.size == 1)
                val output = tx.outputStates.single() as AuctionableAsset
                "The asset must have a description" using !output.description.isEmpty()
                "The asset must be unlocked" using !output.locked
                val requiredSigners = output.participants.map { it.owningKey }.toSet()
                "All participants must sign" using (command.signers.toSet() == requiredSigners)
            }
            is Commands.Transfer -> requireThat {
                "An transfer transaction should only consume one input state." using (tx.inputStates.size == 1)
                "An transfer transaction should only create one output state." using (tx.outputStates.size == 1)
                val input = tx.inputStates.single() as AuctionableAsset
                val output = tx.outputStates.single() as AuctionableAsset
                "The asset must be unlocked" using !input.locked
                "Only the owner property may change." using (output == input.withNewOwner(output.owner))
                "The owner property must change in a transfer." using (input.owner != output.owner)
                "The old owner must sign" using (command.signers == listOf(input.owner.owningKey))
            }
            is Commands.Consume -> requireThat {
                "A consume transaction should only consume one input state." using (tx.inputStates.size == 1)
                "A consume transaction should have no output states" using tx.outputStates.isEmpty()
                val input = tx.inputStates.single() as AuctionableAsset
                "The asset must be unlocked" using !input.locked
                val requiredSigners = input.participants.map { it.owningKey }.toSet()
                "All participants must sign" using (command.signers.toSet() == requiredSigners)
            }
        }
    }
}