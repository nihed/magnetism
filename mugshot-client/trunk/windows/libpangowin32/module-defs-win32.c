/* Hand-generated for the Mugshot PangoWin32 build */

#include "module-defs.h"

extern void         _pango_basic_win32_script_engine_list (PangoEngineInfo **engines, int *n_engines);
extern void         _pango_basic_win32_script_engine_init (GTypeModule *module);
extern void         _pango_basic_win32_script_engine_exit (void);
extern PangoEngine *_pango_basic_win32_script_engine_create (const char *id);

PangoIncludedModule _pango_included_win32_modules[] = {
 { _pango_basic_win32_script_engine_list, 
   _pango_basic_win32_script_engine_init, 
   _pango_basic_win32_script_engine_exit, 
   _pango_basic_win32_script_engine_create },
 { NULL, NULL, NULL, NULL },
};
