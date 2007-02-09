#!/usr/bin/env python
# -*- Mode: Python; py-indent-offset: 4 -*-

### This script autogenerates some of the files in the project
### We then check in the files to CVS, for convenience when 
### python is annoying to obtain (e.g. on Windows)

from string import *
import sys
import os
import re
import exceptions
import string

def die (s):
    print "Error: %s" % (s)
    sys.exit(1)

def warn (s):
    print "Warning: %s" % (s)

def as_int (v):
    try:
        return atoi (v)
    except:
        return v

class OutputTarget:
    def __init__ (self):
        self.header_target = None
        self.impl_target = None
        self.current_target = None

    def set (self, header_target, impl_target):
        self.header_target = header_target
        self.impl_target = impl_target
        self.current_target = self.header_target

    def set_target_implementation (self):
        self.current_target = self.impl_target

    def is_targetting_implementation (self):
        return self.current_target == self.impl_target

    def write (self, s):
        self.current_target.write (s)

    def write_to_header (self, s):
        self.header_target.write (s)

    def write_to_impl (self, s):
        self.impl_target.write (s)

    def header_handle (self):
        return self.header_target

    def impl_handle (self):
        return self.impl_target

    def close (self):
        if self.header_target:
            self.header_target.close ()
        if self.impl_target:
            self.impl_target.close ()
        if self.current_target:
            self.current_target.close ()
    
def argNtype (count):
    return "Arg%dType" % count

def arg_list (preceding_arg, count_int):
    retval = ""
    count = "%d" % count_int
    i = 1
    while i <= count_int:
        if preceding_arg:
            comma = ", "
        else:
            if i == 1:
                comma = ""
            else:
                comma = ", "
        retval = retval + comma + ("arg%d" % i)
        i = i + 1;

    return retval

def param_list (preceding_arg, count_int):
    retval = ""
    count = "%d" % count_int
    i = 1
    while i <= count_int:
        if preceding_arg:
            comma = ", "
        else:
            if i == 1:
                comma = ""
            else:
                comma = ", "
        retval = retval + comma + argNtype (i) + (" arg%d" % i)
        i = i + 1;

    return retval


def type_list (preceding_arg, count_int):
    retval = ""
    count = "%d" % count_int
    i = 1
    while i <= count_int:
        if preceding_arg:
            comma = ", "
        else:
            if i == 1:
                comma = ""
            else:
                comma = ", "
        retval = retval + comma + argNtype (i)
        i = i + 1;

    return retval

def template_list (preceding_arg, count_int):
    retval = ""
    count = "%d" % count_int
    i = 1
    while i <= count_int:
        if preceding_arg:
            comma = ", "
        else:
            if i == 1:
                comma = ""
            else:
                comma = ", "
        retval = retval + comma + "class " + argNtype (i)
        i = i + 1;

    return retval

def write_abstract_slot (outfile, indent, count, count_int):
    
    ### Write the SlotN base class
    
    outfile.write_to_header (("\n" + indent + "// Abstract base class for slots with %d argument(s)\n") % count_int)

    outfile.write_to_header (indent + "template <class ReturnType")
    outfile.write_to_header (template_list (1, count_int))

    outfile.write_to_header (">\n");

    outfile.write_to_header (indent + "class Slot" + count + " : public Slot\n")

    outfile.write_to_header (indent + "{\n" + indent + "protected:\n")

    outfile.write_to_header (indent + "  Slot" + count + " ()\n")

    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "  }\n")

    outfile.write_to_header (indent + "public:\n")

    outfile.write_to_header (indent + "  virtual ReturnType invoke (")

    outfile.write_to_header (param_list(0, count_int))

    outfile.write_to_header (") const\n")

    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "    // we're effectively const since we ref/unref in a pair here\n")
    outfile.write_to_header (indent + "    const_cast<Slot" + count + "*>(this)->ref ();\n")           
    outfile.write_to_header (indent + "    return invoke_impl (" + arg_list (0, count_int) + \
                             ");\n")
    outfile.write_to_header (indent + "    const_cast<Slot" + count + "*>(this)->unref ();\n")
    outfile.write_to_header (indent + "  }\n")

    ## operator ()

    outfile.write_to_header (indent + "  ReturnType operator() (")

    outfile.write_to_header (param_list(0, count_int))

    outfile.write_to_header (") const\n")

    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "    return invoke (" + arg_list (0, count_int) + \
                             ");\n")
    outfile.write_to_header (indent + "  }\n")

    outfile.write_to_header (indent + "protected:\n")

    outfile.write_to_header (indent + "  virtual ReturnType invoke_impl (")

    outfile.write_to_header (param_list(0, count_int))

    outfile.write_to_header (") const = 0;\n\n")

    outfile.write_to_header (indent + "};\n");

