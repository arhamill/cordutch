package com.cordutch.states

import com.cordutch.contracts.AuctionContract
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

/**
 * A state representing a Dutch Auction where [owner] is selling an [asset].
 * The price begins at [price] and can be decremented by the [owner]
 * The auction ends either when a successful bid is placed by one of the [bidders] or the owner manually ends it.
 * The [asset] as a field ensures an asset with this exact state is included and unlocked in any terminal operation (bid/end)
 */
@BelongsToContract(AuctionContract::class)
data class AuctionState(
        val assetId: UniqueIdentifier,
        val owner: AbstractParty,
        val bidders : List<AbstractParty>,
        val price : Amount<IssuedTokenType>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
        override val participants: List<AbstractParty>
                get() = listOf(owner)

        fun withNewPrice(newPrice: Amount<IssuedTokenType>): AuctionState {
                return copy(price = newPrice)
        }
}

@CordaSerializable
data class AuctionResponse(val stx: SignedTransaction, val id: UniqueIdentifier, val otherParties: List<Party>)