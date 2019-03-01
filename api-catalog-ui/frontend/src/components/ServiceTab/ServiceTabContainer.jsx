import {connect} from 'react-redux';
import {withRouter} from 'react-router-dom';
import {fetchTilesStop} from '../../actions/catalog-tile-actions';
import {selectService} from '../../actions/selected-service-actions';
import ServiceTab from './ServiceTab';

const mapStateToProps = state => ({
    tiles: state.tilesReducer.tiles,
    selectedService: state.selectedServiceReducer.selectedService,
    selectedTile: state.selectedServiceReducer.selectedTile
});

const mapDispatchToProps = dispatch => ({
    fetchTilesStop: () => dispatch(fetchTilesStop()),
    selectService: (service, tileId) => dispatch(selectService(service, tileId)),
});

export default withRouter(
    connect(
        mapStateToProps,
        mapDispatchToProps,
    )(ServiceTab),
);
