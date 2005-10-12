/* HippoPreferences.cpp: client preferences
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoPreferences.h"
#include <limits.h>
#include <glib.h>

static const WCHAR *DUMBHIPPO_SUBKEY = L"Software\\DumbHippo\\Client";

HippoPreferences::HippoPreferences()
{
    signIn_ = true;

    load();
}

HRESULT
HippoPreferences::getMessageServer(BSTR *server)
{
    if (messageServer_)
	return messageServer_.CopyTo(server);
    else
	return HippoBSTR(L"dumbhippo.com").CopyTo(server);
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

    if (!nameUTF8)
	*nameUTF8 = g_strdup("dumbhippo.com");
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
HippoPreferences::loadString(HKEY         key,
			     const WCHAR *valueName,
			     BSTR        *str)
{
    long result;
    BYTE buf[1024];
    DWORD bufSize = sizeof(buf) / sizeof(buf[0]);
    DWORD type;

    result = RegQueryValueEx(key, valueName, NULL, 
			     &type, buf, &bufSize);
    if (result == ERROR_SUCCESS && type == REG_SZ)
	HippoBSTR((WCHAR *)buf).CopyTo(str);
}

void
HippoPreferences::loadBool(HKEY         key,
			   const WCHAR *valueName,
			   bool        *value)
{
    long result;
    DWORD tmp;
    DWORD bufSize = sizeof(DWORD);
    DWORD type;

    result = RegQueryValueEx(key, valueName, NULL, 
			     &type, (BYTE *)&tmp, &bufSize);
    if (result == ERROR_SUCCESS && type == REG_DWORD)
	*value = tmp != 0;
}

void
HippoPreferences::load()
{
    LONG result;
    HKEY key;

    result = RegOpenKeyEx(HKEY_CURRENT_USER, DUMBHIPPO_SUBKEY,
	                  0, KEY_READ, 
	    	          &key);
    if (result != ERROR_SUCCESS)
        return;

    messageServer_ = NULL;
    loadString(key, L"MessageServer", &messageServer_);
    webServer_ = NULL;
    loadString(key, L"WebServer", &webServer_);
    signIn_ = true;
    loadBool(key, L"SignIn", &signIn_);

    RegCloseKey(key);
}

void
HippoPreferences::saveString(HKEY         key,
			     const WCHAR *valueName, 
			     BSTR         str)
{
    if (str) {
	unsigned int len = ::SysStringLen(str);
	if (sizeof(WCHAR) * (len + 1) > UINT_MAX)
	    return;

        RegSetValueEx(key, valueName, NULL, REG_SZ,
    	              (const BYTE *)str, (DWORD)sizeof(WCHAR) * (len + 1));
    } else {
	RegDeleteValue(key, valueName);
    }
}

void
HippoPreferences::saveBool(HKEY         key,
			   const WCHAR *valueName, 
			   bool         value)
{
    DWORD tmp = value;

    RegSetValueEx(key, valueName, NULL, REG_SZ,
	          (const BYTE *)&tmp, sizeof(DWORD));
}

void
HippoPreferences::save(void)
{
    LONG result;
    HKEY key;

    result = RegCreateKeyEx(HKEY_CURRENT_USER, DUMBHIPPO_SUBKEY, NULL, NULL, 
			    REG_OPTION_NON_VOLATILE, KEY_WRITE, NULL,
			    &key, NULL);
    if (result != ERROR_SUCCESS)
	return;

    saveString(key, L"MessageServer", messageServer_);
    saveString(key, L"WebServer", webServer_);
    saveBool(key, L"SignIn", signIn_);

    RegCloseKey(key);
}
