/* HippoPreferences.cpp: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoPreferences.h"
#include "Guid.h"
#include "Resource.h"
#include <HippoRegKey.h>
#include <limits.h>
#include <glib.h>

static const WCHAR DUMBHIPPO_SUBKEY[] = L"Software\\DumbHippo\\Client";
static const WCHAR DUMBHIPPO_SUBKEY_DOGFOOD[] = L"Software\\DumbHippo\\DogfoodClient";
static const WCHAR DUMBHIPPO_SUBKEY_DEBUG[] = L"Software\\DumbHippo\\DebugClient";

static const WCHAR DEFAULT_MESSAGE_SERVER[] = L"messages.dumbhippo.com";
static const WCHAR DEFAULT_WEB_SERVER[] = L"dumbhippo.com";

HippoPreferences::HippoPreferences(HippoInstanceType instanceType)
{
    instanceType_ = instanceType;
    signIn_ = true;

    load();
}

HRESULT
HippoPreferences::getMessageServer(BSTR *server)
{
    if (messageServer_)
        return messageServer_.CopyTo(server);
    else
        return HippoBSTR(DEFAULT_MESSAGE_SERVER).CopyTo(server);
}

void
HippoPreferences::parseServer(BSTR          server,
                              const WCHAR  *defaultHost,
                              unsigned int  defaultPort,
                              BSTR         *host,
                              unsigned int *port)
{
    *host = NULL;
    *port = defaultPort;

    if (server) {
        const WCHAR *p = server + SysStringLen(server);

        while (p > server) {
            if (*(p - 1) == ':') {
                HippoBSTR tmpStr((UINT)(p - server - 1), server);
                tmpStr.CopyTo(host);

                if (*p) {
                    WCHAR *end;
                    long tmp;

                    tmp = wcstol(p, &end, 10);
                    if (!*end) 
                        *port = tmp;
                }
                break;
            }

            p--;
        }

        if (p == server) {
            HippoBSTR tmpStr(server);
            tmpStr.CopyTo(host);
        }
    }

    if (!*host) {
        HippoBSTR tmpStr(defaultHost);
        tmpStr.CopyTo(host);
    }
}

void
HippoPreferences::parseMessageServer(BSTR         *host,
                                     unsigned int *port)
{
    parseServer(messageServer_, DEFAULT_MESSAGE_SERVER, 5222, host, port);
}

void 
HippoPreferences::setMessageServer(BSTR server)
{
    messageServer_ = server;

    save();
}

HRESULT
HippoPreferences::getWebServer(BSTR *server)
{
    if (webServer_)
        return webServer_.CopyTo(server);
    else
        return HippoBSTR(DEFAULT_WEB_SERVER).CopyTo(server);
}

void 
HippoPreferences::setWebServer(BSTR server)
{
    webServer_ = server;

    save();
}

void
HippoPreferences::parseWebServer(BSTR         *host,
                                 unsigned int *port)
{
    parseServer(webServer_, DEFAULT_WEB_SERVER, 80, host, port);
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
HippoPreferences::getInstanceSubkey()
{
    switch (instanceType_) {
        case HIPPO_INSTANCE_NORMAL:
        default:
            return DUMBHIPPO_SUBKEY;
        case HIPPO_INSTANCE_DOGFOOD:
            return DUMBHIPPO_SUBKEY_DOGFOOD;
        case HIPPO_INSTANCE_DEBUG:
            return DUMBHIPPO_SUBKEY_DEBUG;
    }
}

const CLSID *
HippoPreferences::getInstanceClassId()
{
    return getInstanceClassId(instanceType_);
}

const WORD
HippoPreferences::getInstanceIcon()
{
    switch (instanceType_) {
        case HIPPO_INSTANCE_NORMAL:
        default:
            return IDI_DUMBHIPPO;
        case HIPPO_INSTANCE_DOGFOOD:
            return IDI_DUMBHIPPO_DOGFOOD;
        case HIPPO_INSTANCE_DEBUG:
            return IDI_DUMBHIPPO_DEBUG;
    }
}

const WORD 
HippoPreferences::getInstanceDisonnectedIcon()
{
    switch (instanceType_) {
        case HIPPO_INSTANCE_NORMAL:
        default:
            return IDI_DUMBHIPPO_DISCONNECTED;
        case HIPPO_INSTANCE_DOGFOOD:
            return IDI_DUMBHIPPO_DOGFOOD_DISCONNECTED;
        case HIPPO_INSTANCE_DEBUG:
            return IDI_DUMBHIPPO_DEBUG_DISCONNECTED;
    }
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
