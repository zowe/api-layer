import { connect } from 'react-redux';
import { withRouter } from 'react-router-dom';
import ServiceTab from './ServiceTab';

const mapStateToProps = state => ({
    tiles: state.tilesReducer.tiles,
});



const mapDispatchToProps = () => ({});

export default withRouter(
    connect(
        mapStateToProps,
        mapDispatchToProps
    )(ServiceTab)
);
