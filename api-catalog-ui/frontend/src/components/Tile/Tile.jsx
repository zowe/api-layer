import { Card, CardContent, Typography } from '@material-ui/core';
import { Component } from 'react';
import Brightness1RoundedIcon from '@material-ui/icons/Brightness1Rounded';
import WarningIcon from '@material-ui/icons/Warning';
import ReportProblemIcon from '@material-ui/icons/ReportProblem';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';

import './Tile.css';

export default class Tile extends Component {
    getStatusFromServiceTotals = tile => {
        const { status } = tile;
        let tileStatus = status;
        if (tileStatus === 'UP' && tile.totalServices !== tile.activeServices) {
            tileStatus = 'WARNING';
        }
        return tileStatus;
    };

    getStatusTextFromServiceTotals = tile => `${tile.activeServices} of ${tile.totalServices} services are running`;

    getTileStatus = tile => {
        if (tile === null || tile === undefined) {
            return <HelpOutlineIcon id="unknown" style={{ color: 'rgb(51, 56, 64)', fontSize: '12px' }} />;
        }
        const status = this.getStatusFromServiceTotals(tile);
        switch (status) {
            case 'UP':
                return <Brightness1RoundedIcon id="success" style={{ color: 'rgb(42, 133, 78)', fontSize: '12px' }} />;
            case 'DOWN':
                return <ReportProblemIcon id="danger" style={{ color: 'rgb(222, 27, 27)', fontSize: '12px' }} />;
            case 'WARNING':
                return <WarningIcon id="warning" style={{ color: 'rgb(173, 95, 0)', fontSize: '12px' }} />;
            default:
                return <HelpOutlineIcon id="unknown" style={{ color: 'rgb(51, 56, 64)', fontSize: '12px' }} />;
        }
    };

    getTileStatusText = tile => {
        if (tile === null || tile === undefined) {
            return 'Status unknown';
        }
        const status = this.getStatusFromServiceTotals(tile);
        switch (status) {
            case 'UP':
                return 'All services are running';
            case 'DOWN':
                return 'No services are running';
            case 'WARNING':
                return this.getStatusTextFromServiceTotals(tile);
            default:
                return 'Status unknown';
        }
    };

    handleClick = () => {
        const { tile, history } = this.props;
        const tileRoute = `/tile/${tile.id}`;
        history.push(tileRoute);
    };

    // not a great way to avoid overlapping text in a card block
    // Mineral bug
    shortenDescription = description => {
        if (description.length > 180) {
            return `${description.substring(0, 177)}...`;
        }
        return description;
    };

    render() {
        const { tile } = this.props;

        return (
            <Card key={tile.id} className="grid-tile pop grid-item" onClick={this.handleClick} data-testid="tile">
                <CardContent style={{ fontSize: '0.875em', color: 'rgb(88, 96, 110)' }} className="tile">
                    <Typography
                        variant="subtitle1"
                        style={{
                            color: 'rgb(88, 96, 110)',
                            fontWeight: 'bold',
                            fontSize: '1.125em',
                        }}
                    >
                        {tile.title}
                    </Typography>
                    <br />
                    {this.shortenDescription(tile.description)}
                    <Typography id="tileLabel" className="grid-tile-status">
                        {this.getTileStatus(tile)}
                        {this.getTileStatusText(tile)}
                    </Typography>
                    {tile.sso && (
                        <Typography variant="h6" className="grid-tile-sso">
                            SSO
                        </Typography>
                    )}
                </CardContent>
            </Card>
        );
    }
}
