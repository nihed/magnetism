/* HippoPreferences.cpp: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoPreferences.h"
#include <HippoRegKey.h>
#include <limits.h>
#include <glib.h>

static const WCHAR *DUMBHIPPO_SUBKEY = L"Software\\DumbHippo\\Client";
static const WCHAR *DUMBHIPPO_SUBKEY_DEBUG = L"Software\\DumbHippo\\DebugClient";

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
        return HippoBSTR(L"messages.dumbhippo.com").CopyTo(server);
}

void
HippoPreferences::parseMessageServer(char        **nameUTF8,
                                     unsigned int *port)
{
    *nameUTF8 = NULL;
    *port = 5222;

    if (messageServer_) {
        const WCHAR *raw = messageServer_.m_str;
        const WCHAR *p = raw + messageServer_.Length();

        while (p > raw) {
            if (*(p - 1) == ':') {
                *nameUTF8 = g_utf16_to_utf8(raw, p - raw - 1, NULL, NULL, NULL);

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

        if (p == raw)
            *nameUTF8 = g_utf16_to_utf8(raw, - 1, NULL, NULL, NULL);
    }

    if (!*nameUTF8)
        *nameUTF8 = g_strdup("messages.dumbhippo.com");
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
        return HippoBSTR(L"dumbhippo.com").CopyTo(server);
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
