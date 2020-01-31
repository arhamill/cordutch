import { createTokensContext, createStateContext } from '@arhamill/cortex'

export const { tokensContext, TokensProvider } = createTokensContext('http://localhost:10055')

const asset = createStateContext('http://localhost:10055', 'com.cordutch.states.AuctionableAsset')
export const assetContext = asset.stateContext
export const AssetProvider = asset.StateProvider

const auction = createStateContext('http://localhost:10055', 'com.cordutch.states.AuctionState')
export const auctionContext = auction.stateContext
export const AuctionProvider = auction.StateProvider