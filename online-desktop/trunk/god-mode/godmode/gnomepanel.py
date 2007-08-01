# Library for programatically controlling GNOME panels:
# Add/remove toplevels, add applets.
# Author: Colin Walters <walters@redhat.com>

import os,sys

import gobject,gconf

def _unix_basename(path):
    if path.endswith('/'):
        path = path[:-1]
    return os.path.basename(path)

PANEL_CONFIG_DIR = '/apps/panel'
PANEL_SCHEMAS_DIR = '/schemas/apps/panel'
PANEL_DEFAULTS_DIR = '/apps/panel/default_setup'

def _associate_schemas_in_dir(profile_dir, schema_dir, client, engine):
    for entry in client.all_entries(schema_dir):
        bn = _unix_basename(entry.get_key())
        key = profile_dir + '/' + bn
        engine.associate_schema(key, entry.get_key())

    for subdir in client.all_dirs(schema_dir):
        bn = _unix_basename(subdir)
        prefs_subdir = profile_dir + '/' + bn
        schema_subdir = schema_dir + '/' + bn

        _associate_schemas_in_dir(prefs_subdir, schema_subdir, client, engine)

def _save_id_list(list_name, vals):
    client = gconf.client_get_default()
    key = PANEL_CONFIG_DIR + '/general/' + list_name 
    if vals is None:
        vals = client.get_list(key, gconf.VALUE_STRING) 
    else:
        vals = list(set(vals))
    client.set_list(key, gconf.VALUE_STRING, vals)

def _save_other_lists(list_name):
    for other_name in ('toplevel_id_list', 'applet_id_list', 'object_id_list'):
        if list_name != other_name:
            _save_id_list(other_name, None)

def _add_to_id_list(list_name, item):
    client = gconf.client_get_default()
    vals = client.get_list(PANEL_CONFIG_DIR + '/general/' + list_name, gconf.VALUE_STRING) 
    vals.append(item)
    _save_id_list(list_name, vals)
    _save_other_lists(list_name)

def add_applet(panel, name, iid, pos, right=False):
    client = gconf.client_get_default()
    engine = gconf.engine_get_default()

    dir = PANEL_CONFIG_DIR + '/applets/' + name

    _associate_schemas_in_dir(dir, PANEL_SCHEMAS_DIR + '/objects', client, engine) 
    client.set_string(dir + '/object_type', 'bonobo-applet')
    client.set_string(dir + '/toplevel_id', panel)
    client.set_int(dir + '/position', pos)
    client.set_bool(dir + '/panel_right_stick', not not right)

    client.set_string(dir + '/bonobo_iid', 'OAFIID:' + iid)
    
    _add_to_id_list('applet_id_list', name)

def create_toplevel(name, screen=None, orient='bottom'):
    client = gconf.client_get_default()
    engine = gconf.engine_get_default()
    dir = PANEL_CONFIG_DIR + '/toplevels/' + name
    _associate_schemas_in_dir(dir, PANEL_SCHEMAS_DIR + '/toplevels', client, engine) 

    client.set_int(dir+'/screen', screen and screen.get_number() or 0)
    client.set_string(dir+'/orientation', orient)

def set_toplevels(names):
    client = gconf.client_get_default()
    _save_id_list('toplevel_id_list', names)
    _save_other_lists('toplevel_id_list')

def get_toplevels():
    return gconf.client_get_default().get_list(PANEL_CONFIG_DIR + '/general/toplevel_id_list', gconf.VALUE_STRING)