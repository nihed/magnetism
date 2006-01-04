/* HippoPreferences.h: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include <HippoUtil.h>

class HippoPreferences
{
public:
    HippoPreferences(bool debug);

    HRESULT getMessageServer(BSTR *server);
    void setMessageServer(BSTR server);
    void parseMessageServer(BSTR         *host,
                            unsigned int *port);

    HRESULT getWebServer(BSTR *server);
    void setWebServer(BSTR server);
    void parseWebServer(BSTR         *host,
                        unsigned int *port);

    bool getSignIn();
    void setSignIn(bool signIn);

private:
    void load();
    void save();
    void parseServer(BSTR          server,
                     const WCHAR  *defaultHost,
                     unsigned int  defaultPort,
                     BSTR         *host,
                     unsigned int *port);

    // Whether to use a separate debug registry section
    bool debug_;

    HippoBSTR messageServer_;
    HippoBSTR webServer_;
    bool signIn_;
};
