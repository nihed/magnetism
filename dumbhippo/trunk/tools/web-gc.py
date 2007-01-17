#! /usr/bin/python

import sys
import os
import re

tags_by_name = {}
javafiles_by_classname = {}
all_file_tags = []
all_java_tags = []
all_jsp_files = []
all_java_files = []

referenced_java_classes = {}

class Tag:
    def __init__(self, name):
        self._name = name
        self._referenced = 0

    def name(self):
        return self._name

    def referenced(self):
        return self._referenced

    def mark_referenced(self):
        self._referenced = 1

tagdir_re = re.compile('tagdir="\/WEB-INF\/(tags[0-9/]*)" +prefix="([0-9a-z]+)"')
tagname_re = re.compile('<dh([a-z0-9]*):([a-z0-9A-Z_]+)')
javaclass_re = re.compile('(com\.dumbhippo\.[.a-zA-Z0-9_]+[a-zA-Z0-9_])')
extends_page_re = re.compile('[a-zA-Z0-9_]\s+extends\s+([a-zA-Z0-9_]+[a-zA-Z0-9_])')

def scan_tag_references(lines):
    prefix_fixup_map = { 'dh' : 'dh' }

    for l in lines:
        m = tagdir_re.search(l)
        if m:
            tagdir = m.group(1)
            localprefix = m.group(2)
            if tagdir == 'tags':
                prefix_fixup_map[localprefix] = 'dht'
            elif tagdir == 'tags/2':
                prefix_fixup_map[localprefix] = 'dht2'
            elif tagdir == 'tags/3':
                prefix_fixup_map[localprefix] = 'dht3'
            else:
                raise "unknown tagdir %s" % tagdir

    referenced_tag_names = []

    for l in lines:
        tagnames = tagname_re.findall(l)
        for p, n in tagnames:
            #print "%s %s" % (p, n)
            localprefix = "dh" + p
            if not prefix_fixup_map.has_key(localprefix):
                raise "tag file %s uses prefix %s without a taglib import of it" % (fullpath, localprefix)
            realprefix = prefix_fixup_map[localprefix]

            referenced_tag_names.append(realprefix + ":" + n)

    return referenced_tag_names

# inner classes and some other things confuse us
ignoreClasses = [
    "com.dumbhippo.mbean.DynamicPollingSystem",
    "com.dumbhippo.identity20.Guid.ParseException",
    "com.dumbhippo.server.Configuration.PropertyNotFoundException",
    "com.dumbhippo.live.LiveState.verboseLogging",
    "com.dumbhippo.live.LiveState.getInstance",
    "com.dumbhippo.services.FlickrPhotosetData.PhotoData",
    'com.dumbhippo.web.CookieAuthentication.NotLoggedInException',
    'com.dumbhippo.web.LoginCookie.BadTastingException',
    'com.dumbhippo.web.pages.LiveObject',
    'com.dumbhippo.web.JavascriptResolver.Page',
    'com.dumbhippo.server.Stacker.GroupQueryType'
   ]

def scan_class_references(lines):
    referenced_java_classes = []
    for l in lines:
        if l.startswith("package "):
            continue
        matches = javaclass_re.findall(l)
        for klass in matches:
            # remove confusing things
            if klass in ignoreClasses:
                continue
            
            # filter out package names
            i = klass.rfind(".")
            nopackage = klass[i+1:]
            if nopackage.islower():
                continue
            # assume anything else is a fully-qualified referenced java class
            referenced_java_classes.append(klass)
    return referenced_java_classes

class TagFile(Tag):
    def __init__(self, fullpath):

        self._fullpath = fullpath
        
        name = os.path.basename(fullpath).replace(".tag", "")
        prefix = None
        if "tags/2" in fullpath:
            prefix="dht2"
        elif "tags/3" in fullpath:
            prefix="dht3"
        else:
            prefix="dht"
        Tag.__init__(self, prefix + ":" + name)

        lines = open(fullpath).readlines()

        self._referenced_tag_names = scan_tag_references(lines)

        self._referenced_java_classes = scan_class_references(lines)

    def referenced_tag_names(self):
        return self._referenced_tag_names

    def referenced_java_classes(self):
        return self._referenced_java_classes

    def mark_referenced(self):
        if self.referenced():
            return
        Tag.mark_referenced(self)
        for tagname in self.referenced_tag_names():
            t = tags_by_name[tagname]
            t.mark_referenced()
        for classname in self.referenced_java_classes():
            referenced_java_classes[classname] = 1

    def fullpath(self):
        return self._fullpath

    def __repr__(self):
        return "{%s}" % (self.name())

    def __str__(self):
        return self.__repr__()

