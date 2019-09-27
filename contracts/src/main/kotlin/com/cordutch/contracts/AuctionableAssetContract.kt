package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction

/**
 * Contract governing the lifecycle of an auctionable asset.
 * The asset is locked and unlocked to prevent spend during the duration of an auction.
 */
class AuctionableAssetContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cordutch.contracts.AuctionableAssetContract"
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Consume: TypeOnlyCommandData(), Commands
        class Lock: TypeOnlyCommandData(), Commands
        class Unlock: TypeOnlyCommandData(), Commands
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
                "Issuer must sign" using (command.signers == listOf(output.issuer.owningKey))
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
                val requiredSigners = setOf(input.owner.owningKey, input.issuer.owningKey)
                "All participants must sign" using (command.signers.toSet() == requiredSigners)
            }
            is Commands.Lock -> requireThat {
                "A lock transaction must have one input asset" using (tx.inputsOfType<AuctionableAsset>().size == 1)
                "A lock transaction must have one output asset" using (tx.outputsOfType<AuctionableAsset>().size == 1)
                val inputAsset = tx.inputsOfType<AuctionableAsset>().single()
                "The input asset must be unlocked" using !inputAsset.locked
                val outputAsset = tx.outputsOfType<AuctionableAsset>().single()
                "The output asset must be locked" using outputAsset.locked
                "Only the lock property may be changed" using (outputAsset == inputAsset.lock())
                tx.commands.requireSingleCommand<AuctionContract.Commands.Create>()
                "Must be signed by owner" using (command.signers == listOf(inputAsset.owner.owningKey))
                "Must have output auction" using (tx.outputsOfType<AuctionState>().size == 1)
                "Auction must correctly reference this asset" using (tx.outputsOfType<AuctionState>().single().assetId == outputAsset.linearId)
            }
            is Commands.Unlock -> {
                "An unlock transaction must have one input asset" using (tx.inputsOfType<AuctionableAsset>().size == 1)
                "An unlock transaction must have one output asset" using (tx.outputsOfType<AuctionableAsset>().size == 1)
                val inputAsset = tx.inputsOfType<AuctionableAsset>().single()
                "The input asset must be locked" using inputAsset.locked
                val outputAsset = tx.outputsOfType<AuctionableAsset>().single()
                "The output asset must be unlocked" using !outputAsset.locked
                "Only the lock property and owner may be changed" using
                        (outputAsset == inputAsset.copy(owner = outputAsset.owner, locked = false))
                "Must be signed by new owner" using (command.signers == listOf(outputAsset.owner.owningKey))
                val auctionCommand = tx.commands.requireSingleCommand<AuctionContract.Commands>().value
                "Must be end or bid" using
                        (auctionCommand is AuctionContract.Commands.End || auctionCommand is AuctionContract.Commands.Bid)
                "Must have input auction" using (tx.inputsOfType<AuctionState>().size == 1)
                "Auction must correctly reference this asset" using (tx.inputsOfType<AuctionState>().single().assetId == inputAsset.linearId)
            }
        }
    }
}