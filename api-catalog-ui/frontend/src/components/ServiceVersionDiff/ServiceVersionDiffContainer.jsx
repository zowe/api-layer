import { connect } from  'react-redux';
import ServiceVersionDiff from './ServiceVersionDiff';
import { getDiff } from '../../actions/service-version-diff-actions';

const mapSateToProps = state => ({
    diffText: state.serviceVersionDiff.diffText,
    version1: state.serviceVersionDiff.oldVersion,
    version2: state.serviceVersionDiff.newVersion,
});

const mapDispatchToProps = {
    getDiff
};

export default connect(
    mapSateToProps,
    mapDispatchToProps,
)(ServiceVersionDiff);