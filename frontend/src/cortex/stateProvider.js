import React, { useState, useEffect, useCallback } from 'react'
import axios from 'axios'
import { SSE } from 'sse.js'

const createStateProvider = (url, stateClass, context) => ({children}) => {
    const [states, setStates] = useState({})
    const [source, setSource] = useState(dummySource)

    const callback = useCallback(
        (event) => handleStateEvent(event, states, setStates),
        [states, setStates]
    )

    useEffect(() => {
        axios.post(url + '/states', stateClass)
        .then(response => setStates(response.data[0]))
        .then(() => setSource(new SSE(url + '/states/updates', {payload: stateClass})))
    }, [])

    useEffect(() => source.stream(), [source])

    useEffect(() => {
        source.addEventListener('message', callback)
        
        return () => source.removeEventListener('message', callback)
    }, [source, callback])

    const { Provider } = context

    return (
        <Provider value={Object.values(states)}>
            {children}
        </Provider>
    )
}

const dummySource = {
    stream: () => {},
    addEventListener: () => {},
    removeEventListener: () => {}
}

const handleStateEvent = (event, states, setStates) => {
    const copy = JSON.parse(JSON.stringify(states))
    const { consumed, produced } = JSON.parse(event.data)

    Object.keys(consumed).forEach(key => {
        delete copy[key]
    })
    Object.keys(produced).forEach(key => {
        copy[key] = produced[key]
    })

    setStates(copy)
}

export default createStateProvider