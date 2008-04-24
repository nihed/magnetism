PATH=c:\Program Files\gecko-sdk\lib;c:\Program Files\gecko-sdk\bin;%PATH%

set geckoidl=c:\Program Files\gecko-sdk\idl
set includes=-I "%geckoidl%" -I ..\..\common\firefox\public

xpidl %includes% -m header -e %2 %1
xpidl %includes% -m typelib -e %3 %1
