import React, { Component } from 'react';
import Assets from './components/Assets'
import Auctions from './components/Auctions'
import './App.css';
import { Proxy } from 'braid-client'
import CircularProgress from '@material-ui/core/CircularProgress'
import { Grid, Paper } from '@material-ui/core';
import Holdings from './components/Holdings';
import axios from 'axios'
import SockJS from 'sockjs-client'

class App extends Component {
  constructor() {
    super()

    this.state = {
      name: "",
      loaded: false,
      auctions: [],
      assets: [],
      holdings: []
    }

    this.onOpen = this.onOpen.bind(this)
  }
  componentDidMount() {
    this.braid = new Proxy({
      url: "http://localhost:8088/api/"
    }, this.onOpen, onClose, onError, { strictSSL: false });

    const sock = new SockJS('http://localhost:8082/api/assets/braid')
    sock.onopen = (ev) => console.log(ev)
    // Promise.all([
    //   axios.get('http://localhost:10055/my-name'),
    //   axios.get('http://localhost:10055/assets'),
    //   axios.get('http://localhost:10055/auctions'),
    //   axios.get('http://localhost:10055/holdings')
    // ]).then(values => {
    //   console.log(values)
    //   this.setState({
    //     name: values[0].data,
    //     assets: values[1].data,
    //     auctions: values[2].data,
    //     holdings: values[3].data,
    //     loaded: true
    //   })
    // })
  }

  render() { 
    let body
    
    if (this.state.loaded) {
      body = (
        <div>
          <h1>Cordutch</h1>
          <h2>Logged in as: {this.state.name}</h2>
          <Grid container spacing={3}>
            <Grid item xs={6}>
              <Paper>
                <h2>Assets</h2>
                <Assets assets={this.state.assets} />
              </Paper>
            </Grid>

            <Grid item xs={6}>
              <Paper>
                <h2>Auctions</h2>
                <Auctions name={this.state.name} auctions={this.state.auctions} />
              </Paper>
            </Grid>

            <Grid item xs={6}>
              <Paper>
                <h2>Holdings</h2>
                <Holdings holdings={this.state.holdings} />
              </Paper>
            </Grid>
          </Grid>
        </div>
      )
    } else {
      body = (
        <CircularProgress />
      )
    }


    return (
    <div>
      {body}
    </div>

  );}

  async onOpen() {
    this.braid.assets.track("com.cordutch.states.AuctionableAsset").then(ret => {
      ret.on('data', console.log)
    })
    console.log('Connected to node.')
    Promise.all([
      this.braid.network.myNodeInfo(),
      this.braid.assets.getAssets(),
      this.braid.assets.getAuctions(),
      this.braid.assets.getHoldings()
    ]).then(values => {
      console.log(values[3])
      this.setState({
        name: values[0].legalIdentities[0].name,
        assets: values[1],
        auctions: values[2],
        holdings: values[3],
        loaded: true
      })
    })
  }
}

const onClose = () => { console.log('Disconnected from node.') }
const onError = (err) => { console.error(err); }

export default App;
