#! /usr/bin/python

### This script automates the process of downloading, unpacking,
### and munging up zip files containing Windows binaries.

import getopt
import re
import sys
import os
import urlparse
import urllib
import md5
import errno
import fnmatch
import shutil

download_dir = '/tmp/prepare-downloads'
unpack_dir = '/tmp/prepare-unpack'
test_install_dir = '/tmp/prepare-target'
overwrite_install_dir = os.path.join(os.environ["HOME"], "workspace/trunk/client/windows/dependencies")
install_dir = None # fixme should just be passed around

all_packages = {
    'libiconv' : {
    'url' : 'ftp://ftp.gtk.org/pub/glib/2.12/win32/dependencies/libiconv-1.9.1.bin.woe32.zip',
    'md5' : 'a18e9420d6a354b585a77230996b4171',
    'excludes' : [ 'bin/iconv.exe',
                   'include/*' ],
    'includes' : [ 'README.libiconv' ]
    },
    'gettext' : {
    'url' : 'ftp://ftp.gtk.org/pub/glib/2.12/win32/dependencies/gettext-0.14.5.zip',
    'md5' : 'e609b4fb01fa2c495d294de442f13847'
    },
    'gettext-dev' : {
    'url' : 'ftp://ftp.gtk.org/pub/glib/2.12/win32/dependencies/gettext-dev-0.14.5.zip',
    'md5' : '48b7cb1ba976fcc4dbbeb6cd75a4a02d',
    'excludes' : [ 'bin/*', '*.tcl', '*/gettext-po.h', '*/gettextpo.lib' ]
    },
    'jpeg-lib' : {
    'url' : 'http://easynews.dl.sourceforge.net/sourceforge/gnuwin32/jpeg-6b-4-lib.zip',
    'md5' : 'ff0b69c04ebc6e73e89da1f3a0c74a82',
    'excludes' : [ "*.mft", "*.ver", "jpeg-bcc.lib" ]
    },
    'jpeg-bin' : {
    'url' : 'http://easynews.dl.sourceforge.net/sourceforge/gnuwin32/jpeg-6b-4-bin.zip',
    'md5' : '2c1affae71628525965c6742f6ba4126',
    'excludes' : [ "contrib/*", "bin/*.exe", "bin/exifautotran", "manifest/*", "man/*" ]
    },        
    'libpng-lib' : {
    'url' : 'http://superb-west.dl.sourceforge.net/sourceforge/gnuwin32/libpng-1.2.8-lib.zip',
    'md5' : '17a34613562c1d352e7cb27253e58498',
    'excludes' : [ 'manifest/*', 'include/*' ]
    },
    'libpng-bin' : {
    'url' : 'http://superb-west.dl.sourceforge.net/sourceforge/gnuwin32/libpng-1.2.8-bin.zip',
    'md5' : '96252d384982f9da7061854539331345',
    'excludes' : [ 'contrib/libpng/1.2.8/libpng-1.2.8-GnuWin32.README',
                   'contrib/libpng/1.2.8/depends-GnuWin32.lst',
                   'contrib/*/README',
                   'contrib/*/LICENSE',
                   'bin/*.exe',
                   'bin/*-config',
                   'manifest/*' ]
    },
    'zlib-lib' : {
    'url' : 'http://superb-west.dl.sourceforge.net/sourceforge/gnuwin32/zlib-1.2.3-lib.zip',
    'md5' : 'b0a2df1a2b0dd0bd801d71d06ce42360',
    'excludes' : [ 'manifest/*', 'lib/zlib-bcc.lib', 'include/*' ]
    },
    'zlib-bin' : {
    'url' : 'http://superb-west.dl.sourceforge.net/sourceforge/gnuwin32/zlib-1.2.3-bin.zip',
    'md5' : '0b431b557399c1b3948c13c803a22c95',
    'excludes' : [ 'contrib/*', 'manifest/*', 'lib/zlib-bcc.lib', 'include/*' ]
    },    
    'cairo' : {
    'url' : 'ftp://ftp.gtk.org/pub/gtk/v2.8/win32/cairo-1.2.4.zip',
    'md5' : 'c4f01404ddfbe802462164be7db57204'
    },
    'cairo-dev' : {
    'url' : 'ftp://ftp.gtk.org/pub/gtk/v2.8/win32/cairo-dev-1.2.4.zip',
    'md5' : '6fca217180259fb632406d341543d7be',
    'excludes' : [ 'make/*' ]
    },
    'glib' : {
    'url' : 'ftp://ftp.gtk.org/pub/glib/2.12/win32/glib-2.12.3.zip',
    'md5' : '13712dc1907f132918a47202961f5070',
    'excludes' : [ 'bin/gspawn-win32-helper.exe',
                   'bin/gspawn-win32-helper-console.exe'
                   ]
    },
    'glib-dev' : {
    'url' : 'ftp://ftp.gtk.org/pub/glib/2.12/win32/glib-dev-2.12.3.zip',
    'md5' : 'fb4fe8ff215cadbf07ff438b49e66baf',
    'includes' : [ 'bin/glib-genmarshal.exe' ],
    'excludes' : [ 'bin/glib-mkenums', 'bin/glib-gettextize',
                   'bin/gobject-query.exe',
                   'share/glib-2.0/gettext/*',
                   'make/*', '*/gobjectnotifyqueue.c'
                  ]
    },
    'pango' : {
    'url' : 'ftp://ftp.gtk.org/pub/pango/1.14/win32/pango-1.14.3.zip',
    'md5' : 'f1d87c085e4df22046759a4a2e680dde',
    'excludes' : [ 'bin/pango-querymodules.exe' ],
    'includes' : [ '*/pango.aliases', '*/pango.modules' ]
    },
    'pango-dev' : {
    'url' : 'ftp://ftp.gtk.org/pub/pango/1.14/win32/pango-dev-1.14.3.zip',
    'md5' : '3a307df7ae2a65534d181b23a8fd0a62',
    'excludes' : [ 'make/*' ]
    }
}


