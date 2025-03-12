"""
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
"""

import importlib.util
import sys
import os
from fastapi import FastAPI
from fastapi.responses import JSONResponse
import ssl
import yaml
import json
import uvicorn

# Add the parent directory of 'onboarding-enabler-python' to sys.path
base_directory = os.path.dirname(os.path.abspath(__file__))
parent_directory = os.path.abspath(os.path.join(base_directory, '..'))
sys.path.insert(0, parent_directory)

# Define the path to the registration module file
module_name = "registration"
module_path = os.path.join(parent_directory, 'onboarding-enabler-python', 'src', 'registration.py')

# Dynamically load the module
spec = importlib.util.spec_from_file_location(module_name, module_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

# Access the PythonEnabler class dynamically
PythonEnabler = module.PythonEnabler

# Construct the absolute path to the configuration file
config_file_path = os.path.join(base_directory, 'service-configuration.yml')

# Initialize the enabler using the SDK
enabler = PythonEnabler(config_file=config_file_path)

# Load SSL configuration from service-configuration.yml
ssl_config = enabler.ssl_config
cert_file = ssl_config.get("certificate")
key_file = ssl_config.get("keystore")

ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS)
ssl_context.load_cert_chain(certfile=cert_file, keyfile=key_file)

app = FastAPI(title="Python Sample Service", description="FastAPI implementation of Python Sample Service")

@app.get("/pythonservice/registerInfo")
def register_python_enabler():
    """Test Endpoint to manually register the service."""
    enabler.register()
    return {"message": "Registered with Python eureka client to Discovery service"}

@app.get("/pythonservice/unregisterInfo")
def unregister_python_enabler():
    """Test Endpoint to manually unregister the service."""
    enabler.unregister()
    return {"message": "Unregistered Python eureka client from Discovery service"}


@app.get("/pythonservice/hello")
def hello():
    """Simple hello endpoint for testing."""
    return {"message": "Hello world in swagger"}


@app.get("/pythonservice/apidoc")
def get_swagger():
    with open('pythonSwagger.json') as f:
        data = yaml.safe_load(f)
    return JSONResponse(content=data)


@app.get("/pythonservice/application/info")
def get_application_info():
    return {
        "build": {
            "name": "python-service",
            "operatingSystem": "Mac OS X (11.6.7)",
            "time": 1660222556.497,
            "machine": "Amandas-MacBook-Pro.local",
            "number": "n/a",
            "version": "2.3.0",
        }
    }


@app.get("/pythonservice/application/health")
def get_application_health():
    return {"status": "UP"}


if __name__ == "__main__":
    # Load SSL configuration
    enabler.register()
    uvicorn.run(app, host="0.0.0.0", port=10018, ssl_certfile="../keystore/localhost/localhost.keystore.cer",
                ssl_keyfile="../keystore/localhost/localhost.keystore.key")
