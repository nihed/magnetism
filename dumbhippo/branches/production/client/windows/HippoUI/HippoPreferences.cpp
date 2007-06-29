/* HippoPreferences.cpp: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx-hippoui.h"
#include "HippoPreferences.h"
#include "Guid.h"
#include "Resource.h"
#include <HippoRegKey.h>
#include <limits.h>
#include <glib.h>

static const WCHAR HIPPO_SUBKEY[] = HIPPO_REGISTRY_KEY L"\\Client";
static const WCHAR HIPPO_SUBKEY_DOGFOOD[] = HIPPO_REGISTRY_KEY L"\\DogfoodClient";
static const WCHAR HIPPO_SUBKEY_DEBUG[] = HIPPO_REGISTRY_KEY L"\\DebugClient";

static const WCHAR DEFAULT_MESSAGE_SERVER[] = HIPPO_DEFAULT_MESSAGE_SERVER_L;
static const WCHAR DEFAULT_WEB_SERVER[] = HIPPO_DEFAULT_WEB_SERVER_L;
static const WCHAR DEFAULT_LOCAL_MESSAGE_SERVER[] = HIPPO_DEFAULT_LOCAL_MESSAGE_SERVER_L;
static const WCHAR DEFAULT_LOCAL_WEB_SERVER[] = HIPPO_DEFAULT_LOCAL_WEB_SERVER_L;

HippoPreferences::HippoPreferences(HippoInstanceType instanceType)
{
    instanceType_ = instanceType;
    signIn_ = true;

    load();
}

void
HippoPreferences::getMessageServer(BSTR *server)
{
    if (messageServer_)
        messageServer_.CopyTo(server);
    else if (instanceType_ == HIPPO_INSTANCE_DEBUG)
        HippoBSTR(DEFAULT_LOCAL_MESSAGE_SERVER).CopyTo(server);
    else
        HippoBSTR(DEFAULT_MESSAGE_SERVER).CopyTo(server);
}

void 
HippoPreferences::setMessageServer(BSTR server)
{
    messageServer_ = server;

    save();
}

void
HippoPreferences::getWebServer(BSTR *server)
{
    if (webServer_)
        webServer_.CopyTo(server);
    else if (instanceType_ == HIPPO_INSTANCE_DEBUG) 
       HippoBSTR(DEFAULT_LOCAL_WEB_SERVER).CopyTo(server);
    else
       HippoBSTR(DEFAULT_WEB_SERVER).CopyTo(server);
}

void 
HippoPreferences::getWebServer(HippoInstanceType instanceType, BSTR *server)
{
    HippoBSTR webServer;
    HippoRegKey key(HKEY_CURRENT_USER, getInstanceSubkey(instanceType), false);

    key.loadString(L"WebServer", &webServer);

    if (webServer)
        webServer.CopyTo(server);
    else if (instanceType == HIPPO_INSTANCE_DEBUG) 
       HippoBSTR(DEFAULT_LOCAL_WEB_SERVER).CopyTo(server);
    else
       HippoBSTR(DEFAULT_WEB_SERVER).CopyTo(server);
}

void 
HippoPreferences::setWebServer(BSTR server)
{
    webServer_ = server;

    save();
}

bool 
HippoPreferences::getSignIn()
{
    return signIn_;
}

void
HippoPreferences::setSignIn(bool signIn)
{
    signIn = signIn != false;
    if (signIn != signIn_) {
        signIn_ = signIn;
        save();
    }
}

void
HippoPreferences::load()
{
    HippoRegKey key(HKEY_CURRENT_USER, getInstanceSubkey(), false);

    messageServer_ = NULL;
    key.loadString(L"MessageServer", &messageServer_);

    webServer_ = NULL;
    key.loadString(L"WebServer", &webServer_);

    signIn_ = true;
    key.loadBool(L"SignIn", &signIn_);
}

void
HippoPreferences::save(void)
{
    HippoRegKey key(HKEY_CURRENT_USER, getInstanceSubkey(), true);

    key.saveString(L"MessageServer", messageServer_);
    key.saveString(L"WebServer", webServer_);
    key.saveBool(L"SignIn", signIn_);
}


const WCHAR *
HippoPreferences::getInstanceSubkey(HippoInstanceType instanceType)
{
    switch (instanceType) {
        case HIPPO_INSTANCE_NORMAL:
        default:
            return HIPPO_SUBKEY;
        case HIPPO_INSTANCE_DOGFOOD:
            return HIPPO_SUBKEY_DOGFOOD;
        case HIPPO_INSTANCE_DEBUG:
            return HIPPO_SUBKEY_DEBUG;
    }
}


const WCHAR *
HippoPreferences::getInstanceSubkey()
{
    return getInstanceSubkey(instanceType_);
}

const CLSID *
HippoPreferences::getInstanceClassId()
{
    return getInstanceClassId(instanceType_);
}

const CLSID *
HippoPreferences::getInstanceClassId(HippoInstanceType instanceType)
{
    switch (instanceType) {
        case HIPPO_INSTANCE_NORMAL:
        default:
            return &CLSID_HippoUI;
        case HIPPO_INSTANCE_DOGFOOD:
            return &CLSID_HippoUI_Dogfood;
        case HIPPO_INSTANCE_DEBUG:
            return &CLSID_HippoUI_Debug;
    }
}