MAP_UNKNOWN = 1
MAP_INCLUDE = 2
MAP_EXCLUDE = 3

class FileMapper:    
    def __init__(self, pkgname, forced_includes, forced_excludes):
        if pkgname.endswith('-dev') or \
           pkgname.endswith('-lib') or \
           pkgname.endswith('-bin'):
            self.pkgname = pkgname[:-4]
        else:
            self.pkgname = pkgname

        self.forced_includes = forced_includes
        self.forced_excludes = forced_excludes

    def try_include(self, src, is_dir, destdir):
        if src.endswith(".h"):
            return os.path.join(destdir, self.pkgname, src)
        elif src.endswith(".lib"):
            return os.path.join(destdir, self.pkgname, src)
        elif src.endswith(".dll"):
            ## dlls go in the top level
            return os.path.join(destdir, os.path.basename(src))
        elif os.path.basename(src) in ['LICENSE', 'COPYING', 'README' ]:
            return os.path.join(destdir, self.pkgname, "doc", os.path.basename(src)) 
        else:
            return None

    def try_exclude(self, src, is_dir):
        if is_dir:
            return 1 # dirs are only included if they should be created despite being empty
        elif src.endswith(".mo"):
            return 1
        elif src.endswith(".1"):
            return 1
        elif src.endswith(".3"):
            return 1        
        elif src.endswith(".html"):
            return 1
        elif src.endswith(".el"):
            return 1
        elif src.endswith(".txt"):
            return 1
        elif src.endswith(".dll.a"):
            return 1
        elif src.endswith(".a"):
            return 1                
        elif src.endswith(".pc"):
            return 1
        elif src.endswith(".def"):
            return 1
        elif src.endswith(".m4"):
            return 1        
        elif src.find('/gtk-doc/') >= 0:
            return 1
        elif os.path.basename(src) in [ 'CHANGES', 'ChangeLog', 'TODO', 'INSTALL', 'ANNOUNCE' ]:
            return 1
        else:
            return 0

    def matches_pattern_list(self, src, list):
        for pattern in list:
            if fnmatch.fnmatch(src, pattern):
                return True
        return False
    
    def map(self, src, is_dir, destdir):

        forced_include = self.matches_pattern_list(src, self.forced_includes)
        forced_exclude = self.matches_pattern_list(src, self.forced_excludes)

        if forced_include and forced_exclude:
            print >>sys.stderr, "file %s in both include and exclude list" % src
        elif forced_include:
            return (MAP_INCLUDE, os.path.join(destdir, self.pkgname, src))
        elif forced_exclude:
            return (MAP_EXCLUDE, None)
        
        dest = self.try_include(src, is_dir, destdir)
        if dest != None:
            return (MAP_INCLUDE, dest)
        else:
            if self.try_exclude(src, is_dir):
                return (MAP_EXCLUDE, None)
            else:
                return (MAP_UNKNOWN, None)

