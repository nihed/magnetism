/* HippoPreferences.h: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include <HippoUtil.h>

enum HippoInstanceType {
    HIPPO_INSTANCE_NORMAL,
    HIPPO_INSTANCE_DOGFOOD,
    HIPPO_INSTANCE_DEBUG
};

class HippoPreferences
{
public:
    HippoPreferences(HippoInstanceType instanceType);

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

    const WCHAR *getInstanceSubkey();
    const CLSID *getInstanceClassId();
    const WORD getInstanceIcon();
    const WORD getInstanceDisconnectedIcon();
    const WORD getInstanceMissedIcon();

    static const CLSID *getInstanceClassId(HippoInstanceType instanceType);

private:
    void load();
    void save();
    void parseServer(BSTR          server,
                     const WCHAR  *defaultHost,
                     unsigned int  defaultPort,
                     BSTR         *host,
                     unsigned int *port);

    // Whether to use a separate debug registry section
    HippoInstanceType instanceType_;

    HippoBSTR messageServer_;
    HippoBSTR webServer_;
    bool signIn_;
};
