package com.template.states

import com.template.contracts.AuctionContract
import net.bytebuddy.implementation.bind.MethodDelegationBinder
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

/**
 * A state representing a Dutch Auction where [owner] is selling an [assetDescription].
 * The price begins at [price] and can be decremented by the [owner]
 * The auction ends either when a successful bid is placed by one of the [bidders] or the owner manually ends it
 */
@BelongsToContract(AuctionContract::class)
data class AuctionState(
        val assetId: UniqueIdentifier,
        val owner: Party,
        val bidders : List<Party>,
        val price : Amount<Currency>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
        override val participants: List<AbstractParty>
                get() = bidders + owner

        fun withNewPrice(newPrice: Amount<Currency>): AuctionState {
                return copy(price = newPrice)
        }
}
