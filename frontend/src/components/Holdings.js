import React, { useContext } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import { tokensContext } from '../context'

function Holdings() {
    const tokens = useContext(tokensContext)

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
            {tokens.map(holding => (
              <TableRow key={holding.token.tokenType.tokenIdentifier + holding.token.issuer}>
                <TableCell>{holding.quantity * holding.displayTokenSize}</TableCell>
                <TableCell>{holding.token.tokenType.tokenIdentifier}</TableCell>
                <TableCell>{holding.token.issuer}</TableCell>
              </TableRow>
            ))}
          </TableBody>
      </Table>
    )
}

export default Holdings