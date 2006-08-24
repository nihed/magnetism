@echo off
PATH=%PATH%;..\dependencies
set genmarshal=..\dependencies\glib\bin\glib-genmarshal --prefix=hippo_common_marshal
set source=..\..\common\hippo\hippo-common-marshal.list

%genmarshal% --header %source% > hippo-common-marshal.h

echo #include "hippo-common-marshal.h" > hippo-common-marshal.c
%genmarshal% --body %source% >> hippo-common-marshal.c

@echo on

