/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { IconButton, Typography } from '@material-ui/core';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import {useNavigate} from "react-router";

function PageNotFound() {

    const navigate = useNavigate();
    const handleGoToHome = () => {
        navigate('/dashboard');
    };


    const iconBack = <ChevronLeftIcon />;
        return (
            <div>
                <br />
                <Typography id="label" variant="h5">
                    Page Not Found
                </Typography>
                <div>
                    <IconButton
                        id="go-back-button"
                        data-testid="go-home-button"
                        onClick={handleGoToHome}
                        size="medium"
                    >
                        {iconBack}
                        Go to Dashboard
                    </IconButton>
                </div>
            </div>
        );

}

export default PageNotFound;
// PageNotFound.propTypes = {
//     history: PropTypes.shape({
//         push: PropTypes.func.isRequired,
//     }).isRequired,
// };
