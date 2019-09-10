package com.template.contracts

import com.template.states.AuctionState
import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class EndAuctionTests {
    private val ledgerServices = MockServices()

    private val auction = AuctionState(
            assetDescription = "My car",
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
                this `fails with` "The owner must sign"
            }
        }
    }
}