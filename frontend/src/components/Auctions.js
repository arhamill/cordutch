import React, { Component } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

class Auctions extends Component {
    constructor(props) {
        super(props)
    
        this.braid = props.braid
        this.state = {
            auctions: []
        }
    }

    componentDidMount() {
        this.braid.assets.getAuctions().then((auctions) => this.setState({auctions: auctions}))
    }

    render() { return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Asset description</TableCell>
                <TableCell>Owner</TableCell>
                <TableCell>Current Price</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {this.state.auctions.map(auction => (
                <TableRow key={auction.id}>
                  <TableCell>{auction.owner}</TableCell>
                  <TableCell>{auction.currentPrice}</TableCell>
                </TableRow>
              ))}
            </TableBody>
        </Table>
      );}
}

export default Auctions