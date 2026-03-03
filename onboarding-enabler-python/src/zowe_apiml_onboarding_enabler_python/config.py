"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""
"""
Copyright 2026 Contributors to the Zowe Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import yaml
import logging

logger = logging.getLogger(__name__)


class ConfigLoader:
    """
    Utility class to load YAML configuration files.

    Attributes:
        config_file (str): Absolute path to the configuration file.
        config (dict): Parsed YAML configuration.

    Methods:
        load_config():
            Loads and parses the YAML file. Logs error if file is missing or malformed.
    """

    def __init__(self, config_file):
        # Assign the absolute path directly passed from app.py
        self.config_file = config_file

        self.config = self.load_config()

    def load_config(self):
        """Load configuration from a YAML file and merge with environment variables."""
        try:
            with open(self.config_file, 'r') as f:
                config = yaml.safe_load(f) or {}
            return config
        except FileNotFoundError:
            logger.error("Configuration file not found. Ensure that it's located inside the config folder.")
            return {}
        except yaml.YAMLError as e:
            logger.error(f"Error parsing YAML file: {e}")
            return {}
