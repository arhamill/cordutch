package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.contracts.UniqueIdentifier

import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CreateAuctionTests {
    private val ledgerServices = MockServices()

    private val asset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = MEGACORP.party,
            locked = true
    )

    private val validAuction = AuctionState(
            assetId = asset.linearId,
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.GBP issuedBy MEGACORP.party
    )

    @Test
    fun validAuctionVerifies() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveNoInputs() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "No auction inputs should be consumed when creating an auction"
            }
        }
    }

    @Test
    fun mustCreateOneOutputState() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction)
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "Must have output auction"
            }
        }
    }

    @Test
    fun mustNotHaveOwnerAsBidder() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(bidders = listOf(validAuction.owner)))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "The owner must not be a bidder"
            }
        }
    }

    @Test
    fun mustHaveAtLeastOneBidder() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(bidders = listOf()))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "There must be at least one bidder"
            }
        }
    }

    @Test
    fun mustHavePriceGreaterThanZero() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(price = 0.GBP issuedBy MEGACORP.party))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "The start price should be greater than zero"
            }
        }
    }

    @Test
    fun mustHaveAllSignatures() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "All participants must sign"
            }
        }
    }

    @Test
    fun mustHaveAssetLockCommand() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                this `fails with` ""
            }
        }
    }

    @Test
    fun mustReferenceCorrectAsset() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(assetId = UniqueIdentifier()))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                input(AuctionableAssetContract.ID, asset.unlock())
                output(AuctionableAssetContract.ID, asset)
                command(asset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "Auction must correctly reference this asset"
            }
        }
    }
}