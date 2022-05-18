from flask import Flask
from py_eureka_client import eureka_client as ec

DISCOVERY_SERVICE = "https://localhost:10011/eureka"

import ssl
import socket

import os, ssl
if (not os.environ.get('PYTHONHTTPSVERIFY', '') and
    getattr(ssl, '_create_unverified_context', None)):
    ssl._create_default_https_context = ssl._create_unverified_context

def register():
    ec.init(eureka_server=DISCOVERY_SERVICE,
            app_name="python_enabler_service",
            instance_port=5000,
            instance_host="localhost",
            metadata={"apiml.service.id":"PYTHON_ENABLER_SERVICE",
                      "apiml.routes.api-v1.gatewayUrl": "api/v1", "apiml.routes.api-v1.serviceUrl": "/",
                      "apiml.catalog.tile.id": "pythonen",
                      "apiml.catalog.tile.title": "Python Enabler Service",
                      "apiml.catalog.tile.version": "1.0.1",
                      "apiml.catalog.tile.description": "A sample python enabler used for onboarding",
                      "apiml.service.title": "My Python Enabler",
                      "apiml.service.description": "Sample python enabler"
                      }
            )

register()

# ssl._create_default_https_context = ssl.VerifyFlags().VERIFY_DEFAULT


app = Flask(__name__)

@app.route("/info", methods=['GET'])
def hello():
    return "Registered with Python eureka client to Discovery service"


if __name__ == "__main__":
    app.run()
