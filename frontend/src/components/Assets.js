import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

function Assets(props) { return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {props.assets.map(asset => (
                <TableRow key={asset.linearId.id}>
                  <TableCell>{asset.linearId.id}</TableCell>
                  <TableCell>{asset.description}</TableCell>
                  <TableCell>{asset.locked ? 'Under auction' : 'Unlocked'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
        </Table>
      )
}

export default Assets