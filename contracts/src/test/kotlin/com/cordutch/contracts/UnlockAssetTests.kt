package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.schemas.TokenSchemaV1
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.internal.packageName
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class UnlockAssetTests {
    private val ledgerServices = MockServices(listOf("com.cordutch", "com.r3.corda.lib.tokens.contracts", TokenSchemaV1::class.packageName))

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = MEGACORP.party,
            locked = true
    )

    private val auction = AuctionState(
            assetId = validAsset.linearId,
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.GBP issuedBy MEGACORP.party,
            decrement = 1.GBP,
            period = 1000L
    )

    private val cash = auction.price heldBy BOB.party
    private val timeWindow = TimeWindow.fromOnly(auction.startTime)

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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                output(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.Create())
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(AuctionContract.ID, auction.copy(assetId = UniqueIdentifier()))
                command(auction.owner.owningKey, AuctionContract.Commands.End())
                this `fails with` "Auction must correctly reference this asset"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(AuctionContract.ID, auction.copy(assetId = UniqueIdentifier()))
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(auction.owner))
                command(BOB.publicKey, MoveTokenCommand(auction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
                this `fails with` "Auction must correctly reference this asset"
            }
        }
    }
}