class JavaTag(Tag):
    def __init__(self, name, javaclass):
        Tag.__init__(self, "dh:" + name)
        self._javaclass = javaclass

    def javaclass(self):
        return self._javaclass

    def mark_referenced(self):
        if self.referenced():
            return
        Tag.mark_referenced(self)
        referenced_java_classes[self.javaclass()] = 1

    def fullpath(self):
        return javafiles_by_classname[self.javaclass()].fullpath()

    def __repr__(self):
        return "{%s %s}" % (self.name(), self.javaclass())

    def __str__(self):
        return self.__repr__()

class JspFile:
    def __init__(self, fullpath):
        self._filename = os.path.basename(fullpath)

        lines = open(fullpath).readlines()

        self._referenced_tag_names = scan_tag_references(lines)

        self._referenced_java_classes = scan_class_references(lines)

    def referenced_tag_names(self):
        return self._referenced_tag_names

    def referenced_java_classes(self):
        return self._referenced_java_classes        

    def filename(self):
        return self._filename

    def __repr__(self):
        return "{%s}" % (self.filename())

    def __str__(self):
        return self.__repr__()

unknown_classes = []

class JavaFile:
    def __init__(self, fullpath):
        self._filename = os.path.basename(fullpath)
        self._fullpath = fullpath

        self._referenced = 0

        s = fullpath.replace('/', '.')
        i = s.find('.src.')
        s = s[i+5:]
        self._javaclass = s.replace(".java", "")

        lines = open(fullpath).readlines()

        # this is not very robust e.g. won't get references to stuff
        # in the same package
        self._referenced_java_classes = scan_class_references(lines)

        # a little hack to get the extra java refs we most need
        # for now
        if self._javaclass.startswith("com.dumbhippo.web.pages."):
            for l in lines:
                m = extends_page_re.search(l)
                if m:
                    pageSuperclass = m.group(1)
                    #print "%s extends %s" % (self._javaclass, pageSuperclass)
                    if not pageSuperclass.endswith("Page"):
                        raise "page %s extends %s not ending in Page?" % (self._javaclass, pageSuperclass)
                    self._referenced_java_classes.append("com.dumbhippo.web.pages." + pageSuperclass)

    def referenced_java_classes(self):
        return self._referenced_java_classes        

    def javaclass(self):
        return self._javaclass

    def filename(self):
        return self._filename

    def fullpath(self):
        return self._fullpath

    def referenced(self):
        return self._referenced

    def mark_referenced(self):
        if self._referenced:
            return
        self._referenced = 1
        for classname in self.referenced_java_classes():
            if not javafiles_by_classname.has_key(classname):
                unknown_classes.append(classname)
            else:
                javafiles_by_classname[classname].mark_referenced()

    def __repr__(self):
        return "{%s}" % (self.filename())

    def __str__(self):
        return self.__repr__()    

name_re = re.compile('<name>([^<]+)<\/name>')
tagclass_re = re.compile('<tagclass>([^<]+)<\/tagclass>')

def load_tld(fullpath):
    lines = open(fullpath).readlines()
    current_name = None
    current_tagclass = None
    inside_tag = 0
    for l in lines:
        if not inside_tag and '<tag>' in l:
            inside_tag = 1
        elif inside_tag and current_name == None:
            m = name_re.search(l)
            if (m):
                current_name = m.group(1)
                #print current_name
        elif inside_tag and current_tagclass == None:
            m = tagclass_re.search(l)
            if (m):
                current_tagclass = m.group(1)
                #print current_tagclass
        elif inside_tag and '</tag>' in l:
            if not current_name or not current_tagclass:
                raise "did not find a name and tagclass for this tag"
            t = JavaTag(current_name, current_tagclass)
            all_java_tags.append(t)
            current_name = None
            current_tagclass = None
            inside_tag = 0

def load_servlet_info(fullpath):
    pass
    # fixme we could check that jsps are listed in RewriteServlet and
    # gc unused servlet files

