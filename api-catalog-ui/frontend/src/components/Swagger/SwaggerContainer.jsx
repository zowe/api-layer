import { connect } from 'react-redux';
import Swagger from './Swagger';

const mapStateToProps = state => ({
    selectedService: state.selectedServiceReducer.selectedService,
});

const mapDispatchToProps = {};

export default connect(
    mapStateToProps,
    mapDispatchToProps,
)
(Swagger);
