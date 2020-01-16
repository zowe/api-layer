# PassTicket Test Programs

Files in this directory can be used to test PassTicket setup on different systems.

Source code can be built using `build.sh`. You need to have zosmf and ssh profiles setup in Zowe CLI and change the workspace directory in `build.sh`.

The `pt_passwd` executable needs to be program-controlled. The user ID used to run the build needs to have `BPX.FILEATTR.APF CLASS(FACILITY) ACCESS(READ)`.

You need to setup security to generate PassTickets using:

- `racf.jcl`

These jobs are using following user IDs:

- `SDKBLD1` - to generate and evaluate PassTickets
- `SDKTST1` - to use the PassTicket in C (`__passwd` and `__passw_applid`)

Then you can run it under user `SDKBLD1` that can both generate and evaluate PassTickets:

```sh
java -Xquickstart -cp /usr/include/java_classes/IRRRacf.jar:. PtGen SDKTST1 TSTAPPL
```

```sh
java -Xquickstart -cp /usr/include/java_classes/IRRRacf.jar:. PtEval SDKTST1 TSTAPPL $(cat .passticket)
```

You can use the PassTicket under user ID `SDKTST1` which has access to `TSTAPPL` APPLID:

```sh
pt_passwd SDKTST1 TSTAPPL $(cat .passticket)
```

Findings:

- Without reply protection, you can generate PassTickets as much as you need, it will return the same PassTicket, if you run it at the similar time. Both of them are valid and can be used.
- To generate PassTicket via Java class `IRRPassTicket`, the user ID that runs it needs to have `IRRPTAUTH.TSTAPPL.* CLASS(PTKTDATA) ACCESS(UPDATE)`
- To evaluation via Java class `IRRPassTicket`, the user ID that runs it needs to have `IRRPTAUTH.TSTAPPL.* CLASS(PTKTDATA) ACCESS(READ)`
- `IRRPassTicket` does not check that the user ID for which the ticket is issued can use it
- `IRRPassTicket` requires that PassTicket is set up properly using `RDEFINE PTKTDATA ...`
