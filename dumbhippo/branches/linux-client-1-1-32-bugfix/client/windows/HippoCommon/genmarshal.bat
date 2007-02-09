@echo off
PATH=%PATH%;..\dependencies
set genmarshal=..\dependencies\glib\bin\glib-genmarshal
set source=..\..\common\hippo\hippo-common-marshal.list

%genmarshal%  --prefix=hippo_common_marshal --header %source% > hippo-common-marshal.h

echo #include "hippo-common-marshal.h" > hippo-common-marshal.c
%genmarshal%  --prefix=hippo_common_marshal --body %source% >> hippo-common-marshal.c

set source=..\..\common\canvas\hippo\hippo-canvas-marshal.list

%genmarshal%  --prefix=hippo_canvas_marshal --header %source% > hippo-canvas-marshal.h

echo #include "hippo-canvas-marshal.h" > hippo-canvas-marshal.c
%genmarshal%  --prefix=hippo_canvas_marshal --body %source% >> hippo-canvas-marshal.c

@echo on

