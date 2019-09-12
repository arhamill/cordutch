package com.template.contracts

import com.template.states.AuctionableAsset
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class TransferAssetTests {
    private val ledgerServices = MockServices()

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = BOB.party
    )

    @Test
    fun validTransferVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.withNewOwner(CHARLIE.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveOneInput() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.withNewOwner(CHARLIE.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "An transfer transaction should only consume one input state."
            }
        }
    }

    @Test
    fun mustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.withNewOwner(CHARLIE.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "An transfer transaction should only create one output state."
            }
        }
    }

    @Test
    fun mustOnlyChangeOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(description = "A bigger house"))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "Only the owner property may change."
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(issuer = MINICORP.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "Only the owner property may change."
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "Only the owner property may change."
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(linearId = UniqueIdentifier()))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "Only the owner property may change."
            }
        }
    }

    @Test
    fun mustChangeOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "The owner property must change in a transfer."
            }
        }
    }

    @Test
    fun mustBeSignedByOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.withNewOwner(CHARLIE.party))
                command(CHARLIE.publicKey, AuctionableAssetContract.Commands.Transfer())
                this `fails with` "The old owner must sign"
            }
        }
    }
}