import React from 'react'
import createTokensProvider from './tokensProvider'
import createStateProvider from './stateProvider'

export const createTokensContext = (url) => {
    const context = React.createContext()

    return {
        tokensContext: context,
        TokensProvider: createTokensProvider(url, context)
    }
}

export const createStateContext = (url, stateClass) => {
    const context = React.createContext()

    return {
        stateContext: context,
        StateProvider: createStateProvider(url, stateClass, context)
    }
}