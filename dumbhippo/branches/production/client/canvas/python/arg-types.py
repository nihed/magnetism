from argtypes import ArgType, matcher
import reversewrapper

class CairoParam(reversewrapper.Parameter):
    def get_c_type(self):
        return self.props.get('c_type').replace('const-', 'const ')
    def convert_c2py(self):
        self.wrapper.add_declaration("PyObject *py_%s;" % self.name)
        self.wrapper.write_code(
            code=('py_%s = PycairoContext_FromContext(cairo_reference(%s), NULL, NULL);' %
                  (self.name, self.name)),
            cleanup=("Py_DECREF(py_%s);" % self.name))
        self.wrapper.add_pyargv_item("py_%s" % self.name)

matcher.register_reverse("cairo_t*", CairoParam)

class CairoSurfaceArg(ArgType):

    before = ('    %(name)s = &((PycairoSurface*)(py_%(name)s))->surface;\n')

    def write_param(self, ptype, pname, pdflt, pnull, info):
        info.varlist.add('PyObject', '*py_' + pname)
        info.varlist.add('cairo_surface_t', '*'+pname)
        info.add_parselist('O', ['&py_'+pname], [pname])
        info.arglist.append(pname)
        info.codebefore.append (self.before % { 'name' : pname, 'namecopy' : 'NULL' })


    def write_return(self, ptype, ownsreturn, info):
        info.varlist.add('cairo_surface_t', '*ret')
        info.codeafter.append('    if (ret)\n'
                              '        return PycairoSurface_FromSurface(ret, NULL);\n'
                              '    else {\n'
                              '        Py_INCREF(Py_None);\n'
                              '        return Py_None;\n'
                              '    }');

matcher.register('cairo_surface_t*', CairoSurfaceArg())

