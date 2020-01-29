import React, { useContext } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import { auctionContext } from '../context'
import Auction from './Auction'

const Auctions = () => {

  const auctions = useContext(auctionContext)

  return (
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
          {auctions.map(auction => (
            <Auction key={auction.linearId.id} data={auction} />
          ))}
        </TableBody>
    </Table>
  );
}

export default Auctions