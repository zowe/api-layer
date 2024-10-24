import os
import yaml
import logging

logger = logging.getLogger(__name__)


class ConfigLoader:
    def __init__(self, config_file):
        # Assign the absolute path directly passed from app.py
        self.config_file = config_file

        # Print for debugging
        print(f"Configuration file path: {self.config_file}")

        self.config = self.load_config()

    def load_config(self):
        """Load configuration from a YAML file and merge with environment variables."""
        try:
            with open(self.config_file, 'r') as f:
                config = yaml.safe_load(f) or {}

            # Make sure 'instance' exists in the configuration
            config['instance'] = config.get('instance', {})

            # Override with environment variables if available, ensuring the correct structure
            config['instance']['host'] = os.getenv('INSTANCE_HOST', config['instance'].get('host'))

            # Adjust how you access the port, assuming the structure uses a nested '$' key
            port_value = config['instance'].get('port', {}).get('$', None)
            if port_value is not None:
                config['instance']['port'] = int(os.getenv('INSTANCE_PORT', port_value))

            config['instance']['ipAddress'] = os.getenv('INSTANCE_IP', config['instance'].get('ipAddress'))
            config['instance']['homePageUrl'] = os.getenv('HOMEPAGE_URL', config['instance'].get('homePageUrl'))
            config['instance']['statusPageUrl'] = os.getenv('STATUS_PAGE_URL', config['instance'].get('statusPageUrl'))

            return config
        except FileNotFoundError:
            logger.error("Configuration file not found.")
            return {}
        except yaml.YAMLError as e:
            logger.error(f"Error parsing YAML file: {e}")
            return {}
