import importlib.util
import sys
import os
from flask import Flask, jsonify
import ssl

# Add the parent directory of 'onboarding-enabler-python' to sys.path
base_directory = os.path.dirname(os.path.abspath(__file__))
parent_directory = os.path.abspath(os.path.join(base_directory, '..'))
sys.path.insert(0, parent_directory)

# Define the path to the registration module file
module_name = "registration"
module_path = os.path.join(parent_directory, 'onboarding-enabler-python', 'registration.py')

# Dynamically load the module
spec = importlib.util.spec_from_file_location(module_name, module_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

# Access the PythonEnabler class dynamically
PythonEnabler = module.PythonEnabler

app = Flask(__name__)

# Construct the absolute path to the configuration file
config_file_path = os.path.join(base_directory, 'service-configuration.yml')

# Initialize the enabler using the SDK
enabler = PythonEnabler(config_file=config_file_path)

ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS)
ssl_context.load_cert_chain(
    "../keystore/localhost/localhost.keystore.cer",  # Path to your certificate file
    "../keystore/localhost/localhost.keystore.key"   # Path to your key file
)

@app.route("/registerInfo", methods=['GET'])
def register_python_enabler():
    """Endpoint to manually register the service."""
    enabler.register()
    return jsonify({"message": "Registered with Python eureka client to Discovery service"})

@app.route("/hello", methods=['GET'])
def hello():
    """Simple hello endpoint for testing."""
    return jsonify({"message": "Hello world in swagger"})

@app.route("/unregisterInfo", methods=['GET'])
def unregister_python_enabler():
    """Endpoint to manually unregister the service."""
    enabler.unregister()
    return jsonify({"message": "Unregistered Python eureka client from Discovery service"})

if __name__ == "__main__":
    # Load SSL configuration
    ssl_context = (
        "../keystore/localhost/localhost.keystore.cer",  # Path to your certificate file
        "../keystore/localhost/localhost.keystore.key"  # Path to your key file
    )

    # Register the service on startup
    enabler.register()
    app.run(port=10018, ssl_context=ssl_context)
