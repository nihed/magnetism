#!/bin/sh

# Build zipfiles for Pango on Win32: separate runtime and developer packages

ZIP=/tmp/pango-1.14.8.zip
DEVZIP=/tmp/pango-dev-1.14.8.zip
cd /home/behdad/.local

rm $ZIP
zip $ZIP -@ <<EOF
bin/pango-querymodules.exe
etc/pango/pango.aliases
etc/pango/pango.modules
bin/libpango-1.0-0.dll
bin/libpangoft2-1.0-0.dll
bin/libpangowin32-1.0-0.dll
bin/libpangocairo-1.0-0.dll
EOF

zip $ZIP lib/pango/1.5.0/modules/*.dll

rm $DEVZIP
zip -r $DEVZIP -@ <<EOF
include/pango-1.0
lib/libpango-1.0.dll.a
lib/pango-1.0.lib
lib/pango-1.0.def
lib/libpangoft2-1.0.dll.a
lib/pangoft2-1.0.lib
lib/pangoft2-1.0.def
lib/libpangowin32-1.0.dll.a
lib/pangowin32-1.0.lib
lib/pangowin32-1.0.def
lib/libpangocairo-1.0.dll.a
lib/pangocairo-1.0.lib
lib/pangocairo-1.0.def
lib/pkgconfig/pango.pc
lib/pkgconfig/pangoft2.pc
lib/pkgconfig/pangowin32.pc
lib/pkgconfig/pangocairo.pc
share/gtk-doc/html/pango
EOF