def main():

    if len(sys.argv) != 2:
        raise "have to provide server directory"

    serverdir = sys.argv[1]
    
    tagfiles = []
    jspfiles = []
    tldfiles = []
    javafiles = []

    javasrcdir = os.path.join(serverdir, "src")
    webdir = os.path.join(serverdir, "web")

    for root, dirs, files in os.walk(webdir, topdown=False):
        for name in files:
            fullpath = os.path.join(root, name)
            if fullpath.endswith(".tag"):
                tagfiles.append(fullpath)
            elif fullpath.endswith(".jsp"):
                jspfiles.append(fullpath)
            elif fullpath.endswith(".tld"):
                tldfiles.append(fullpath)
            elif fullpath.endswith("/servlet-info.xml"):
                load_servlet_info(fullpath)

    for root, dirs, files in os.walk(javasrcdir, topdown=False):
        for name in files:
            fullpath = os.path.join(root, name)
            if fullpath.endswith(".java"):
                javafiles.append(fullpath)

    for tagfilepath in tagfiles:
       t = TagFile(tagfilepath)
       all_file_tags.append(t)

    for jspfilepath in jspfiles:
        j = JspFile(jspfilepath)
        all_jsp_files.append(j)

    for tldfilepath in tldfiles:
        load_tld(tldfilepath)

    for javafilepath in javafiles:
        j = JavaFile(javafilepath)
        all_java_files.append(j)

    for j in all_java_files:
        if javafiles_by_classname.has_key(j.javaclass()):
            raise "somehow got two java files with class %s" % j.javaclass()
        javafiles_by_classname[j.javaclass()] = j

    for t in all_java_tags:
        if tags_by_name.has_key(t.name()):
            print "duplicate tag name %s %s" % (tags_by_name[t.name()], t)
        tags_by_name[t.name()] = t

    for t in all_file_tags:
        if tags_by_name.has_key(t.name()):
            print "duplicate tag name %s %s" % (tags_by_name[t.name()], t)
        tags_by_name[t.name()] = t

    for jsp in all_jsp_files:
        #print "%s references %d tags %d classes" % (jsp, len(jsp.referenced_tag_names()), len(jsp.referenced_java_classes()))
        for tagname in jsp.referenced_tag_names():
            if not tags_by_name.has_key(tagname):
                raise "%s references tag %s which we did not find" % (jsp, tagname)
            t = tags_by_name[tagname]
            t.mark_referenced()
        for classname in jsp.referenced_java_classes():
            referenced_java_classes[classname] = 1

    ## before this point, to reference a java file you set referenced_java_classes[class]=1,
    ## and then after this point you have to mark_referenced() on the JavaFile object

    for classname in referenced_java_classes.keys():
        if not javafiles_by_classname.has_key(classname):
            raise "java class %s is referenced but not known" % classname
        javafiles_by_classname[classname].mark_referenced()

    unused_files = []

    for t in all_java_tags:
        if not t.referenced():
            print "%s unused (java class %s)" % (t.name(), t.javaclass())
            unused_files.append(t.fullpath())

    for t in all_file_tags:
        if not t.referenced():
            print "%s unused (file %s)" % (t.name(), t.fullpath())
            unused_files.append(t.fullpath())

    for j in all_java_files:
        if j.javaclass().startswith("com.dumbhippo.web.pages."):
            if not j.referenced():
                print "page bean %s looks unused" % j.javaclass()
                unused_files.append(j.fullpath())

    if len(unused_files) == 0:
        print "no unused files"
        return

    scriptname = 'remove-unused.sh'
    fd = os.open(scriptname, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0755)
    if fd < 0:
        raise "failed to open %s" % scriptname
    f = os.fdopen(fd, 'w')

    f.write("#!/bin/sh\n")
    f.write("set -e\n")
    f.write("function die() { echo $* 1>&2; exit 1; }\n")
    for u in unused_files:
        f.write('/bin/rm %s || die "failed to remove %s"\n' % (u, u))

    f.write("\n")
    f.write("svn rm ")
    for u in unused_files:
        f.write("%s " % u)
    f.write("\n")
    f.write("\n")

    print "Wrote script %s which will remove the above" % scriptname
    print "Remember to edit .tld also if removing java tags"
    print "Note that Java classes may be used from Java, which this script won't find"

    if len(unknown_classes) > 0:
        print "Some classes were not understood and the script thinks something uses them"
        print unknown_classes

main()