def write_method_slot (outfile, indent, count, count_int):

    ### Write the MethodSlotN concrete class
    
    outfile.write_to_header (("\n" + indent + "// Concrete class for slots created from methods with %d argument(s)\n") % count_int)

    outfile.write_to_header (indent + "template <class MethodClassType, class ReturnType")
    outfile.write_to_header (template_list (1, count_int))

    outfile.write_to_header (">\n");

    outfile.write_to_header (indent + "class MethodSlot" + count + " : public Slot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))

    outfile.write_to_header (">\n");

    outfile.write_to_header (indent + "{\n" + indent + "private:\n")

    outfile.write_to_header (indent +
                             "  typedef ReturnType (MethodClassType::* MethodType) (")
    outfile.write_to_header (type_list(0, count_int))

    outfile.write_to_header (");\n")

    outfile.write_to_header (indent + "public:\n")
    outfile.write_to_header (indent + "  MethodSlot" + count + " (MethodClassType *object,\n")
    outfile.write_to_header (indent + "               MethodType method)\n")
    outfile.write_to_header (indent + "    : obj_(object), method_(method)\n")
    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "  }\n")

    outfile.write_to_header (indent + "  virtual ReturnType invoke_impl (")

    outfile.write_to_header (param_list(0, count_int))

    outfile.write_to_header (") const\n")
    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "    return (obj_->*method_) (")
    outfile.write_to_header (arg_list(0, count_int))
    outfile.write_to_header (");\n");
    outfile.write_to_header (indent + "  }\n")

    # destructor
    outfile.write_to_header (indent + "protected:\n")
    outfile.write_to_header (indent + "  virtual ~MethodSlot" + count + " ()\n")
    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "  }\n")

    # members
    outfile.write_to_header (indent + "private:\n")
    outfile.write_to_header (indent + "  MethodClassType *obj_;\n")
    outfile.write_to_header (indent + "  MethodType method_;\n")

    # close MethodSlot class
    outfile.write_to_header (indent + "}; // class MethodSlot" + count + "\n");
    
