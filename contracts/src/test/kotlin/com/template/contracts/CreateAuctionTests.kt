package com.template.contracts

import com.template.states.AuctionState

import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CreateAuctionTests {
    private val ledgerServices = MockServices()

    private val validAuction = AuctionState(
            assetDescription = "My car",
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.POUNDS
    )

    @Test
    fun validAuctionVerifies() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction)
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
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
                this `fails with` "No inputs should be consumed when creating an auction"
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
                this `fails with` "Only one output state should be created when creating an Auction."
            }
        }
    }

    @Test
    fun mustHaveADescription() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(assetDescription = ""))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
                this `fails with` "The asset must have a description"
            }
        }
    }

    @Test
    fun mustNotHaveOwnerAsBidder() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(bidders = listOf(validAuction.owner)))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
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
                this `fails with` "There must be at least one bidder"
            }
        }
    }

    @Test
    fun mustHavePriceGreaterThanZero() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, validAuction.copy(price = 0.POUNDS))
                command(listOf(ALICE, BOB, CHARLIE).map { it.publicKey }, AuctionContract.Commands.Create())
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
                this `fails with` "All participants must sign"
            }
        }
    }
}