import { Link, Text, Tooltip } from 'mineral-ui';
import { Fragment, Component } from 'react';
import Shield from '../ErrorBoundary/Shield/Shield';
import '../Swagger/Swagger.css';
import SwaggerContainer from '../Swagger/SwaggerContainer';
import './ServiceTab.css';
import ServiceVersionDiffContainer from '../ServiceVersionDiff/ServiceVersionDiffContainer';

export default class ServiceTab extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedVersion: null,
        };
    }

    get basePath() {
        const { selectedService } = this.props;
        const { selectedVersion } = this.state;

        let basePath = '';
        if (selectedService.basePath) {
            const version = selectedVersion || selectedService.defaultApiVersion;
            basePath = selectedService.basePath.replace('{api-version}', version);
        }
        return basePath;
    }

    get currentService() {
        let currentService = null;

        const {
            match: {
                params: { tileID, serviceId },
            },
            tiles,
            selectedService,
            selectedTile,
            selectService,
        } = this.props;

        tiles[0].services.forEach(service => {
            if (service.serviceId === serviceId) {
                if (service.serviceId !== selectedService.serviceId || selectedTile !== tileID) {
                    selectService(service, tileID);
                }
                currentService = service;
            }
        });

        return currentService;
    }

    get hasHomepage() {
        const { selectedService } = this.props;
        return (
            selectedService.homePageUrl !== null &&
            selectedService.homePageUrl !== undefined &&
            selectedService.homePageUrl.length > 0
        );
    }

    get apiVersions() {
        let apiVersions = [];

        const { selectedVersion } = this.state;
        const { currentService } = this;

        if (currentService && currentService.apiVersions) {
            apiVersions = currentService.apiVersions.map(version => {
                // Pre select default version or if only one version exists select that
                let tabStyle = {};
                if (
                    selectedVersion === null &&
                    (currentService.defaultApiVersion === version || currentService.apiVersions.length === 1)
                ) {
                    tabStyle = { backgroundColor: '#fff' };
                }
                if (selectedVersion === version) {
                    tabStyle = { backgroundColor: '#fff' };
                }
                return (
                    // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions
                    <span
                        className="nav-tab"
                        key={version}
                        style={tabStyle}
                        onClick={() => {
                            this.setState({ selectedVersion: version });
                        }}
                    >
                        <Text className="version-text">{version}</Text>
                    </span>
                );
            });
            if (apiVersions.length >= 2) {
                apiVersions.push(
                    // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions
                    <span
                        className="nav-tab"
                        onClick={() => {
                            this.setState({ selectedVersion: 'diff' });
                        }}
                        style={selectedVersion === 'diff' ? { backgroundColor: '#fff' } : {}}
                        key="diff"
                    >
                        <Text className="version-text">Compare</Text>
                    </span>
                );
            }
        }
        return apiVersions;
    }

    render() {
        const {
            match: {
                params: { serviceId },
            },
            tiles,
            selectedService,
        } = this.props;
        if (tiles === null || tiles === undefined || tiles.length === 0) {
            throw new Error('No tile is selected.');
        }

        const { selectedVersion } = this.state;
        const { basePath } = this;
        const { currentService } = this;
        const { hasHomepage } = this;
        const { apiVersions } = this;
        const message = 'The API documentation was retrieved but could not be displayed.';
        const sso = selectedService.ssoAllInstances ? 'supported' : 'not supported';

        return (
            <Fragment>
                {currentService === null && (
                    <Text element="h3" style={{ margin: '0 auto', background: '#ffff', width: '100vh' }}>
                        <br />
                        <br />
                        <p style={{ marginLeft: '122px' }}>This tile does not contain service "{serviceId}"</p>
                    </Text>
                )}
                <Shield title={message}>
                    <Fragment>
                        <div className="serviceTab">
                            <div className="header">
                                <Text element="h2">{selectedService.title}</Text>
                                {hasHomepage && (
                                    <Fragment>
                                        {selectedService.status === 'UP' && (
                                            <Tooltip
                                                key={selectedService.serviceId}
                                                content="Open Service Homepage"
                                                placement="bottom"
                                            >
                                                <Link href={selectedService.homePageUrl}>
                                                    <strong>Service Homepage</strong>
                                                </Link>
                                            </Tooltip>
                                        )}
                                        {selectedService.status === 'DOWN' && (
                                            <Tooltip
                                                key={selectedService.serviceId}
                                                content="API Homepage navigation is disabled as the service is not running"
                                                placement="bottom"
                                            >
                                                <Link variant="danger">
                                                    <strong>API Homepage</strong>
                                                </Link>
                                            </Tooltip>
                                        )}
                                    </Fragment>
                                )}
                                <br />
                                <br />
                                <div className="apiInfo-item">
                                    <Tooltip
                                        key={basePath}
                                        content="The path used by the Gateway to access API endpoints. This can be used to identify a service in client tools like Zowe CLI and Zowe explorer."
                                        placement="bottom"
                                    >
                                        <Text>
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="apiBasePath">API Base Path:</label>
                                            <span id="apiBasePath">{basePath}</span>
                                        </Text>
                                    </Tooltip>
                                    <br />
                                    <Tooltip
                                        key={selectedService.serviceId}
                                        content="The identifier for this service"
                                        placement="bottom"
                                    >
                                        <Text>
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="serviceId">Service ID:</label>
                                            <span id="serviceId">{selectedService.serviceId}</span>
                                        </Text>
                                    </Tooltip>
                                    <br />
                                    <Tooltip
                                        key={selectedService.ssoAllInstances}
                                        content="All the instances of this service claim support of the SSO using Zowe API ML JWT tokens"
                                        placement="bottom"
                                    >
                                        <Text>
                                            {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                                            <label htmlFor="sso">SSO:</label>
                                            <span id="sso">{sso}</span>
                                        </Text>
                                    </Tooltip>
                                </div>

                                <Text style={{ marginTop: '15px' }}>{selectedService.description}</Text>
                            </div>
                            <div className="tabs-container" style={{ width: '100%' }}>
                                {apiVersions}
                            </div>
                            {selectedVersion !== 'diff' ? (
                                <SwaggerContainer selectedVersion={selectedVersion} />
                            ) : (
                                <ServiceVersionDiffContainer
                                    serviceId={selectedService.serviceId}
                                    versions={currentService.apiVersions}
                                />
                            )}
                        </div>
                    </Fragment>
                </Shield>
            </Fragment>
        );
    }
}
