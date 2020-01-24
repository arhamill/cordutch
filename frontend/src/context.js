import { createTokensContext, createStateContext } from './cortex/cortex'

export const { tokensContext, TokensProvider } = createTokensContext('http://localhost:10055')

const asset = createStateContext('http://localhost:10055', 'com.cordutch.states.AuctionableAsset')
export const assetContext = asset.stateContext
export const AssetProvider = asset.StateProvider