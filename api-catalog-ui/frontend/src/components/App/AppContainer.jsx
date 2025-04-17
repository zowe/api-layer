/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import App from './App';
import {getFilteredServices} from "../../selectors/selectors";
import {connect} from "react-redux";
import { userActions } from '../../actions/user-actions';
import {
    fetchNewService, fetchServiceFailed,
    fetchServiceStop,
    fetchTilesFailed,
    fetchTilesStart,
    fetchTilesStop,
    fetchTilesSuccess
} from "../../actions/catalog-tile-actions";
import {clearService, selectService} from "../../actions/selected-service-actions";

const withRouter = (App) =>{
    return (props) =>{
        return <App {...props}/>
    }
}

const mapDispatchToProps = (dispatch) => ({
    success: (user) => dispatch(userActions.query(user)),
});


const mapStateToProps = (state) => ({
    authentication: state.authenticationReducer,
});


export default connect(mapStateToProps, mapDispatchToProps)(withRouter(App))
