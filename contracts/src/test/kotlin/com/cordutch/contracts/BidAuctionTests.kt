package com.cordutch.contracts

import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.TokenCommand
import com.r3.corda.lib.tokens.contracts.schemas.TokenSchemaV1
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.money.USD
import net.corda.core.contracts.TimeWindow
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
    private val ledgerServices = MockServices(listOf("com.cordutch", "com.r3.corda.lib.tokens.contracts", TokenSchemaV1::class.packageName))

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
            price = 10.GBP issuedBy MEGACORP.party,
            decrement = 1.GBP,
            period = 1000L
    )

    private val cash = 9.GBP issuedBy MEGACORP.party heldBy BOB.party

    private val timeWindow = TimeWindow.fromOnly(validAuction.startTime.plusMillis(1000L))

    @Test
    fun validBidVerifies() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(1), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(2)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(MINICORP.party))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
                this `fails with` "The time window must be correct"
            }
        }
    }

    @Test
    fun mustHaveCorrectPaymentAmount() {
        val badAmountCash = validAuction.price - 5.of(validAuction.price.token) heldBy BOB.party
        val badCurrencyCash = 10.USD issuedBy MEGACORP.party heldBy BOB.party
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(FungibleTokenContract.contractId, badAmountCash)
                output(FungibleTokenContract.contractId, badAmountCash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
                this `fails with` "The time window must be correct"
            }
        }
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(FungibleTokenContract.contractId, badCurrencyCash)
                output(FungibleTokenContract.contractId, badCurrencyCash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
                this `fails with` "Token mismatch"
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(1), listOf(0)))
                timeWindow(timeWindow)
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
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                timeWindow(timeWindow)
                this `fails with` "Must reference correct asset"
            }
        }
    }

    @Test
    fun mustHaveTimeWindow() {
        ledgerServices.ledger {
            transaction {
                input(AuctionContract.ID, validAuction)
                command(BOB.publicKey, AuctionContract.Commands.Bid())
                input(AuctionableAssetContract.ID, asset)
                output(AuctionableAssetContract.ID, asset.unlock().withNewOwner(BOB.party))
                command(BOB.publicKey, AuctionableAssetContract.Commands.Unlock())
                input(FungibleTokenContract.contractId, cash)
                output(FungibleTokenContract.contractId, cash.withNewHolder(validAuction.owner))
                command(BOB.publicKey, MoveTokenCommand(validAuction.price.token, listOf(2), listOf(1)))
                `fails with`("The transaction must be time windowed")
            }
        }
    }
}