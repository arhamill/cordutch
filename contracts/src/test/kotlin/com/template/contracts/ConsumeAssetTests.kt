package com.template.contracts

import com.template.states.AuctionableAsset
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ConsumeAssetTests {
    private val ledgerServices = MockServices()

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = BOB.party
    )

    @Test
    fun validConsumeVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Consume())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveOneInputState() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                input(AuctionableAssetContract.ID, validAsset)
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Consume())
                this `fails with` "A consume transaction should only consume one input state."
            }
        }
    }

    @Test
    fun mustHaveOneOutputState() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Consume())
                this `fails with` "A consume transaction should have no output states"
            }
        }
    }

    @Test
    fun mustBeUnlocked() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Consume())
                this `fails with` "The asset must be unlocked"
            }
        }
    }

    @Test
    fun mustBeSignedByParticipants() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                command(MINICORP.publicKey, AuctionableAssetContract.Commands.Consume())
                this `fails with` "All participants must sign"
            }
        }
    }
}