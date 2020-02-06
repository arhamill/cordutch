import { createTokensContext, createLinearContext } from '@arhamill/cortex'

export const { tokensContext, TokensProvider } = createTokensContext('http://localhost:10055')

const asset = createLinearContext('http://localhost:10055', 'com.cordutch.states.AuctionableAsset')
export const assetContext = asset.linearContext
export const AssetProvider = asset.LinearProvider

const auction = createLinearContext('http://localhost:10055', 'com.cordutch.states.AuctionState')
export const auctionContext = auction.linearContext
export const AuctionProvider = auction.LinearProvider