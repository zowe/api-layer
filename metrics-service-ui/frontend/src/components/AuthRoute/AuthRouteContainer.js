import { connect } from 'react-redux';

import AuthRoute from './AuthRoute';

const mapStateToProps = (state) => {
    const authenticated = !!state.authenticationReducer.sessionOn;
    return { authenticated };
};

export default connect(mapStateToProps)(AuthRoute);
