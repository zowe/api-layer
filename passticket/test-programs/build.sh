#!/usr/bin/env bash
WORKSPACE_DIR=/zaas1/sdkbld1/pt2
zowe uss issue ssh "mkdir -p $WORKSPACE_DIR"
zowe zos-files upload file-to-uss "PtEval.java" "$WORKSPACE_DIR/PtEval.java"
zowe zos-files upload file-to-uss "PtGen.java" "$WORKSPACE_DIR/PtGen.java"
zowe zos-files upload file-to-uss "pt_passwd.c" "$WORKSPACE_DIR/pt_passwd.c"
zowe uss issue ssh "xlc -o pt_passwd pt_passwd.c; extattr +p pt_passwd" --cwd $WORKSPACE_DIR
zowe uss issue ssh "javac -cp /usr/include/java_classes/IRRRacf.jar PtGen.java PtEval.java" --cwd $WORKSPACE_DIR
zowe uss issue ssh "ls -E" --cwd $WORKSPACE_DIR
zowe zos-files download uss-file "$WORKSPACE_DIR/pt_passwd" -b -f "build/pt_passwd"
zowe zos-files download uss-file "$WORKSPACE_DIR/PtGen.class" -b -f "build/PtGen.class"
zowe zos-files download uss-file "$WORKSPACE_DIR/PtEval.class" -b -f "build/PtEval.class"
