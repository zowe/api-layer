#!/usr/bin/env bash
set -ex
#WORKSPACE_DIR=/zaas1/sdkbld1/pt2
WORKSPACE_DIR=/z/sdkbld/cp
zowe uss issue ssh "mkdir -p $WORKSPACE_DIR"
zowe zos-files upload file-to-uss "CheckPerm.java" "$WORKSPACE_DIR/CheckPerm.java"
zowe uss issue ssh "javac CheckPerm.java && ls -E" --cwd $WORKSPACE_DIR
zowe uss issue ssh "java -Xquickstart CheckPerm SDKBLD XSPATNY ZOWE2 BADZOWE APIML.SERVICES BADAPIML 1 4" --cwd $WORKSPACE_DIR
zowe zos-files download uss-file "$WORKSPACE_DIR/CheckPerm.class" -b -f "build/CheckPerm.class"
