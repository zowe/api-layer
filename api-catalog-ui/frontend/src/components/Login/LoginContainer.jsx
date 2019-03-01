import {connect} from 'react-redux';
import {withRouter} from 'react-router-dom';
import Login from './Login';
import {userActions} from '../../actions/user.actions';
import {createLoadingSelector} from '../../selectors/selectors';

const loadingSelector = createLoadingSelector(['USERS_LOGIN']);

const mapStateToProps = state => ({
    authentication: state.authenticationReducer,
    isFetching: loadingSelector(state),
});

const mapDispatchToProps = {
    login: credentials => userActions.login(credentials),
    logout: () => userActions.logout(),
};

export default withRouter(
    connect(
        mapStateToProps,
        mapDispatchToProps
    )(Login)
);
