{{#each user.jobcard}}
{{{this}}}
{{/each}}
//*Contact Info: Api Mediation Layer team - Rest In Api
//*   Alternate:
//* Description: Api Mediation testing deployment instance
//*      Co Req: N/A
//*     Est CPU: UNKNOWN
//* Est Elapsed: 1 DAYS
//*    Start Up:
//*        Stop: you can
//*      Cancel: you can
//*Special Info:
//*LAST UPDATED: 2020-03-24
//*      Region:
//*
//********************************************************************
//RUNBATCH   PROC SRVRPATH='{{{user.zosTargetDir}}}'
//*-------------------------------------------------------------------
//EXPORT EXPORT SYMLIST=*
//RUNSTEP EXEC PGM=BPXBATSL,REGION=0M,TIME=NOLIMIT,
//  PARM='PGM /bin/sh &SRVRPATH/run-wrapper.sh'
//STDOUT   DD SYSOUT=*
//STDERR   DD SYSOUT=*
//STDENV   DD *
dir={{{user.zosTargetDir}}}
JAVA_HOME={{{user.javaHome}}}
basePort={{{user.basePort}}}
systemHostname={{{user.systemHostname}}}
LIBPATH=/lib:/usr/lib:/usr/lpp/Printsrv/lib
/*
// PEND
//********************************************************************
//JAVA EXEC PROC=RUNBATCH
