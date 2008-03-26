@echo off
PATH=%PATH%;..\dependencies
set genmarshal=%1\glib-genmarshal
set source=..\..\..\desktop-data-model\ddm\ddm-marshal.list

%genmarshal%  --prefix=ddm_marshal --header %source% > ddm-marshal.h

echo #include "ddm-marshal.h" > ddm-marshal.c
%genmarshal%  --prefix=ddm_marshal --body %source% >> ddm-marshal.c

@echo on
