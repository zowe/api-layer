import { connect } from 'react-redux';
import { withRouter } from 'react-router-dom';
import ServiceTab from './ServiceTab';
import { fetchTilesStop } from "../../actions/catalog-tile-actions";

const mapStateToProps = state => ({
    tiles: state.tilesReducer.tiles,
});

const mapDispatchToProps = dispatch => ({
    fetchTilesStop: () => dispatch(fetchTilesStop()),
});

export default withRouter(
    connect(
        mapStateToProps,
        mapDispatchToProps
    )(ServiceTab)
);
