import React from 'react';
import './App.css';
import Holdings from './components/Holdings';
import Assets from './components/Assets';
import Auctions from './components/Auctions'
import { Grid, Paper } from '@material-ui/core'

const App = () => {
  return (
    <Grid container spacing={2}>
      <Grid item xs={6}>
        <Paper>
          <h2>Assets</h2>
          <Assets />
        </Paper>
      </Grid>

      <Grid item xs={6}>
        <Paper>
          <h2>Holdings</h2>
          <Holdings />
        </Paper>
      </Grid>

      <Grid item xs={12}>
        <Paper>
          <h2>Auctions</h2>
          <Auctions />
        </Paper>
      </Grid>
    </Grid>
  )
}

export default App;
