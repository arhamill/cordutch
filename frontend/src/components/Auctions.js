import React, { Component } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Button from '@material-ui/core/Button'

class Auctions extends Component {
    constructor(props) {
        super(props)
    
        this.state = {
            auctions: auctionTimer(props.auctions)
        }
    }

    componentDidMount() {
        this.interval = setInterval(() => this.setState({auctions: auctionTimer(this.state.auctions)}), 1000)
    }

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    render() { return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Asset</TableCell>
                <TableCell>Owner</TableCell>
                <TableCell>Current Price</TableCell>
                <TableCell>Next Price</TableCell>
                <TableCell>Next decrement</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {this.state.auctions.map(auction => (
                <TableRow key={auction.id}>
                  <TableCell>{auction.assetDesc}</TableCell>
                  <TableCell>{auction.owner}</TableCell>
                  <TableCell>{`${auction.price} ${auction.currency} issued by ${auction.issuer}`}</TableCell>
                  <TableCell>{`${auction.price - auction.decrement}`}</TableCell>
                  <TableCell>{auction.remaining}</TableCell>
                  { this.props.name === auction.owner ? null :
                    <TableCell>
                      <Button variant="contained" color="primary">
                        Bid!
                      </Button>
                    </TableCell>
                  }
                </TableRow>
              ))}
            </TableBody>
        </Table>
      );}
}

const auctionTimer = auctions => auctions.map(auction => {
    const elapsed = Date.now() - auction.startTime
    const periods = Math.floor(elapsed / auction.period)
    const d = new Date(((periods + 1) * auction.period) - elapsed)
    const price = auction.startPrice - (auction.decrement * periods)

    return {
        ...auction,
        price,
        remaining: `${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:${pad(d.getUTCSeconds())}`
    }
})

const pad = (num) => {
    return String(num).padStart(2, '0')
}

export default Auctions