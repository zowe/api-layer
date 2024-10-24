#!/usr/bin/env python3
# import custom_http_client
from py_eureka_client import eureka_client as ec
import importlib
import os
import ssl
import py_eureka_client.logger as eurekalogger
import logging


# Dynamically import the config module
config = importlib.import_module('onboarding-enabler-python.config')
httpClient = importlib.import_module('onboarding-enabler-python.custom_http_client')
ConfigLoader = getattr(config, 'ConfigLoader')

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)
eurekalogger.set_level("DEBUG")


def unregister():
    """Unregister the service from the Discovery Service."""
    try:
        ec.stop()
        logger.info("Service unregistered successfully.")
    except Exception as e:
        logger.error(f"Error during unregistration: {e}")


class PythonEnabler:
    logger.info("PythonEnabler initialized")

    def __init__(self, config_file='service-configuration.yml', discovery_service=None):
        self.config_loader = ConfigLoader(config_file)
        self.config_data = self.config_loader.config
        self.discovery_service = discovery_service or self.get_discovery_service()
        self.ssl_config = self.config_data.get('ssl', {})

        # Set up SSL environment if SSL is enabled
        self.setup_ssl_environment()

    def get_discovery_service(self):
        """Construct the discovery service URL based on the configuration."""
        config = self.config_data.get('eureka', {})
        protocol = "https" if config.get('ssl', False) else "http"
        host = config.get('host', 'localhost')
        port = config.get('port', '10011')
        service_path = config.get('servicePath', '/eureka')
        return f"{protocol}://{host}:{port}{service_path}"

    def setup_ssl_environment(self):
        """Set up SSL environment variables if SSL is enabled."""
        if self.ssl_config.get('enabled', False):
            ca_file = self.ssl_config.get('caFile')
            if ca_file:
                os.environ['REQUESTS_CA_BUNDLE'] = ca_file

            cert_file = self.ssl_config.get('certificate')
            key_file = self.ssl_config.get('keystore')
            if cert_file and key_file:
                os.environ['CLIENT_CERT'] = cert_file
                os.environ['CLIENT_KEY'] = key_file

            logger.info("SSL environment configured.")
            # logger.info({cert_file})
            # logger.info({key_file})

    def register(self):
        """Register the service with the Discovery Service, using SSL if configured."""
        instance_config = self.config_data.get('instance', {})
        if instance_config:
            try:
                # Extract the instance port correctly, ensuring it's an integer
                instance_port = instance_config.get('port')
                if isinstance(instance_port, dict):
                    instance_port = instance_port.get('$')

                instance_host = instance_config.get('hostName')
                instance_ip = instance_config.get('ipAddr')

                if not instance_host or not instance_ip or not instance_port:
                    raise ValueError("Instance configuration is incomplete.")

                logger.info(f"Registering with service URL: {self.discovery_service}")

                ec.init(
                    eureka_server=self.discovery_service,
                    eureka_protocol="https",
                    app_name=instance_config.get('app', 'python_enabler_service'),
                    instance_port=int(instance_port),
                    instance_host=instance_host,
                    instance_ip=instance_ip,
                    instance_id=instance_config.get("instanceId"),
                    vip_adr=instance_config.get("vipAddress"),
                    instance_secure_port_enabled=True,
                    instance_secure_port=10018,
                    secure_vip_addr=instance_config.get("secureVipAddress"),
                    home_page_url=instance_config.get('homePageUrl'),
                    metadata=instance_config.get('metadata', {}),
                    data_center_name=instance_config.get('dataCenterInfo', {}),
                )
                logger.info("Service registered successfully with SSL.")
            except Exception as e:
                logger.error(f"Error during registration: {e}")


# Example usage
if __name__ == "__main__":
    enabler = PythonEnabler()
    enabler.register()
