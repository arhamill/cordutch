package com.cordutch.states

import com.cordutch.contracts.AuctionableAssetContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty

@BelongsToContract(AuctionableAssetContract::class)
data class AuctionableAsset(
        val description: String,
        val owner: AbstractParty,
        val issuer: AbstractParty,
        val locked: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    fun withNewOwner(newOwner: AbstractParty) : AuctionableAsset {
        return copy(owner = newOwner)
    }

    fun lock() : AuctionableAsset {
        return copy(locked = true)
    }

    fun unlock() : AuctionableAsset {
        return copy(locked = false)
    }
}