#!/bin/sh
ACTION=

function usage {
    echo "APIML Certificate Management"
    echo "usage: apiml_cm.sh --action <action>"
    echo ""
    echo "  <action> action to be done:"
    echo "     - setup - setups APIML certificate management"
    echo "     - new-service - adds new service"
    echo ""
    echo "  apiml_cm.sh --action setup"
    echo "  apiml_cm.sh --action setup --local-ca-alias ... --local-ca-filename ... --local-ca-dname ... --local-ca-password ... --local-ca-validity ..."
    echo "  apiml_cm.sh --action new-service --service-alias <alias> --service-hostname <hostname> --service-keystore <keystore_path> --service-truststore <truststore_path> --service-dname <dname> --service-password <password> --service-validity <days>"
}

LOCAL_CA_ALIAS="localca"
LOCAL_CA_FILENAME="keystore/local_ca/localca"
LOCAL_CA_DNAME="CN=Zowe Development Instances Certificate Authority, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
LOCAL_CA_PASSWORD="local_ca_password"
LOCAL_CA_VALIDITY=3650

SERVICE_ALIAS="localhost"
SERVICE_PASSWORD="password"
SERVICE_KEYSTORE="keystore/localhost/localhost"
SERVICE_TRUSTSTORE="keystore/localhost/localhost"
SERVICE_DNAME="CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
SERVICE_EXT="SAN=dns:localhost.localdomain,dns:localhost"
SERVICE_VALIDITY=3650
SERVICE_HOSTNAME=

function clean_local_ca {
    if [[ -e ${LOCAL_CA_FILENAME}.keystore.p12 && -e ${LOCAL_CA_FILENAME}.cer ]];
    then
        rm -f ${LOCAL_CA_FILENAME}.keystore.p12 ${LOCAL_CA_FILENAME}.cer
    fi
}

function clean_service {
    if [[ -e ${SERVICE_KEYSTORE}.keystore.p12 && -e ${SERVICE_KEYSTORE}.csr && -e ${SERVICE_KEYSTORE}.cer && -e ${SERVICE_TRUSTSTORE}.truststore.p12 ]];
    then
        rm -f ${SERVICE_KEYSTORE}.keystore.p12 ${SERVICE_KEYSTORE}.csr ${SERVICE_KEYSTORE}_signed.cer ${SERVICE_TRUSTSTORE}.truststore.p12
    fi
}

function create_certificate_authority {
    echo "Generate keystore with the local CA private key and local CA public certificate:"
    keytool -genkeypair -v -alias ${LOCAL_CA_ALIAS} -keyalg RSA -keysize 2048 -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -dname "${LOCAL_CA_DNAME}" -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 -validity ${LOCAL_CA_VALIDITY} \
        -ext KeyUsage="keyCertSign" -ext BasicConstraints:"critical=ca:true"

    echo "Export local CA public certificate:"
    keytool -export -v -alias ${LOCAL_CA_ALIAS} -file ${LOCAL_CA_FILENAME}.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 -rfc \
        -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12
}

function create_service_certificate_and_csr {
    if [ ! -e "${SERVICE_KEYSTORE}.keystore.p12" ];
    then
        echo "Generate service private key and service:"
        keytool -genkeypair -v -alias ${SERVICE_ALIAS} -keyalg RSA -keysize 2048 -keystore ${SERVICE_KEYSTORE}.keystore.p12 -keypass ${SERVICE_PASSWORD} -storepass ${SERVICE_PASSWORD} \
            -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}

        echo "Generate CSR for the the service certificate:"
        keytool -certreq -v -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE}.keystore.p12 -storepass ${SERVICE_PASSWORD} -file ${SERVICE_KEYSTORE}.csr \
            -keyalg RSA -storetype PKCS12 -dname "${SERVICE_DNAME}" -validity ${SERVICE_VALIDITY}
    fi
}

function sign_csr_using_local_ca {
    echo "Sign the CSR using the Certificate Authority:"
    keytool -gencert -v -infile ${SERVICE_KEYSTORE}.csr -outfile ${SERVICE_KEYSTORE}_signed.cer -keystore ${LOCAL_CA_FILENAME}.keystore.p12 \
        -alias ${LOCAL_CA_ALIAS} -keypass ${LOCAL_CA_PASSWORD} -storepass ${LOCAL_CA_PASSWORD} -storetype PKCS12 \
        -ext ${SERVICE_EXT} -ext KeyUsage:critical=keyEncipherment,digitalSignature,nonRepudiation,dataEncipherment -ext ExtendedKeyUsage=clientAuth,serverAuth -rfc \
        -validity ${SERVICE_VALIDITY}
}

function import_signed_certificate_and_ca_certificate {
    echo "Import the Certificate Authority to the truststore:"
    keytool -importcert -v -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_TRUSTSTORE}.truststore.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the Certificate Authority to the keystore:"
    keytool -importcert -v -trustcacerts -noprompt -file ${LOCAL_CA_FILENAME}.cer -alias ${LOCAL_CA_ALIAS} -keystore ${SERVICE_KEYSTORE}.keystore.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12

    echo "Import the signed CSR to the keystore:"
    keytool -importcert -v -trustcacerts -noprompt -file ${SERVICE_KEYSTORE}_signed.cer -alias ${SERVICE_ALIAS} -keystore ${SERVICE_KEYSTORE}.keystore.p12 -storepass ${SERVICE_PASSWORD} -storetype PKCS12
}

function setup_local_ca {
    clean_local_ca
    create_certificate_authority
    ls -l  ${LOCAL_CA_FILENAME}*
}

function setup_local_apiml {
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
        * )                     usage
                                exit 1
    esac
    shift
    if [ "$ACTION" == "setup" ];
    then
        case $1 in
            --local-ca-alias )      shift
                                    LOCAL_CA_ALIAS=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --local-ca-filename )   shift
                                    LOCAL_CA_FILENAME=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --local-ca-dname )      shift
                                    LOCAL_CA_DNAME=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --local-ca-password )   shift
                                    LOCAL_CA_PASSWORD=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --local-ca-validity )   shift
                                    LOCAL_CA_VALIDITY=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
    else
        case $1 in
            --service-alias )       shift
                                    SERVICE_ALIAS=$1
                                    ;;
                * )                 usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-hostname )    shift
                                    SERVICE_HOSTNAME=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-keystore )    shift
                                    SERVICE_KEYSTORE=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-truststore )  shift
                                    SERVICE_TRUSTSTORE=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-dname )       shift
                                    SERVICE_DNAME=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-password )    shift
                                    SERVICE_PASSWORD=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
        case $1 in
            --service-validity )    shift
                                    SERVICE_VALIDITY=$1
                                    ;;
            * )                     usage
                                    exit 1
        esac
        shift
    fi
done

case $ACTION in
    setup)
        setup_local_ca
        setup_local_apiml
        ;;
    new-service)
        exit 0
        ;;
    *)
        usage
        exit 1
esac

