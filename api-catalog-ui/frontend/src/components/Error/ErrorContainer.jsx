import { connect } from 'react-redux';
import { clearAllErrors } from '../../actions/error-actions';
import Error from './Error';

const mapStateToProps = state => ({
    errors: state.errorReducer.errors,
});

const mapDispatchToProps = {
    clearAllErrors,
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Error);
