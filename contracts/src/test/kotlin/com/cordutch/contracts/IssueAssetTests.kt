package com.cordutch.contracts

import com.cordutch.states.AuctionableAsset
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IssueAssetTests {
    private val ledgerServices = MockServices()

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = BOB.party
    )

    @Test
    fun validAssetVerifies() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.issuer.owningKey, AuctionableAssetContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveNoInputs() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Issue())
                this `fails with` "No inputs should be consumed when issuing an asset."
            }
        }
    }

    @Test
    fun mustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Issue())
                this `fails with` "Only one output state should be created when issuing an asset."
            }
        }
    }

    @Test
    fun mustHaveADescription() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.copy(description = ""))
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Issue())
                this `fails with` "The asset must have a description"
            }
        }
    }

    @Test
    fun mustBeUnlocked() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.participants.map { it.owningKey }, AuctionableAssetContract.Commands.Issue())
                this `fails with` "The asset must be unlocked"
            }
        }
    }

    @Test
    fun mustBeSignedByIssuer() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset)
                command(MINICORP.publicKey, AuctionableAssetContract.Commands.Issue())
                this `fails with` "Issuer must sign"
            }
        }
    }
}