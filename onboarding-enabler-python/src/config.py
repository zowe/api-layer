"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""
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
            return config
        except FileNotFoundError:
            logger.error("Configuration file not found.")
            return {}
        except yaml.YAMLError as e:
            logger.error(f"Error parsing YAML file: {e}")
            return {}
