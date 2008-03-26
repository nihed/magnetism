/* HippoPreferences.h: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <engine/hippo-engine-basics.h>

class HippoPreferences
{
public:
    HippoPreferences(HippoInstanceType instanceType);

    void getMessageServer(BSTR *server) throw (std::bad_alloc);
    void setMessageServer(BSTR server);

    void getWebServer(BSTR *server) throw (std::bad_alloc);
    void setWebServer(BSTR server);

    // Used from the crash-dump code
    static void getWebServer(HippoInstanceType instanceType, BSTR *server);

    bool getSignIn();
    void setSignIn(bool signIn);

    const WCHAR *getInstanceSubkey();
    const CLSID *getInstanceClassId();

    static const CLSID *getInstanceClassId(HippoInstanceType instanceType);

private:
    void load();
    void save();

    static const WCHAR *getInstanceSubkey(HippoInstanceType instanceType);

    // Whether to use a separate debug registry section
    HippoInstanceType instanceType_;

    HippoBSTR messageServer_;
    HippoBSTR webServer_;
    bool signIn_;
};
