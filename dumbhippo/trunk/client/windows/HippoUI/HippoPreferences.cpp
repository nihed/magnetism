/* HippoPreferences.cpp: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoPreferences.h"
#include <HippoRegKey.h>
#include <limits.h>
#include <glib.h>

static const WCHAR DUMBHIPPO_SUBKEY[] = L"Software\\DumbHippo\\Client";
static const WCHAR DUMBHIPPO_SUBKEY_DEBUG[] = L"Software\\DumbHippo\\DebugClient";

static const WCHAR DEFAULT_MESSAGE_SERVER[] = L"messages.dumbhippo.com";
static const WCHAR DEFAULT_WEB_SERVER[] = L"dumbhippo.com";

HippoPreferences::HippoPreferences(bool debug)
{
    debug_ = debug;
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
    HippoRegKey key(HKEY_CURRENT_USER, 
                    debug_ ? DUMBHIPPO_SUBKEY_DEBUG : DUMBHIPPO_SUBKEY,
                    false);

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
    HippoRegKey key(HKEY_CURRENT_USER, 
                    debug_ ? DUMBHIPPO_SUBKEY_DEBUG : DUMBHIPPO_SUBKEY,
                    true);

    key.saveString(L"MessageServer", messageServer_);
    key.saveString(L"WebServer", webServer_);
    key.saveBool(L"SignIn", signIn_);
}
