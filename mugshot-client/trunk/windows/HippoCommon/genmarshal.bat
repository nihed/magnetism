@echo off
PATH=%PATH%;..\dependencies
set genmarshal=%1\glib-genmarshal

set source=..\..\..\desktop-data-model\engine\hippo-engine-marshal.list

%genmarshal%  --prefix=hippo_engine_marshal --header %source% > hippo-engine-marshal.h

echo #include "hippo-engine-marshal.h" > hippo-engine-marshal.c
%genmarshal%  --prefix=hippo_engine_marshal --body %source% >> hippo-engine-marshal.c

set source=..\..\common\stacker\hippo-stacker-marshal.list

%genmarshal%  --prefix=hippo_stacker_marshal --header %source% > hippo-stacker-marshal.h

echo #include "hippo-stacker-marshal.h" > hippo-stacker-marshal.c
%genmarshal%  --prefix=hippo_stacker_marshal --body %source% >> hippo-stacker-marshal.c

set source=..\..\canvas\common\hippo\hippo-canvas-marshal.list

%genmarshal%  --prefix=hippo_canvas_marshal --header %source% > hippo-canvas-marshal.h

echo #include "hippo-canvas-marshal.h" > hippo-canvas-marshal.c
%genmarshal%  --prefix=hippo_canvas_marshal --body %source% >> hippo-canvas-marshal.c

@echo on

