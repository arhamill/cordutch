package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.internal.packageName
import net.corda.finance.POUNDS
import net.corda.finance.USD
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BidAuctionTests {
    private val ledgerServices = MockServices(listOf("com.cordutch", "net.corda.finance.contracts.asset", CashSchemaV1::class.packageName))

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
            price = 10.POUNDS
    )

    private val cash = createCashState(validAuction.price, BOB.party)

    @Test
    fun validBidVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                verifies()
            }
        }
    }

    @Test
    fun mustHaveAuctionInput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Required com.cordutch.contracts.AuctionContract.Commands command"
            }
        }
    }

    @Test
    fun mustNotHaveAuctionOutput() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                output(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "A bid transaction must have no outputs"
            }
        }
    }

    @Test
    fun mustHaveCashOutputToOwner() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(MINICORP.party))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "There must be output cash paid to the owner."
            }
        }
    }

    @Test
    fun mustHaveCorrectPaymentAmount() {
        val badAmountCash = createCashState(validAuction.price - 1.POUNDS, BOB.party)
        val badCurrencyCash = createCashState(validAuction.price.copy(token = USD), BOB.party)
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, badAmountCash)
                output(Cash.PROGRAM_ID, badAmountCash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "The exact amount must be paid"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, badCurrencyCash)
                output(Cash.PROGRAM_ID, badCurrencyCash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "The exact amount must be paid"
            }
        }
    }

    @Test
    fun mustBeBidder() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(MINICORP.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "The signer must be a bidder"
            }
        }
    }

    @Test
    fun mustHaveAssetUnlock() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Required com.cordutch.contracts.AuctionableAssetContract.Commands.Unlock command"
            }
        }
    }

    @Test
    fun mustReferenceCorrectAsset() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction.copy(assetId = UniqueIdentifier()))
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(Cash.PROGRAM_ID, cash)
                output(Cash.PROGRAM_ID, cash.ownedBy(validAuction.owner))
                command(BOB.publicKey, Cash.Commands.Move())
                this `fails with` "Must reference correct asset"
            }
        }
    }
}