class Package:
    def __init__(self, name, props):
        
        self.name = name
        self.url = props['url']
        self.md5 = props['md5']
        self.relative = urlparse.urlparse(self.url)[2]
        if not self.relative:
            print >>sys.stderr, "could not parse relative path from %s" % self.url
            sys.exit(1)
        self.filename = os.path.basename(self.relative)
        if not self.filename:
            print >>sys.stderr, "could not parse filename from %s" % self.relative
            sys.exit(1)
        self.download = os.path.join(download_dir, self.filename)
        if not self.download:
            print >>sys.stderr, "could not build download dest from dir %s file %s" % (download_dir, self.filename)
            sys.exit(1)        
        self.download_md5 = None # md5 on our downloaded file

        self.unpack_dir = os.path.join(unpack_dir, self.name)

        force_includes = []
        if props.has_key('includes'):
            force_includes = props['includes']

        force_excludes = []
        if props.has_key('excludes'):
            force_excludes = props['excludes']
        self.mapper = FileMapper(self.name, force_includes, force_excludes)

    def map_file(self, src, is_dir):
        return self.mapper.map(src, is_dir, install_dir)

    def get_filename(self):
        return self.filename

    def get_download(self):
        return self.download

    def get_download_md5(self):
        return self.download_md5

    def get_unpack_dir(self):
        return self.unpack_dir

    def check_download(self):
        hash = None
        try:
            hash = md5.new(open(self.get_download()).read()).hexdigest()
        except IOError, e:
            if e.errno == errno.EEXIST:
                pass

        if hash == None:
            return 0
        else:
            self.download_md5 = hash
            if self.md5 == None or self.md5 == hash:
                return 1
            else:
                return 0


# returns true if it already existed
def ensure_dir(dirname):
    if not dirname:
        raise "no directory name"
    try:
        os.makedirs(dirname)
        return False
    except OSError, e:
        if e.errno == errno.EEXIST:
            return True
        else:
            raise e

def do_init(packages):
    ensure_dir(download_dir)
    ensure_dir(unpack_dir)
    ensure_dir(test_install_dir)

# total_file_size is -1 if server did not provide one
def download_progress_hook(blocks_so_far, block_size, total_file_size):
    if total_file_size < 0:
        sys.stdout.write(".")
    else:
        sys.stdout.write("\r %d / %d" % (blocks_so_far * block_size, total_file_size))

def do_download(packages):
    try:
        os.chdir(download_dir)
    except:
        print >>sys.stderr, "Failed to change to directory %s, do you need to init?" % download_dir

    for p in packages:
        if p.check_download():
            print "Already have %s with md5sum %s" % (p.get_download(), p.get_download_md5())
            continue

        if p.get_download_md5() != None:
            print "Existing file %s has wrong md5 %s" % (p.get_download(), p.get_download_md5())

        print "Downloading %s to %s" % (p.url, p.get_download())
        try:
            urllib.urlretrieve(p.url, p.get_download(), download_progress_hook)
        except Exception, e:
            print >>sys.stderr, "Failed to download %s: %s" % (p.get_filename(), e)
        sys.stdout.write('\n')

        if p.check_download():
            print "Successfully downloaded %s with md5sum %s" % (p.get_download(), p.get_download_md5())
        else:
            print >>sys.stderr, "Failed to download %s (expected md5 %s downloaded file hashes to %s)" % (p.get_filename(), p.md5, p.get_download_md5())

    print "Everything downloaded"

def rmrf(top):
    if top == None or top == '/':
        raise "you are trying to hurt yourself"
    try:
        os.stat(top)
    except:
        return
    print "  (blowing away old directory %s)" % top
    for root, dirs, files in os.walk(top, topdown=False):
        for name in files:
            os.remove(os.path.join(root, name))
        for name in dirs:
            os.rmdir(os.path.join(root, name))
        
def do_unpack(packages):
    for p in packages:
        rmrf(p.get_unpack_dir())
        ensure_dir(p.get_unpack_dir())
        os.chdir(p.get_unpack_dir())
        os.system("unzip %s 1>/dev/null" % p.get_download())
        print "Unpacked %s to %s" % (p.get_download(), p.get_unpack_dir())
    print "Everything unpacked"


def get_install_info(p, absolute_src_dir, relative_src_dir, name, is_dir):
    absolute_src = os.path.join(absolute_src_dir, name)
    relative_src = os.path.join(relative_src_dir, name)
    (result, dest) = p.map_file(relative_src, is_dir)

    if result == MAP_INCLUDE:
        #print "mapped %s to %s" % (relative_src, dest)
        return (MAP_INCLUDE, absolute_src, dest)
    elif result == MAP_EXCLUDE:
        return (MAP_EXCLUDE, None, None)
    elif result == MAP_UNKNOWN:
        print >>sys.stderr, "Don't know what to do with '%s' in '%s' (add to excludes or handle in file mapper)" % (relative_src, p.name)
        return (MAP_UNKNOWN, None, None)
    else:
        raise "broken map result"
    
