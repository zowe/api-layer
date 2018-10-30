import { connect } from 'react-redux';
import { filterText, clear } from '../../actions/filter-actions';
import SearchCriteria from './SearchCriteria';

const mapStateToProps = state => ({
    filterText: state.filtersReducer.text,
    criteria: state.filtersReducer.text,
});

const mapDispatchToProps = dispatch => ({
    filterText: text => dispatch(filterText(text)),
    clear: () => dispatch(clear()),
});

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(SearchCriteria);
