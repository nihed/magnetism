# Copyright 2005, Red Hat, Inc.

import os
import re
import socket
import stat
import tempfile
import time

class Deployer:
    """A class that handles writing out an archive file containing super
    itself and all the necessary configuration files and sources for a
    set of services"""
    
    def __init__(self, config):
        """Create a new Deployer object

        config -- the Config object for the current invocation of super
        """
        self.svndir = config.expand_parameter("svndir")
        self.superdir = config.expand_parameter("superdir")
        self.darfile = config.expand_parameter("darfile")
        
        self.outdir = tempfile.mkdtemp("", "super.")

        self.directories = {}
        self.files = {}
        self.topfiles = {}

        self._add_file("super", self.superdir, "super")
        self._add_file("super", self.superdir, "base.conf")

        # Hacky - add all files that end in .exclude in subdirs of super. We
        # really should record the exclude files we need when creating the
        # directory tree
        for e in os.listdir(self.superdir):
            e_path = os.path.join(self.superdir, e)
            e_stat = os.stat(e_path)
            if stat.S_ISDIR(e_stat.st_mode):
                for f in os.listdir(e_path):
                    if f.endswith(".exclude") and not f.startswith("."):
                        self._add_file("super", self.superdir, os.path.join(e, f))
        
        for f in os.listdir(os.path.join(self.superdir, "lib", "super")):
            if f.endswith(".py") and not f.startswith("."):
                self._add_file("super", self.superdir, os.path.join("lib", "super", f))
        
        self._write_info()
        
    def add_dirtree(self, dirtree):
        """Add a Dirtree object into the output of the deployer"""
        for source in dirtree.list_sources():
            if (source.startswith(self.svndir)):
                self._add_source(None, self.svndir, source[len(self.svndir) + 1:])
            elif (source.startswith(self.superdir)):
                self._add_source("super", self.superdir, source[len(self.superdir) + 1:])

    def write(self):
        """Finish writing out the deployement archive"""
        topfiles = self.topfiles.keys()
        topfiles.sort()
        filestr = " ".join(topfiles)
        os.spawnl(os.P_WAIT, "/bin/sh", "sh", "-c", "(cd %s && tar cfz - %s) > %s" % (self.outdir, filestr, self.darfile))
        os.spawnl(os.P_WAIT, "/bin/rm", "rm", "-rf", self.outdir)

    def _add_directory(self, directory):
        """Make the directory (and parents) in the temporary output dir if we haven't already"""
        parent = os.path.dirname(directory)
        if parent != '':
            self._add_directory(parent)
        else:
            self.topfiles[directory] = 1

        if not self.directories.has_key(directory):
            os.spawnl(os.P_WAIT, "/bin/mkdir", "mkdir", os.path.join(self.outdir, directory))
            self.directories[directory] = 1
        
    def _add_file(self, targetdir, sourcedir, filename):
        """Copy the file into the temporary output directory"""
        if targetdir == None:
            target = filename
        else:
            target = os.path.join(targetdir, filename)
            
        parent = os.path.dirname(target)
        if parent != '':
            self._add_directory(parent)
        else:
            self.topfiles[target] = 1
            
        if not self.files.has_key(target):
            os.spawnl(os.P_WAIT, "/bin/cp", "cp", "-a", os.path.join(sourcedir, filename), os.path.join(self.outdir, target))
            self.files[target] = 1

    def _add_source(self, targetdir, sourcedir, filename):
        """Add a source file or directory to the temporary output directory"""
        src_path = os.path.join(sourcedir, filename)
        src_stat = os.stat(src_path)
        src_is_dir = stat.S_ISDIR(src_stat.st_mode)

        if src_is_dir:
            if targetdir == None:
                target = filename
            else:
                target = os.path.join(targetdir, filename)
                
            self._add_directory(target)
        else:
            self._add_file(targetdir, sourcedir, filename)

    def _write_info(self):
        """Write a file META-INFO/dumbhippo.info containing information about the deployment archive"""
        os.spawnl(os.P_WAIT, "/bin/mkdir", "mkdir", os.path.join(self.outdir, "META-INF"))
        self.topfiles[os.path.join("META-INF", "dumbhippo.info")] = 1
        outfile = open(os.path.join(self.outdir, "META-INF", "dumbhippo.info"), "w")

        print >>outfile, "Date: %s" % time.strftime("%a, %d %b %Y %H:%M:%S %z")
        
        hostname = socket.gethostname()
        if (hostname != 'localhost.localdomain'):
            print >>outfile, "BuildHost: %s" % hostname

        f = os.popen("svn info %s" % self.superdir)
        l = f.readlines()
        f.close()

        if (len(l) > 0):
            m = re.match('Revision:\s*([0-9]+)\s*', l[-1])
            if m:
                print >>outfile, "SvnVersion: %s" % m.group(1)

        outfile.close()