INSTALL=1
OVERWRITE=2
def do_install_or_overwrite(mode, packages, dir):
    global install_dir
    install_dir = dir
    ensure_dir(install_dir)
    os.chdir(install_dir)

    any_unknown_mappings = 0
    file_mappings = {}
    dir_mappings = {}

    for p in packages:
        for root, dirs, files in os.walk(p.get_unpack_dir(), topdown=False):
            relative_root = root[len(p.get_unpack_dir())+1:]
            for name in files:
                info = get_install_info(p, root, relative_root, name, False)
                if info[0] == MAP_INCLUDE:
                    file_mappings[info[1]] = info[2]
                elif info[0] == MAP_UNKNOWN:
                    any_unknown_mappings = 1
            for name in dirs:
                info = get_install_info(p, root, relative_root, name, True)
                if info[0] == MAP_INCLUDE:
                    dir_mappings[info[1]] = info[2]
                elif info[0] == MAP_UNKNOWN:
                    any_unknown_mappings = 1

    if any_unknown_mappings:
        print >>sys.stderr, "could not install due to files we don't know what to do with"
        sys.exit(1)

    any_dest_problems = 0
    all_dests = {}
    for (src, dest) in file_mappings.items():
        if dest[0] != '/':
            print >>sys.stderr, "destination path %s is not absolute (src %s)" % (dest, src)
            any_dest_problems = 1
            
        if all_dests.has_key(dest):
            # first person to have this problem can fix it to print the other file in the collision ;-)
            print >>sys.stderr, "collision, two files %s and %s map to %s" % (all_dests[dest], src, dest)
            any_dest_problems = 1
        all_dests[dest] = src

    for (src, dest) in dir_mappings.items():
        if dest[0] != '/':
            print >>sys.stderr, "destination dir %s is not absolute (src %s)" % (dest, src)
            any_dest_problems = 1
        
        if all_dests.has_key(dest):
            # first person to have this problem can fix it to print the other file in the collision ;-)
            print >>sys.stderr, "collision, file %s and dir %s both map to %s" % (all_dests[dest], src, dest)
            any_dest_problems = 1
        # don't put directories in all_dests because two directories that conflict
        # are just fine (all directories are the same iow)

    if any_dest_problems:
        print >>sys.stderr, "could not install due to above problems"
        sys.exit(1)

    if len(dir_mappings) > 0:
        print "%d empty directories to be created" % len(dir_mappings)

    print "Looks like we can install OK"

    for d in dir_mappings.values():
        if not ensure_dir(d):
            print "  created possibly-empty directory %s since a glob matched it" % d

    installed_count = 0
    for (src, dest) in file_mappings.items():
        dir = os.path.dirname(dest)
        if not ensure_dir(dir):
            print "  created directory %s" % dir
        shutil.copy2(src, dest)
        installed_count = installed_count + 1

    if mode == INSTALL:
        print "Installed %d files to %s" % (installed_count, install_dir)
        print "You can use './dirdiff.py client/windows/dependencies %s' to see what may have changed" % install_dir
        print "Then run 'prepare.py overwrite'"
    else:
        print "Overwrote %d files in %s" % (installed_count, install_dir)

def do_install(packages):
    do_install_or_overwrite(INSTALL, packages, test_install_dir)
    
def do_all(packages):
    do_init(packages)
    do_download(packages)
    do_unpack(packages)
    do_install(packages)
    # deliberately does not include overwrite

def do_overwrite(packages):
    do_install_or_overwrite(OVERWRITE, packages, overwrite_install_dir)

def usage():
    print >>sys.stderr,  "Usage: prepare.py ACTION PACKAGE"

def main():
    try:
        options, remaining = getopt.getopt(sys.argv[1:], '')
    except getopt.GetoptError:
        usage()
        sys.exit(1)

    init_params={}
    conf = None
    for opt, val in options:
        pass
    
    if len(remaining) < 2:
        usage()
        sys.exit(1)

    known_actions = { 'init' : do_init,
                      'download' : do_download,
                      'unpack' : do_unpack,
                      'install' : do_install,
                      'all' : do_all,
                      'overwrite' : do_overwrite }

    action = remaining[0]

    if not known_actions.has_key(action):
        print >>sys.stderr, "Known actions are: %s (not '%s')" % (' '.join(known_actions.keys()), action)
        sys.exit(1)

    packages = {}
    for pkg in remaining[1:]:
        # if you specify both a package and "all" then it creates
        # the package twice, but no big deal
        if pkg == 'all':
            for a in all_packages.keys():
                packages[a] = Package(a, all_packages[a])
        elif not all_packages.has_key(pkg):
            print >>sys.stderr, "don't know package %s" % pkg
            sys.exit(1)
        else:
            packages[pkg] = Package(pkg, all_packages[pkg])

    print "will do %s for packages %s" % (action, packages.keys())

    action_func = known_actions[action]
    action_func(packages.values())

main()
