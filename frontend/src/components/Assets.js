import React, { Component } from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

class Assets extends Component {
    constructor(props) {
        super(props)
    
        this.braid = props.braid
        this.state = {
            assets: []
        }
    }

    componentDidMount() {
        this.braid.assets.getAssets().then((assets) => this.setState({assets: assets}))
    }

    render() { return (
        <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Description</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {this.state.assets.map(asset => (
                <TableRow key={asset.linearId.id}>
                  <TableCell>{asset.linearId.id}</TableCell>
                  <TableCell>{asset.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
        </Table>
      );}
}

export default Assets