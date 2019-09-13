package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.internal.packageName
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockAssetTests {
    private val ledgerServices = MockServices(listOf("com.cordutch", "net.corda.finance.contracts.asset", CashSchemaV1::class.packageName))

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = MEGACORP.party,
            locked = true
    )

    private val auction = AuctionState(
            asset = validAsset,
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.POUNDS
    )

    private val cash = createCashState(auction.price, BOB.party)

    @Test
    fun validUnlockVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                verifies()
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                verifies()
            }
        }
    }

    @Test
    fun mustHaveOneInput() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Must reference correct asset"
            }
        }
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Must reference correct asset"
            }
        }
    }

    @Test
    fun mustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "An unlock transaction must have one output asset"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "An unlock transaction must have one output asset"
            }
        }
    }

    @Test
    fun mustBeLocked() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.unlock())
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "The input asset must be locked"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.unlock())
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "The input asset must be locked"
            }
        }
    }

    @Test
    fun mustUnlockOutput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "The output asset must be unlocked"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "The output asset must be unlocked"
            }
        }
    }

    @Test
    fun mustBeSignedByNewOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(MINICORP.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Must be signed by new owner"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(MINICORP.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Must be signed by new owner"
            }
        }
    }

    @Test
    fun mustBeAuctionEndOrBid() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.copy(price = auction.price - 1.POUNDS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Must be end or bid"
            }
        }
    }

    @Test
    fun mustHaveInputAuction() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                output(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Must have input auction"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                output(AuctionContract.ID, auction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Must have input auction"
            }
        }
    }

    @Test
    fun mustHaveAssetReferencedInAuction() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock())
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction.copy(asset = validAsset.withNewOwner(MINICORP.party)))
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Auction must correctly reference this asset"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction.copy(asset = validAsset.withNewOwner(MINICORP.party)))
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(auction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Auction must correctly reference this asset"
            }
        }
    }
}