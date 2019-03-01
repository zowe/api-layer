import {connect} from 'react-redux';
import {userActions} from '../../actions/user.actions';
import Header from './Header';

const mapStateToProps = () => ({});

const mapDispatchToProps = {
    logout: () => userActions.logout(),
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Header);
