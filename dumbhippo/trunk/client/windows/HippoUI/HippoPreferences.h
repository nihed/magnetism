/* HippoPreferences.h: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include <HippoUtil.h>

class HippoPreferences
{
public:
    HippoPreferences();

    HRESULT getMessageServer(BSTR *server);
    void setMessageServer(BSTR server);
    void parseMessageServer(char        **nameUTF8,
	                    unsigned int *port);

    HRESULT getWebServer(BSTR *server);
    void setWebServer(BSTR server);

private:
    void load();
    void save();

    void loadString(HKEY         key,
	            const WCHAR *valueName,
		    BSTR        *str);
    void saveString(HKEY         key,
	            const WCHAR *valueName, 
	            BSTR         str);

    HippoBSTR messageServer_;
    HippoBSTR webServer_;
};
