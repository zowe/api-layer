#!/bin/sh

ACTION=
function usage {
    echo "APIML Certificate Management"
    echo "usage: apiml_cm.sh --action <action>"
    echo ""
    echo "  <action> action to be done:"
    echo "     - setup - setups APIML certificate management"
    echo "     - new-service - adds new service"
    echo "     - clean - removes files created by setup"
    echo ""
    echo "  See keystore/README.md for more details"
}

ACTION=

LOCAL_CA_ALIAS="localca"
LOCAL_CA_FILENAME="keystore/local_ca/localca"
LOCAL_CA_DNAME="CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
LOCAL_CA_PASSWORD="local_ca_password"
LOCAL_CA_VALIDITY=3650

SERVICE_ALIAS="localhost"
SERVICE_PASSWORD="password"
SERVICE_KEYSTORE="keystore/localhost/localhost.keystore.p12"
SERVICE_TRUSTSTORE="keystore/localhost/localhost.truststore.p12"
SERVICE_DNAME="CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
SERVICE_EXT="SAN=dns:localhost.localdomain,dns:localhost"
SERVICE_VALIDITY=3650

function pkeytool {
    ARGS=$@
    echo "Calling keytool $ARGS"
    keytool "$@"
}

function clean_local_ca {
    rm -f ${LOCAL_CA_FILENAME}.keystore.p12 ${LOCAL_CA_FILENAME}.cer
}

function clean_service {
    rm -f ${SERVICE_KEYSTORE} ${SERVICE_KEYSTORE}.csr ${SERVICE_KEYSTORE}_signed.cer ${SERVICE_TRUSTSTORE}
}

function create_certificate_authority {
    echo "Generate keystore with the local CA private key and local CA public certificate:"
    pkeytool -genkeypair -v -alias ${LOCAL_CA_ALIAS} -keyalg RSA -keysize 2048 -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -dname "${LOCAL_CA_DNAME}" -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 -validity ${LOCAL_CA_VALIDITY} \
        -ext KeyUsage="keyCertSign" -ext BasicConstraints:"critical=ca:true"

    echo "Export local CA public certificate:"
    pkeytool -export -v -alias ${LOCAL_CA_ALIAS} -file ${LOCAL_CA_FILENAME}.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 -rfc \
        -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12
}

function create_service_certificate_and_csr {
    if [ ! -e "${SERVICE_KEYSTORE}.keystore.p12" ];
    then
        echo "Generate service private key and service:"
        pkeytool -genkeypair -v -alias ${SERVICE_ALIAS} -keyalg RSA -keysize 2048 -keystore ${SERVICE_KEYSTORE} -keypass ${SERVICE_PASSWORD} -storepass ${SERVICE_PASSWORD} \
            -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}

        echo "Generate CSR for the the service certificate:"
        pkeytool -certreq -v -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE} -storepass ${SERVICE_PASSWORD} -file ${SERVICE_KEYSTORE}.csr \
            -keyalg RSA -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}
    fi
}

function sign_csr_using_local_ca {
    echo "Sign the CSR using the Certificate Authority:"
    pkeytool -gencert -v -infile ${SERVICE_KEYSTORE}.csr -outfile ${SERVICE_KEYSTORE}_signed.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -alias ${LOCAL_CA_ALIAS} -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 \
        -ext ${SERVICE_EXT} -ext KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment -ext ExtendedKeyUsage=clientAuth,serverAuth -rfc \
        -validity ${SERVICE_VALIDITY}
}

function import_signed_certificate_and_ca_certificate {
    echo "Import the Certificate Authority to the truststore:"
    pkeytool -importcert -v -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_TRUSTSTORE} -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the Certificate Authority to the keystore:"
    pkeytool -importcert -v -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_KEYSTORE} -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the signed CSR to the keystore:"
    pkeytool -importcert -v -trustcacerts -noprompt -file ${SERVICE_KEYSTORE}_signed.cer -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE} -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function setup_local_ca {
    clean_local_ca
    create_certificate_authority
    ls -l  ${LOCAL_CA_FILENAME}*
}

function new_service {
    clean_service
    create_service_certificate_and_csr
    sign_csr_using_local_ca
    import_signed_certificate_and_ca_certificate
    ls ${SERVICE_KEYSTORE}*
}

while [ "$1" != "" ]; do
    case $1 in
        -a | --action )         shift
                                ACTION=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        --local-ca-alias )      shift
                                LOCAL_CA_ALIAS=$1
                                ;;
        --local-ca-filename )   shift
                                LOCAL_CA_FILENAME=$1
                                ;;
        --local-ca-dname )      shift
                                LOCAL_CA_DNAME=$1
                                ;;
        --local-ca-password )   shift
                                LOCAL_CA_PASSWORD=$1
                                ;;
        --local-ca-validity )   shift
                                LOCAL_CA_VALIDITY=$1
                                ;;
        --service-alias )       shift
                                SERVICE_ALIAS=$1
                                ;;
        --service-ext )         shift
                                SERVICE_EXT=$1
                                ;;
        --service-keystore )    shift
                                SERVICE_KEYSTORE=$1
                                ;;
        --service-truststore )  shift
                                SERVICE_TRUSTSTORE=$1
                                ;;
        --service-dname )       shift
                                SERVICE_DNAME=$1
                                ;;
        --service-password )    shift
                                SERVICE_PASSWORD=$1
                                ;;
        --service-validity )    shift
                                SERVICE_VALIDITY=$1
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

case $ACTION in
    clean)
        clean_local_ca
        clean_service
        ;;
    setup)
        setup_local_ca
        new_service
        ;;
    new-service)
        new_service
        ;;
    *)
        usage
        exit 1
esac

