import React, { useContext } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import { assetContext } from '../context'

function Assets() {
  const assets = useContext(assetContext)

  return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {assets.map(asset => (
                <TableRow key={asset.linearId.id}>
                  <TableCell>{asset.linearId.id}</TableCell>
                  <TableCell>{asset.description}</TableCell>
                  <TableCell>{asset.locked ? 'Under auction' : 'Free for transfer'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
        </Table>
      )
}

export default Assets