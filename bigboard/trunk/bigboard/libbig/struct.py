import re

import gobject

def get_attr_or_none(dict, attr):
    if dict.has_key(attr):
        return dict[attr]
    else:
        return None
    
_stud_re = re.compile(r'[A-Z]\w')
def studly_to_underscore(str):
    match = _stud_re.search(str)
    result = ''
    while match:
        prev = str[:match.start()]
        result += prev
        if prev != '':
            result += '_'
        result += match.group().lower()
        str = str[match.end():]
        match = _stud_re.search(str)
    result += str
    return result
        
class AutoStruct(object):
    """Kind of like a dictionary, except the values are accessed using
    normal method calls, i.e. get_VALUE(), and the keys are determined
    by arguments passed to the constructor (and are immutable thereafter).
    
    Dictionary keys should be alphanumeric.  Transformation rules are
    applied to make the key more friendly to the get_VALUE syntax.  
    First, hyphens (-) are transformed to underscore (_).  Second,
    studlyCaps style names are replaced by their underscored versions;
    e.g. fooBarBaz is transformed to foo_bar_baz.    
    """
    def __init__(self, values):    
        self._struct_values = {}
        self._struct_values.update(self._transform_values(values))
        
    def __getattribute__(self, name):
        try:
            return object.__getattribute__(self, name)
        except AttributeError:
            if name[0:4] == 'get_':
                attr = name[4:] # skip over get_
                return lambda: get_attr_or_none(self._struct_values, attr)
            raise AttributeError,name
    
    def _get_keys(self):
        return self._struct_values.keys()
    
    def _get_value(self, name):
        return self._struct_values[name]
    
    def _transform_values(self, values):
        temp_args = {}
        for k,v in values.items():
            if type(k) == unicode:
                k = str(k)
            k = k.replace('-','_')
            k = studly_to_underscore(k)
            temp_args[k] = v    
        return temp_args

    # set the given key-value pairs only if not already set, used
    # in constructor
    def _default_values(self, attrs):
        transformed = self._transform_values(attrs)
        for k in transformed.keys():
            if not self._struct_values.has_key(k):
                self._struct_values[k] = transformed[k]
    
    def update(self, values):
        for k in values.keys():
            if not self._struct_values.has_key(k):
                raise Exception("Unknown key '%s' added to %s" % (k, self))
        self._struct_values.update(self._transform_values(values))
        
    def __str__(self):
        return "autostruct values=%s" % (self._struct_values,)
    
class AutoSignallingStruct(gobject.GObject, AutoStruct):
    """An AutoStruct that also emits a "changed" signal when
    its values change."""
    __gsignals__ = {
        "changed": (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ())        
        }
    
    def __init__(self, values):
        gobject.GObject.__init__(self)
        AutoStruct.__init__(self, values)
            
    def update(self, values):
        changed = False
        values = self._transform_values(values)
        for k,v in values.items():
            # FIXME use deep comparison here
            if self._get_value(k) != v:
                changed = True
        AutoStruct.update(self, values)
        if changed:
            self.emit("changed")    