def write_function_slot (outfile, indent, count, count_int):    

    ### Write the MethodSlotN concrete class
    
    outfile.write_to_header (("\n" + indent + "// Concrete class for slots created from static functions with %d argument(s)\n") % count_int)

    outfile.write_to_header (indent + "template <class ReturnType")
    outfile.write_to_header (template_list (1, count_int))

    outfile.write_to_header (">\n");

    outfile.write_to_header (indent + "class FunctionSlot" + count + " : public Slot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))

    outfile.write_to_header (">\n");

    outfile.write_to_header (indent + "{\n" + indent + "private:\n")

    outfile.write_to_header (indent +
                             "  typedef ReturnType (* FunctionType) (")
    outfile.write_to_header (type_list(0, count_int))

    outfile.write_to_header (");\n")

    outfile.write_to_header (indent + "public:\n")
    outfile.write_to_header (indent + "  FunctionSlot" + count + " (FunctionType function)\n")
    outfile.write_to_header (indent + "    : function_(function)\n")
    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "  }\n")

    outfile.write_to_header (indent + "  virtual ReturnType invoke_impl (")

    outfile.write_to_header (param_list(0, count_int))

    outfile.write_to_header (") const\n")
    outfile.write_to_header (indent + "  {\n")
    
    outfile.write_to_header (indent + "    return (* function_) (")
    outfile.write_to_header (arg_list(0, count_int))
    outfile.write_to_header (");\n");
    outfile.write_to_header (indent + "  }\n")

    # destructor
    outfile.write_to_header (indent + "protected:\n")
    outfile.write_to_header (indent + "  virtual ~FunctionSlot" + count + " ()\n")
    outfile.write_to_header (indent + "  {\n")
    outfile.write_to_header (indent + "  }\n")

    # member variable
    outfile.write_to_header (indent + "private:\n")
    outfile.write_to_header (indent + "  FunctionType function_;\n")
    
    # close FunctionSlot class
    outfile.write_to_header (indent + "}; // class FunctionSlot" + count + "\n");

