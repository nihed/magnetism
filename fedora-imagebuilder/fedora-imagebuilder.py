#!/usr/bin/python -tt
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Library General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
# Written by Seth Vidal
# Sections taken from Mach by Thomas Vander Stichele

import os, sys
import shutil, tempfile
import getopt

def usage(ecode):
    print """fedora-imagebuilder.py
Usage:
    fedora-imagebuilder.py --target=FCNUM --name=HOSTNAME [--size=MB] [--addbaseurl=ADDITIONAL_URL]

Example:
    
    fedora-imagebuilder.py --target=4 --name=testserv --addbaseurl=http://192.168.1.11/yum/fedora/4
"""
    sys.exit(ecode)

try:
    opts, args = getopt.getopt(sys.argv[1:], "hs:n:t:u:", ["help", "name=", "size=", "target=", "baseurl=", "addbaseurl="])
except getopt.GetoptError:
    # print help information and exit:
    usage(2)

imagesize = 2048 # megabytes
yum_repos = []
hostname = None
fc_target = 4
fc_baseurl = None
fc_add_baseurls = []

for o, a in opts:
    if o in ("-h", "--help"):
        usage(0)
    elif o in ("-s", "--size"):
        imagesize = int(a)
    elif o in ("-n", "--name"):
        hostname = a
    elif o in ("--yum-repo",):
        yum_repos.append(os.path.expanduser(a))
    elif o in ("-t", "--target"):
        fc_target = int(a)
    elif o in ("-u", "--baseurl"):
        fc_baseurl = a
    elif o in ("--addbaseurl",):
        fc_add_baseurls.append(a)

if hostname is None:
    usage(2)

workdir = tempfile.mkdtemp(prefix="fedora-image")
print "Using working directory %s" % (workdir,)
os.chdir(workdir)
imagefilename = os.path.join(workdir, 'image')
print "Creating file: %s" % (imagefilename,)
os.spawnlp(os.P_WAIT, 'dd', 'dd,', 'if=/dev/zero', 'of=%s' % (imagefilename,), 'bs=1M', 'count=1', 'seek=%d' % (imagesize,))
print "Creating filesystem on %s" % (imagefilename,)
os.spawnl(os.P_WAIT, '/sbin/mke2fs', 'mke2fs', '-F', '-j', imagefilename)
os.spawnl(os.P_WAIT, '/sbin/tune2fs', 'tune2fs', '-i', '0', imagefilename)
print "Mounting"
mntpath = os.path.join(workdir, 'mnt')
os.mkdir(mntpath)

os.spawnl(os.P_WAIT, '/bin/mount', 'mount', '-o', 'loop', imagefilename, mntpath)

print "Bootstrapping basedirs and devices"
for dir in ['dev', 'etc/rpm', 'tmp', 'var/tmp', 'etc/yum.repos.d', 'var/lib/rpm', 'var/log', 'proc', 'sys']:
    os.makedirs(os.path.join(mntpath, dir), 0770)
# we need stuff
devices = [('null', 'c', '1', '3', '666'),
            ('urandom', 'c', '1', '9', '644'), 
            ('random', 'c', '1', '9', '644'),
            ('full', 'c', '1', '7', '666'),
            ('ptmx', 'c', '5', '2', '666'),
            ('tty', 'c', '5', '0', '666'),
            ('zero', 'c', '1', '5', '666')]

for (dev, devtype, major, minor, perm) in devices:
    devpath = os.path.join(mntpath, 'dev', dev)
    cmd = '%s %s -m %s %s %s %s' % ('/sbin/mknod',  devpath, perm, devtype, major, minor)
    
print "Generating fstab"
fstab = """
/dev/sda1               /                       ext3    defaults 1 1
none                    /dev/pts                devpts  gid=5,mode=620 0 0
none                    /dev/shm                tmpfs   defaults 0 0
none                    /proc                   proc    defaults 0 0
none                    /sys                    sysfs   defaults 0 0
"""

f = open(os.path.join(mntpath, 'etc', 'fstab'), 'w')
f.write(fstab)
f.close()

# link fd to ../proc/self/fd
os.symlink('../proc/self/fd', os.path.join(mntpath, 'dev/fd'))

for file in ['mtab', 'var/log/yum.log']:
    f = open(os.path.join(mntpath, file), 'w')
    f.close()

print "Mounting proc"
os.spawnl(os.P_WAIT, '/bin/mount', 'mount', '-t', 'proc', 'none', os.path.join(mntpath, 'proc'))

yum_conf = """
[main]
cachedir=/var/cache/yum
debuglevel=2
logfile=/var/log/yum.log
exclude=*-debuginfo
gpgcheck=0
obsoletes=1
reposdir=/dev/null
"""

default_yum_repos = """
[base]
name=Fedora Core $FC_TARGET - $basearch - Base
mirrorlist=http://fedora.redhat.com/download/mirrors/fedora-core-$FC_TARGET
enabled=1

[updates-released]
name=Fedora Core $FC_TARGET - $basearch - Released Updates
mirrorlist=http://fedora.redhat.com/download/mirrors/updates-released-fc$FC_TARGET
enabled=1
""".replace('$FC_TARGET', str(fc_target))

custom_yum_repos = """
[base]
name=Custom Yum Repository
baseurl=$BASEURL
enabled=1
"""

yum_conf_path = os.path.join(workdir, 'yum.conf')
f = open(yum_conf_path, 'w')
f.write(yum_conf)
if fc_baseurl is None:
    f.write(default_yum_repos)
else:
    f.write(custom_yum_repos.replace('$BASEURL', fc_baseurl))
i = 0
for addurl in fc_add_baseurls:
    f.write("""[customrepo$REPONUM]
name=Custom Yum URL $REPONUM
baseurl=$BASEURL
enabled=1
""".replace('$REPONUM', str(i)).replace('$BASEURL', addurl))
    i = i + 1
f.close()

for f in yum_repos:
    shutil.copy(f, os.path.join(mntpath, 'etc/yum.repos.d'))

print "Invoking yum"
newenv = os.environ.copy()
newenv['LD_PRELOAD'] = 'libselinux-mock.so'
os.spawnle(os.P_WAIT, '/usr/bin/yum', 'yum', '-c', yum_conf_path, '--installroot=%s' % (mntpath,), '-y', 'groupinstall', 'Base', newenv)

print "Post-installation"

selinux="""
# This file controls the state of SELinux on the system.
# SELINUX= can take one of these three values:
#       enforcing - SELinux security policy is enforced.
#       permissive - SELinux prints warnings instead of enforcing.
#       disabled - SELinux is fully disabled.
SELINUX=disabled
# SELINUXTYPE= type of policy in use. Possible values are:
#       targeted - Only targeted network daemons are protected.
#       strict - Full SELinux protection.
SELINUXTYPE=targeted
"""

cfg = open(os.path.join(mntpath, 'etc/selinux/config'), 'w')
cfg.write(selinux)
cfg.close()

os.spawnl(os.P_WAIT, '/bin/umount', 'umount', os.path.join(mntpath, 'proc'))
os.spawnl(os.P_WAIT, '/bin/umount', 'umount', mntpath)

print "Done!"
