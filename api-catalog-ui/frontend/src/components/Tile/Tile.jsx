/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {Card, CardActionArea, CardContent, Typography} from '@material-ui/core';
import Brightness1RoundedIcon from '@material-ui/icons/Brightness1Rounded';
import ReportProblemIcon from '@material-ui/icons/ReportProblem';
import HelpOutlineIcon from '@material-ui/icons/HelpOutline';
import {useNavigate} from "react-router";

function Tile({service, fetchNewService}) {
    const navigate = useNavigate();
    const getTileStatus = (service) => {
        const unknownIcon = <>
            <HelpOutlineIcon id="unknown" style={{color: 'rgb(51, 56, 64)', fontSize: '12px'}}/>
            Status unknown
        </>;
        if (service === null || service === undefined) {
            return unknownIcon;
        }
        const {status} = service;
        switch (status) {
            case 'UP':
                return <>
                    <Brightness1RoundedIcon data-testid="success-icon" id="success"
                                            style={{color: 'rgb(42, 133, 78)', fontSize: '12px'}}/>
                    The service is running
                </>;
            case 'DOWN':
                return <>
                    <ReportProblemIcon id="danger" data-testid="danger-icon" style={{color: 'rgb(222, 27, 27)', fontSize: '12px'}}/>
                    The service is not running
                </>
            default:
                return unknownIcon;
        }
    };

    const handleClick = () => {
        const tileRoute = `/service/${service.serviceId}`;
        fetchNewService(service.serviceId);
        navigate(tileRoute);
        localStorage.setItem('serviceId', service.serviceId);
    };

    return (
        <Card key={service.serviceId} className="grid-tile pop grid-item" onClick={handleClick} data-testid="tile">
            <CardActionArea style={{fontSize: '0.875em', color: 'rgb(88, 96, 110)'}} className="card-action">
                <CardContent style={{fontSize: '0.875em', color: 'rgb(88, 96, 110)'}} className="tile">
                    <div className="tile-ctn">
                        <div className="tile-title">
                            <Typography id="tileLabel" className="grid-tile-status">
                                {getTileStatus(service)}
                            </Typography>
                            <Typography id="tiles-service-title" variant="subtitle1">
                                {service.title}
                            </Typography>
                            {service.sso && (
                                <Typography variant="h6" id="grid-tile-sso">
                                    (SSO)
                                </Typography>
                            )}
                        </div>
                    </div>
                </CardContent>
            </CardActionArea>
        </Card>
    );

}

export default Tile;