def process_make_slot (outfile, indent, count):
    count_int = as_int (count)

    write_abstract_slot (outfile, indent, count, count_int)
    write_method_slot (outfile, indent, count, count_int)
    write_function_slot (outfile, indent, count, count_int)

    ## slot() convenience function for MethodSlot
    outfile.write_to_header ("\n" + indent + "// convenience function that creates a MethodSlot\n");
    outfile.write_to_header (indent + "template <class MethodClassType, class ReturnType")
    outfile.write_to_header (template_list(1, count_int))
    outfile.write_to_header (">\n")
    outfile.write_to_header (indent + "inline Slot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))
    outfile.write_to_header ("> *\n")
    outfile.write_to_header (indent + "slot (MethodClassType *obj, ReturnType (MethodClassType::* method) (")
    outfile.write_to_header (type_list(0, count_int))
    outfile.write_to_header ("))\n");
    outfile.write_to_header (indent + "{\n")
    outfile.write_to_header (indent + "  return new MethodSlot" + count + "<MethodClassType, ReturnType")
    outfile.write_to_header (type_list(1, count_int))
    outfile.write_to_header ("> (obj, method);\n")
    outfile.write_to_header (indent + "}\n")

    ## slot() convenience function for FunctionSlot
    outfile.write_to_header ("\n" + indent + "// convenience function that creates a FunctionSlot\n");
    outfile.write_to_header (indent + "template <class ReturnType")
    outfile.write_to_header (template_list(1, count_int))
    outfile.write_to_header (">\n")
    outfile.write_to_header (indent + "inline Slot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))
    outfile.write_to_header ("> *\n")
    outfile.write_to_header (indent + "slot (ReturnType (* function) (")
    outfile.write_to_header (type_list(0, count_int))
    outfile.write_to_header ("))\n");
    outfile.write_to_header (indent + "{\n")
    outfile.write_to_header (indent + "  return new FunctionSlot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))
    outfile.write_to_header ("> (function);\n")
    outfile.write_to_header (indent + "}\n")


def process_make_bind (outfile, indent, count):
    count_int = as_int (count)
    plusone = ("%d" % (count_int+1))
    last_argtype = "Arg" + plusone + "Type"
    last_argname = "arg" + plusone
    boundslotname = "BoundSlot" + count + "_" + plusone
    targetslottype = "Slot" + count + "<ReturnType" + type_list(1, count_int) + ">"
    sourceslottype = "Slot" + plusone + \
                     "<ReturnType" + type_list(1, count_int+1) + ">"

    ## The class itself

    outfile.write_to_header("\n")
    outfile.write_to_header(indent + "// slot with " + count + " argument(s) created from a slot with " + plusone + " argument(s)\n")
    outfile.write_to_header(indent + "template <class ReturnType")
    outfile.write_to_header(template_list(1, count_int + 1))
    outfile.write_to_header(">\n")
    outfile.write_to_header(indent + "class " + boundslotname + ": public " + targetslottype + "\n")

    outfile.write_to_header(indent + "{\n")
    

    outfile.write_to_header(indent + "public:\n")
    # constructor
    outfile.write_to_header(indent + "  " + boundslotname + " (" + sourceslottype + \
                            " * slot, " + last_argtype + " " + last_argname + \
                            ")\n")
    outfile.write_to_header(indent + "    : original_slot_(slot), " + \
                            last_argname + "_(" + last_argname + ")\n")
    outfile.write_to_header(indent + "  {\n")
    outfile.write_to_header(indent + "    original_slot_->ref ();\n")
    outfile.write_to_header(indent + "    original_slot_->sink ();\n")
    outfile.write_to_header(indent + "  }\n\n")
    
    # invoke
    outfile.write_to_header(indent + "  virtual ReturnType invoke_impl (")
    outfile.write_to_header(param_list(0, count_int))
    outfile.write_to_header(") const\n")

    outfile.write_to_header(indent + "  {\n")
    comma = None
    if count_int > 0:
        comma = ", "
    else:
        comma = ""
    outfile.write_to_header(indent + "    return original_slot_->invoke (" + \
                            arg_list(0, count_int) + \
                            comma + \
                            last_argname + "_" + ");\n")
    outfile.write_to_header(indent + "  }\n")


    outfile.write_to_header(indent + "protected:\n")
    # destructor
    outfile.write_to_header(indent + "  virtual ~" + boundslotname + " ()\n")
    outfile.write_to_header(indent + "  {\n")
    outfile.write_to_header(indent + "    original_slot_->unref ();\n")
    outfile.write_to_header(indent + "  }\n")

    # members
    outfile.write_to_header(indent + "private:\n")
    outfile.write_to_header(indent + "  " + sourceslottype + " * original_slot_;\n")
    outfile.write_to_header(indent + "  " + last_argtype + " " + last_argname + "_;\n")

    # close class
    outfile.write_to_header(indent + "}; // class " + boundslotname + "\n")
    
    ## bind() convenience function
    outfile.write_to_header ("\n" + indent + "// convenience function that creates a BoundSlot\n");
    outfile.write_to_header (indent + "template <class ReturnType")
    outfile.write_to_header (template_list(1, count_int + 1))
    outfile.write_to_header (">\n")
    outfile.write_to_header (indent + "inline Slot" + count + "<ReturnType")
    outfile.write_to_header (type_list(1, count_int))
    outfile.write_to_header ("> *\n")
    outfile.write_to_header (indent + "bind (" + sourceslottype + \
                             " * s, " + last_argtype + " " + last_argname + \
                             ")\n");
    outfile.write_to_header (indent + "{\n")
    outfile.write_to_header (indent + "  return new " + boundslotname + \
                             "<ReturnType")
    outfile.write_to_header (type_list(1, count_int+1))
    outfile.write_to_header ("> (s, " + last_argname + ");\n")
    outfile.write_to_header (indent + "}\n")


ot = OutputTarget ()
ot.set ( open ("HippoSignals.h", 'w'),
         open ("HippoSignals.cpp", 'w') )

ot.write_to_header("// Generated file from codegen.py ; do not edit\n")
ot.write_to_impl("// Generated file from codegen.py ; do not edit\n")
ot.write_to_impl("#include \"stdafx.h\"\n")

ot.write_to_header("#pragma once\n\n");

process_make_slot(ot, "", "0")
process_make_slot(ot, "", "1")
process_make_slot(ot, "", "2")
process_make_slot(ot, "", "3")
process_make_slot(ot, "", "4")

## You must make one less bind() than you make slots, since the bind 
## converts a slot to one with fewer args, and the src slot must exist
process_make_bind(ot, "", "0")
process_make_bind(ot, "", "1")
process_make_bind(ot, "", "2")
process_make_bind(ot, "", "3")


