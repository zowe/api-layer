import json
from datetime import datetime

from flask import Flask
from py_eureka_client import eureka_client as ec
from pyctuator.pyctuator import Pyctuator

DISCOVERY_SERVICE = "https://localhost:10011/eureka"

import ssl
import socket

import os, ssl, yaml

if (not os.environ.get('PYTHONHTTPSVERIFY', '') and
    getattr(ssl, '_create_unverified_context', None)):
    ssl._create_default_https_context = ssl._create_unverified_context


# https://localhost:10011/eureka/apps for the metadata

def register():
    parsedYaml = parseYaml()['instance']
    ec.init(eureka_server=DISCOVERY_SERVICE,
            app_name="python_enabler_service",
            instance_port=int(parsedYaml['port']),
            instance_host=parsedYaml['host'],
            instance_ip=parsedYaml['ipAddress'],
            home_page_url=parsedYaml['homePageUrl'],
            status_page_url=parsedYaml['statusPageUrl'],
            metadata=parsedYaml['metadata']
            )


def unregister():
    ec.stop()


# **** logic needs to be defined ****
def parseYaml():
    with open('service-configuration.yml') as f:
        data = yaml.load(f, Loader=yaml.FullLoader)
        return data


# ssl._create_default_https_context = ssl.VerifyFlags().VERIFY_DEFAULT


app = Flask(__name__)


# service/api/v1/
# https://localhost:10010/python_enabler_service/api/v1/registerInfo
@app.route("/registerInfo", methods=['GET'])
# **** Doesn't work if service isn't already registered ****
def registerPythonEnabler():
    register()
    return "Registered with Python eureka client to Discovery service"


@app.route("/hello", methods=['GET'])
def hello():
    return "Hello world in swagger"


# https://localhost:10010/python_enabler_service/api/v1/unregisterInfo from the gateway OR
# localhost:10018/unregisterInfo
@app.route("/unregisterInfo", methods=['GET'])
def unRegisterPythonEnabler():
    unregister()
    return "Unregistered Python eureka client from Discovery service"


# investigate how to get /python_enabler_service/application/info link on discovery service
# localhost:10018/python_enabler_service/application/info works if routed correctly on line 71, but link doesn't appear in discovery service.
# Only localhost:10018/application/info appears in discovery service
@app.route("/application/info", methods=['GET'])
def getApplicationInfo():
    data = {"build": {"name": "python-service", "operatingSystem": "Mac OS X (11.6.7)", "time": 1660222556.497000000,
                      "machine": "Amandas-MacBook-Pro.local", "number": "n/a", "version": "2.3.3-SNAPSHOT", "by": "<userId>",
                      "group": "api-layer", "artifact": "python-service"}}
    response = app.response_class(
        response=json.dumps(data),
        status=200,
        mimetype='application/json'
    )
    return response


@app.route("/pythonSwagger.json", methods=['GET'])
def getSwagger():
    with open('pythonSwagger.json') as f:
        data = yaml.load(f, Loader=yaml.FullLoader)
        response = app.response_class(
            response=json.dumps(data),
            status=200,
            mimetype='application/json'
        )
        return response


if __name__ == "__main__":
    register()
    # registerActuatorEndpoints()
    app.run(port=10018)
