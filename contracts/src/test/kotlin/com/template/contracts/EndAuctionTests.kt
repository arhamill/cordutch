package com.template.contracts

import com.template.states.AuctionState
import com.template.states.AuctionableAsset
import net.corda.core.contracts.UniqueIdentifier
import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class EndAuctionTests {
    private val ledgerServices = MockServices()

    private val asset = AuctionableAsset(
            description = "A car",
            owner = ALICE.party,
            issuer = MEGACORP.party,
            locked = true
    )

    private val auction = AuctionState(
            assetId = asset.linearId,
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.POUNDS
    )

    @Test
    fun validEndVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.copy(locked = false))
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveOneInput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.copy(locked = false))
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                this `fails with` "An end auction transaction must have one input state."
            }
        }
    }

    @Test
    fun mustHaveNoOutputs() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.copy(locked = false))
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                this `fails with` "An end auction transaction must have no outputs"
            }
        }
    }

    @Test
    fun mustBeSignedByOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                command(MINICORP.publicKey, AuctionContract.Commands.End())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.copy(locked = false))
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                this `fails with` "The owner must sign"
            }
        }
    }

    @Test
    fun mustHaveUnlockCommand() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Required com.template.contracts.AuctionableAssetContract.Commands.Unlock command"
            }
        }
    }

    @Test
    fun mustHaveCorrectAssetId() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction.copy(assetId = UniqueIdentifier()))
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.copy(locked = false))
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                this `fails with` "Must have correct asset id"
            }
        }
    }
}