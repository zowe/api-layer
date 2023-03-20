//IDF{{esm}} JOB ({{account}}),CLASS=A,MSGCLASS=X{{sysaff}}
//*-------------------------------------------------------------------
//*
//* Zowe CLI ID Federation Plugin
//*
//* This JCL can be used for mapping from a distributed identity to
//* a mainframe identity
//*
//* 1) Validate the job header and make all necessary changes
//*    according to your system environment.
//*
//* 2) Run the JCL on {{system}}.
//*
//* Note(s):
//*
//* 1. THE USER ID THAT RUNS THIS JOB MUST HAVE SUFFICIENT AUTHORITY
//*    TO ALTER SECURITY DEFINITIONS
//*
//*-------------------------------------------------------------------
//RUN      EXEC PGM=IKJEFT01,REGION=0M
//SYSTSPRT DD SYSOUT=*
//SYSTSIN     DD DATA,SYMBOLS=JCLONLY
{{commands}}
/*
