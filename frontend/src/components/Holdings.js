import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

function Holdings(props) {
    return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Value</TableCell>
                <TableCell>Currency</TableCell>
                <TableCell>Issuer</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {props.holdings.map(holding => (
                <TableRow key={holding}>
                  <TableCell>{holding.quantity * holding.displayTokenSize}</TableCell>
                  <TableCell>{holding.token.tokenIdentifier}</TableCell>
                  <TableCell>{holding.token.issuer.name}</TableCell>
                </TableRow>
              ))}
            </TableBody>
        </Table>
      )
}

export default Holdings