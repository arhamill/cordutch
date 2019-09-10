package com.template.contracts

import com.template.states.AuctionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class DecreaseAuctionTests {
    private val ledgerServices = MockServices()

    private val auction = AuctionState(
            assetDescription = "My car",
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.POUNDS
    )

    @Test
    fun validUpdateVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.withNewPrice(auction.price - 1.POUNDS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveOneInputState() {
        ledgerServices.ledger {
            transaction {
                output(AuctionContract.ID, auction.withNewPrice(auction.price - 1.POUNDS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "An auction decrease transaction must have one input state."
            }
        }
    }

    @Test
    fun mustHaveOneOutputState() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "An auction decrease transaction must have one output state."
            }
        }
    }

    @Test
    fun mustOnlyChangePrice() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.copy(assetDescription = "My other car"))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Only the price may change"
            }
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.copy(owner = MINICORP.party))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Only the price may change"
            }
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.copy(bidders = listOf(MINICORP.party)))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Only the price may change"
            }
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.copy(linearId = UniqueIdentifier()))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Only the price may change"
            }
        }
    }

    @Test
    fun mustDecreasePrice() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.withNewPrice(auction.price + 1.POUNDS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "The price must decrease"
            }
        }
    }

    @Test
    fun mustNotChangeCurrency() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.withNewPrice(1.DOLLARS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "Token mismatch: GBP vs USD"
            }
        }
    }

    @Test
    fun mustBePositivePrice() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.withNewPrice(0.POUNDS))
                command(auction.owner.owningKey, AuctionContract.Commands.Decrease())
                this `fails with` "The new price must be greater than zero"
            }
        }
    }

    @Test
    fun mustBeSignedByOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, auction)
                output(AuctionContract.ID, auction.withNewPrice(1.POUNDS))
                command(MINICORP.publicKey, AuctionContract.Commands.Decrease())
                this `fails with` "The owner must sign"
            }
        }
    }
}