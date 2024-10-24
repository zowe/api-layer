"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""

import importlib
import logging
import ssl

import py_eureka_client.logger as eurekalogger
from py_eureka_client import eureka_client as ec

# Dynamically import the config module
config = importlib.import_module('onboarding-enabler-python.config')
httpClient = importlib.import_module('onboarding-enabler-python.custom_http_client')
ConfigLoader = getattr(config, 'ConfigLoader')

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)
eurekalogger.set_level("DEBUG")


class PythonEnabler:
    logger.info("Python Onboarding Enabler initialized")

    def __init__(self, config_file='service-configuration.yml', discovery_service=None):
        self.ssl_context = None
        self.config_loader = ConfigLoader(config_file)
        self.config_data = self.config_loader.config
        self.discovery_service = discovery_service or self.get_discovery_service()
        self.ssl_config = self.config_data.get('ssl', {})

        # Set up SSL environment if SSL is enabled
        if self.config_data.get('instance', {}).get('scheme') == 'https':
            self.setup_ssl_environment()

    def get_discovery_service(self):
        """Construct the discovery service URL based on the configuration."""
        eureka_config = self.config_data.get('eureka', {})
        protocol = "https" if eureka_config.get('ssl', False) else "http"
        host = eureka_config.get('host', 'localhost')
        port = eureka_config.get('port', '10011')
        service_path = eureka_config.get('servicePath', '/eureka')
        return f"{protocol}://{host}:{port}{service_path}"

    def setup_ssl_environment(self):
        """Set up SSL environment variables if SSL is enabled."""
        ca_file = self.ssl_config.get('caFile')
        cert_file = self.ssl_config.get('certificate')
        key_file = self.ssl_config.get('keystore')

        if cert_file and key_file and ca_file:
            ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
            ssl_context.load_cert_chain(certfile=cert_file, keyfile=key_file)
            ssl_context.load_verify_locations(cafile=ca_file)
            self.ssl_context = ssl_context
            logger.info("SSL environment configured.")
        else:
            logger.error("SSL configuration is incomplete. Certificate, key, and CA files must be provided.")
            self.ssl_context = None

    def register(self):
        """Register the service with the Discovery Service, using SSL if configured."""
        instance_config = self.config_data.get('instance', {})
        if instance_config:
            try:
                instance_port = instance_config.get('port')
                secure_instance_port = instance_config.get('securePort')
                instance_host = instance_config.get('hostName')
                instance_ip = instance_config.get('ipAddr')
                scheme = instance_config.get('scheme')

                # Validation based on scheme
                if scheme == 'https':
                    if not instance_host or not instance_ip or not secure_instance_port:
                        raise ValueError("Instance configuration for HTTPS is incomplete.")
                else:
                    if not instance_host or not instance_ip or not instance_port:
                        raise ValueError("Instance configuration is incomplete.")

                logger.info(f"Registering with Eureka service with URL: {self.discovery_service}")

                ec.init(
                    eureka_server=self.discovery_service,
                    eureka_protocol=scheme,
                    app_name=instance_config.get('app'),
                    instance_port=int(instance_port) if instance_port else None,
                    instance_unsecure_port_enabled=instance_config.get('portEnabled', False),
                    instance_host=instance_host,
                    instance_ip=instance_ip,
                    instance_id=instance_config.get("instanceId"),
                    vip_adr=instance_config.get("vipAddress"),
                    instance_secure_port_enabled=instance_config.get('securePortEnabled', False),
                    instance_secure_port=int(secure_instance_port) if secure_instance_port else None,
                    secure_vip_addr=instance_config.get("secureVipAddress"),
                    home_page_url=instance_config.get('homePageUrl'),
                    status_page_url=instance_config.get("statusPageUrl"),
                    health_check_url=instance_config.get("healthCheckUrl"),
                    metadata=instance_config.get('metadata', {}),
                )
                logger.info("Service registered successfully.")
            except Exception as e:
                logger.error(f"Error during registration: {e}")

    def unregister(self):
        """Unregister the service from the Discovery Service."""
        try:
            ec.stop()
            logger.info("Service unregistered successfully.")
        except Exception as e:
            logger.error(f"Error during un-registration: {e}")
