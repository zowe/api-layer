//IDF{{esm}} JOB ({{account}}),CLASS=A,MSGCLASS=X
/*JOBPARM SYSAFF={{lpar}}
//*-------------------------------------------------------------------
//*
//* Zowe CLI ID Federation Plugin
//*
//* This JCL can be used for mapping between distributed identity
//* and mainframe identity
//*
//* 1) Validate job header and make all necessary changes according
//*    to your system environment.
//*
//* 2) Run the JCL on LPAR {{lpar}}
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
