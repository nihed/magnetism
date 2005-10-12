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

    bool getSignIn();
    void setSignIn(bool signIn);

private:
    void load();
    void save();

    void loadString(HKEY         key,
	            const WCHAR *valueName,
		    BSTR        *str);
    void loadBool(HKEY         key,
	          const WCHAR *valueName,
	          bool        *result);
    void saveString(HKEY         key,
	            const WCHAR *valueName, 
	            BSTR         str);
    void saveBool(HKEY         key,
	          const WCHAR *valueName, 
		  bool         value);

    HippoBSTR messageServer_;
    HippoBSTR webServer_;
    bool signIn_;
};
