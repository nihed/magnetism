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

All libraries included here are licensed under the GNU LGPL.
See the file COPYING.txt for details. (And change this text
if you add a library under a different license.)

If you change anything in this directory, update the Manifest
file to match. The Manifest file should be an exact description
of how to get what is derived here from publically available
tarballs. Don't break that.

