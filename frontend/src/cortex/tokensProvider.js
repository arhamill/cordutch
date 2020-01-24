import React, { useState, useEffect, useCallback } from 'react'
import axios from 'axios'
import { SSE } from 'sse.js'

const createTokensProvider = (url, context) => ({children}) => {
    const [tokens, setTokens] = useState({})
    const [source, setSource] = useState(dummySource)

    const callback = useCallback(
        (event) => handleTokenEvent(event, tokens, setTokens),
        [tokens, setTokens]
    )

    useEffect(() => {
        axios.post(url + '/tokens')
        .then(response => setTokens(response.data[0]))
        .then(() => setSource(new SSE(url + '/tokens/updates', {payload: 'tokens'})))
    }, [])

    useEffect(() => source.stream(), [source])

    useEffect(() => {
        source.addEventListener('message', callback)
        
        return () => source.removeEventListener('message', callback)
    }, [source, callback])

    const { Provider } = context

    return (
        <Provider value={Object.values(tokens)}>
            {children}
        </Provider>
    )
}

const dummySource = {
    stream: () => {},
    addEventListener: () => {},
    removeEventListener: () => {}
}

const handleTokenEvent = (event, tokens, setTokens) => {
    const copy = JSON.parse(JSON.stringify(tokens))
    const { consumed, produced } = JSON.parse(event.data)

    Object.keys(produced).forEach(key => {
        if (key in copy) {
            copy[key].quantity += produced[key].quantity
        } else {
            copy[key] = produced[key]
        }
    });

    Object.keys(consumed).forEach(key => {
        if (key in copy) {
            const newValue = copy[key].quantity - consumed[key].quantity
            if (newValue > 0) {
                copy[key].quantity = newValue
            } else {
                delete copy[key]
            }
        }
    })

    setTokens(copy)
}

export default createTokensProvider