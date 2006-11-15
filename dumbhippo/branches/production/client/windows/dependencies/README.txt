This directory consists of binary builds for windows of some of
the open-source dependencies of DumboHippo. The reasons for
checking in the binary builds are:

 - To use standard builds, rather than creating a custom
   build process which might be buggy. 
   
   These official builds (by Tor Lillqvist) are known to work 
   across a variety of platforms.
   
 - To reduce the checkout size and compile time ... while
   the libraries are big, the source are bigger.
   
 - To simplify everybody's life

Most libraries included here are licensed under the GNU LGPL.
See the file COPYING.txt for details. libpng is under a different
license though. Packages with a COPYING or LICENSE file will have 
it copied to their "doc" dir by prepare.py

If you change anything in this directory, do it by modifying and
re-running the prepare.py script. Don't manually unpack zip files.

The basic way prepare.py works is:

  ./prepare.py all all

This should download everything (if not already downloaded), unpack
it, apply needed exclusions/inclusions, and then create a directory
tree simulating what should go in subversion.

Then to copy into your subversion directory, run:
   
  ./prepare.py overwrite all

The second "all" can be replaced with just the package you want to
update, like:

  ./prepare.py all libpng 
  ./prepare.py overwrite libpng
 
You can also run only some phases (download, unpack, etc.) - just read
the prepare.py source.


