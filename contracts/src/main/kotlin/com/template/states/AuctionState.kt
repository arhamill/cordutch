package com.template.states

import com.template.contracts.AuctionContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

/**
 * A state representing a Dutch Auction where [owner] is selling an [assetDescription].
 * The price begins at [startPrice] at [startTime] and decreases by [decrement] every [decrementPeriod] seconds.
 * The auction ends either when a successful bid is placed by one of the [bidders] or the owner manually ends it
 * (providing the price is below the [reservePrice]).
 */
@BelongsToContract(AuctionContract::class)
data class AuctionState(
        val assetDescription: String,
        val owner: Party,
        val bidders : List<Party>,
        val startPrice : Amount<Currency>,
        val startTime : Instant,
        val decrement : Amount<Currency>,
        val decrementPeriod : Long,
        val reservePrice : Amount<Currency>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
        override val participants: List<AbstractParty>
                get() = bidders + owner
}
