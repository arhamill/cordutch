package com.cordutch.states

import com.cordutch.contracts.AuctionableAssetContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * Represents any asset that can be sold via a dutch auction.
 * The [locked] property blocks the transferal and consumption of the asset to ensure these operations cannot occur during an auction.
 */
@BelongsToContract(AuctionableAssetContract::class)
data class AuctionableAsset(
        val description: String,
        val owner: Party,
        val issuer: Party,
        val locked: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<Party>
        get() = listOf(owner)

    fun withNewOwner(newOwner: Party) : AuctionableAsset {
        return copy(owner = newOwner)
    }

    fun lock() : AuctionableAsset {
        return copy(locked = true)
    }

    fun unlock() : AuctionableAsset {
        return copy(locked = false)
    }
}