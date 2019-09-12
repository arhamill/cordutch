package com.template.contracts

import com.template.states.AuctionState
import com.template.states.AuctionableAsset
import net.corda.core.contracts.UniqueIdentifier
import net.corda.finance.POUNDS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class LockAssetTests {
    private val ledgerServices = MockServices()

    private val validAsset = AuctionableAsset(
            description = "A big house",
            owner = ALICE.party,
            issuer = MEGACORP.party
    )

    private val auction = AuctionState(
            assetId = validAsset.linearId,
            owner = ALICE.party,
            bidders = listOf(BOB.party, CHARLIE.party),
            price = 10.POUNDS
    )

    @Test
    fun validLockVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun mustHaveInputAsset() {
        ledgerServices.ledger {
            transaction {
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "A lock transaction must have one input asset"
            }
        }
    }

    @Test
    fun mustHaveOutputAsset() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "A lock transaction must have one output asset"
            }
        }
    }

    @Test
    fun mustBeUnlocked() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "The input asset must be unlocked"
            }
        }
    }

    @Test
    fun mustLockAsset() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset)
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "The output asset must be locked"
            }
        }
    }

    @Test
    fun mustOnlyChangeLock() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true, description = "A bigger house"))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "Only the lock property may be changed"
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true, owner = MINICORP.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "Only the lock property may be changed"
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true, issuer = MINICORP.party))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "Only the lock property may be changed"
            }
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true, linearId = UniqueIdentifier()))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "Only the lock property may be changed"
            }
        }
    }

    @Test
    fun mustHaveCreateAuctionCommand() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(validAsset.owner.owningKey, AuctionableAssetContract.Commands.Lock())
                this `fails with` "Required com.template.contracts.AuctionContract.Commands.Create command"
            }
        }
    }

    @Test
    fun mustBeSignedByOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, validAsset)
                output(AuctionableAssetContract.ID, validAsset.copy(locked = true))
                command(MINICORP.publicKey, AuctionableAssetContract.Commands.Lock())
                output(AuctionContract.ID, auction)
                command(auction.participants.map {it.owningKey}, AuctionContract.Commands.Create())
                this `fails with` "Must be signed by owner"
            }
        }
    }
}