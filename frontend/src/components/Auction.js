import React, { useState, useEffect } from 'react'
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';

const Auction = ({data}) => {
    const [timeData, setTimeData] = useState(calculateTimeData(data))

    useEffect(() => {
        const interval = setInterval(() => setTimeData(calculateTimeData(data)), 1000)

        return () => clearInterval(interval)
    }, [data])

    return (
        <TableRow>
            <TableCell>{data.assetId.id}</TableCell>
            <TableCell>{data.owner}</TableCell>
            <TableCell>{`${timeData.price} ${data.price.token.tokenType.tokenIdentifier} issued by ${data.price.token.issuer}`}</TableCell>
            <TableCell>{`${timeData.price - (data.decrement.displayTokenSize * data.decrement.quantity)}`}</TableCell>
            <TableCell>{timeData.remaining}</TableCell>
        </TableRow>
      )
}

const calculateTimeData = auction => {
    const startTime = Date.parse(auction.startTime)
    const startPrice = auction.price.displayTokenSize * auction.price.quantity
    const decrement = auction.decrement.displayTokenSize * auction.decrement.quantity
    const elapsed = Date.now() - startTime
    const periods = Math.floor(elapsed / auction.period)
    const d = new Date(((periods + 1) * auction.period) - elapsed)
    const price = startPrice - (decrement * periods)

    return {
        price,
        remaining: `${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:${pad(d.getUTCSeconds())}`
    }
}

const pad = (num) => {
    return String(num).padStart(2, '0')
}

export default Auction