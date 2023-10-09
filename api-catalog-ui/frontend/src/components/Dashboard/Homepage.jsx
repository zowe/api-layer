/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Typography, IconButton, Button } from '@material-ui/core';
import { Alert } from '@mui/material';
import React, { Component } from 'react';
import HomepageHeader from '../../assets/images/homepage_header.svg';
import Enterprise from '../../assets/images/enterprise.svg';
import Mainframe from '../../assets/images/mainframe.svg';
import Payment from '../../assets/images/payment.svg';
import Security from '../../assets/images/security.svg';
import Storage from '../../assets/images/storage.svg';
import BroadcomLogo from '../../assets/images/broadcom_logo.svg';
import Code from '../../assets/images/code.svg';
import ZoweLogo from '../../assets/images/zowe_logo.svg';
import productImage from '../../assets/images/api-catalog-logo.png';
import Footer from '../Footer/Footer';
import SearchCriteria from '../Search/SearchCriteria';
import Shield from '../ErrorBoundary/Shield/Shield';
import ServicesNavigationBarContainer from '../ServicesNavigationBar/ServicesNavigationBarContainer';
import Tile from '../Tile/Tile';
import Spinner from '../Spinner/Spinner';
import formatError from '../Error/ErrorFormatter';
import ErrorDialog from '../Error/ErrorDialog';
import WizardContainer from '../Wizard/WizardContainer';
import DialogDropdown from '../Wizard/DialogDropdown';
import { enablerData } from '../Wizard/configs/wizard_onboarding_methods';
import ConfirmDialogContainer from '../Wizard/ConfirmDialogContainer';
import { isAPIPortal, openMobileMenu, closeMobileMenu } from '../../utils/utilFunctions';
import MenuCloseImage from '../../assets/images/xmark.svg';

const loadFeedbackButton = () => {
    if (isAPIPortal()) {
        return import('../FeedbackButton/FeedbackButton');
    }
    return Promise.resolve(null);
};

const FeedbackButton = React.lazy(loadFeedbackButton);

export default class Homepage extends Component {
    componentDidMount() {
        if (isAPIPortal()) {
            const goBackButton = document.getElementById('go-back-button-portal');
            if (goBackButton) {
                goBackButton.style.display = 'none';
            }
        }
        const { fetchTilesStart, clearService } = this.props;
        clearService();
        fetchTilesStart();
    }

    componentWillUnmount() {
        const { fetchTilesStop, clear } = this.props;
        clear();
        fetchTilesStop();
    }

    handleSearch = (value) => {
        const { filterText } = this.props;
        filterText(value);
    };

    refreshStaticApis = () => {
        const { refreshedStaticApi } = this.props;
        refreshedStaticApi();
    };

    toggleWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    handleClose = () => {
        const { closeAlert } = this.props;
        closeAlert();
    };

    render() {
        const cardClick = (type) => {
          console.log('type', type);
        }
        const {
          tiles,
          history,
          searchCriteria,
          isLoading,
          fetchTilesError,
          fetchTilesStop,
          refreshedStaticApisError,
          clearError,
          authentication,
          storeCurrentTileId,
        } = this.props;
        const hasTiles = !fetchTilesError && tiles && tiles.length > 0;
        const hasSearchCriteria =
            typeof searchCriteria !== 'undefined' &&
            searchCriteria !== undefined &&
            searchCriteria !== null &&
            searchCriteria.length > 0;
        return (
          <>
            <div className="header">
              <div className="product-name">
                <div className="app-icon-container">
                  <img id="logo" alt="API Catalog" src={productImage} />
                </div>
              </div>
            </div>
            <div className="main-content homepage-content">
                {isAPIPortal() && <FeedbackButton />}
                <div className='homepage-header'>
                  <div>
                    <h1>Start Developing for Mainframe</h1>
                    <h3>All the resources you need to develop for the Mainframe</h3>
                    <Button
                        className="button-cta"
                        color="primary"
                        size="medium"
                        onClick={openMobileMenu}
                    >
                        Explore APIs
                    </Button>
                  </div>
                  <div className='homepage-header-images'>
                    <img src={Security} className='homepage-security-img homepage-header-img' />
                    <img src={Payment} className='homepage-payment-img homepage-header-img' />
                    <img src={Mainframe} className='homepage-mainframe-img homepage-header-img' />
                    <img src={Enterprise} className='homepage-enterprise-img homepage-header-img' />
                    <img src={Storage} className='homepage-storage-img homepage-header-img' />
                    <img src={HomepageHeader} className='homepage-main-img' />
                  </div>
                </div>
                <h2 className="header-primary">Have you ever wondered how to start developing for Mainframe?</h2>
                <div className='homepage-cards'>
                  <div>
                    <Button onClick={openMobileMenu}>
                      <img src={Code} />
                      <h5>Explore APIs</h5>
                      <p>
                        Essential tools for web development that allow different applications to communicate and exchange data
                      </p>
                    </Button>
                    <Button target="_blank" href="https://techdocs.broadcom.com/">
                      <img src={BroadcomLogo} />
                      <h5>Broadcom TechDocs</h5>
                      <p>
                        Provides clear and concise information on how to use, maintain, and troubleshoot the software product
                      </p>
                    </Button>
                  </div>
                  <div>
                    <Button target="_blank" href="https://docs.zowe.org">
                      <img src={ZoweLogo} />
                      <h5>Zowe Docs</h5>
                      <p>
                      Zowe is an open source framework for leveraging data and applications in z/OS from modern applications and tools.
                      </p>
                    </Button>
                    <Button target="_blank" href="https://docs.zowe.org/stable/extend/extend-apiml/authentication-for-apiml-services/#authentication-endpoints">
                      <img src={ZoweLogo} />
                      <h5>Zowe Authentication</h5>
                      <p>
                        Learn more about how to establish secured connection
                      </p>
                    </Button>
                  </div>
                </div>
                
                <div>
                  <div className="nav-bar dashboard-mobile-menu homepage-menu">
                    {isLoading && <div className="loadingDiv" />}

                    <div className="mobile-menu-close-ctn">
                      <Button
                          className="mobile-menu-close-btn icon-btn"
                          aria-label="close-menu"
                          onClick={closeMobileMenu}
                      >
                          <img alt="Menu" src={MenuCloseImage} className="mobile-menu-close" />
                      </Button>
                  </div>

                    <div id="search">
                      <Shield title="Search Bar is broken !">
                        <SearchCriteria
                            id="search-input"
                            placeholder="Search for APIs..."
                            doSearch={this.handleSearch}
                        />
                      </Shield>
                    </div>

                    <Typography id="serviceIdTabs" variant="h5">Product APIs</Typography>
                    <div className="homepage-menu-content">
                      {hasTiles &&
                          tiles.map((tile) =>
                              tile.services.map((service) => (
                                  <Tile
                                    storeCurrentTileId={storeCurrentTileId}
                                    service={service}
                                    key={service}
                                    tile={tile}
                                    history={history}
                                  />
                              ))
                          )}
                      {!hasTiles && hasSearchCriteria && (
                          <Typography id="search_no_results" variant="subtitle2" className="no-content">
                              No services found matching search criteria
                          </Typography>
                      )}
                    </div>
                  </div>
                  <Footer />
              </div>
            </div>
          </>
        );
    }
}
