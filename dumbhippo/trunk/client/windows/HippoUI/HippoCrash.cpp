/* HippoCrash.h: Crash dump management
 *
 * Copyright Red Hat, Inc. 2007
 **/

#include "stdafx-hippoui.h"
#include <process.h>
#include <handler/exception_handler.h>
#include "HippoCrash.h"
#include <HippoRegKey.h>
#include "HippoUIUtil.h"
#include <glib.h>

using namespace google_airbag;

static ExceptionHandler *handler;

// Number of crash times we keep in a registry key
#define MAX_SAVED_CRASH_TIMES 5

// If with the addition of this crash, we've seen more than RECENT_MAX_CRASHES 
// crashes in the last RECENT_TIME seconds, we don't automatically respawn
#define RECENT_MAX_CRASHES 2
#define RECENT_TIME 300        

static void
crashTimesFromString(HippoBSTR crashTimesString, long times[])
{
	HippoUStr ustr(crashTimesString);
	char **split = g_strsplit(ustr.c_str(), ";", -1);

	int i = 0;
	while (i < MAX_SAVED_CRASH_TIMES && split[i]) {
		times[i] = atol(split[i]);
		i++;
	}
	while (i < MAX_SAVED_CRASH_TIMES) {
		times[i] = 0;
		i++;
	}

	g_strfreev(split);
}

static HippoBSTR
crashTimesToString(long times[])
{
	HippoBSTR result;
	WCHAR buf[32];

	for (int i = 0; i < MAX_SAVED_CRASH_TIMES && times[i] != 0; i++) {
		if (result.Length() > 0)
			result.Append(';');
		StringCchPrintfW(buf, sizeof(buf) / sizeof(buf[0]), L"%ld", times[i]);
		result.Append(buf);
	}

	return result;
}

static bool
checkRespawn()
{
	HippoRegKey key(HKEY_CURRENT_USER, HIPPO_REGISTRY_KEY L"\\Client", true);

	HippoBSTR crashTimesString;
	if (!key.loadString(L"CrashTimes", &crashTimesString)) {
		crashTimesString = HippoBSTR(L"");
	}

	long now;
	GTimeVal tmp;
	g_get_current_time(&tmp);
	now = tmp.tv_sec;

	long crashTimes[MAX_SAVED_CRASH_TIMES];
	crashTimesFromString(crashTimesString, crashTimes);

	for (int i = MAX_SAVED_CRASH_TIMES - 1; i > 0; i--)
		crashTimes[i] = crashTimes[i - 1];
	crashTimes[0] = now;

	int recentCrashes = 0;
	for (int i = 0; i < MAX_SAVED_CRASH_TIMES && crashTimes[i] != 0; i++) {
		if (now - crashTimes[i] < RECENT_TIME)
			recentCrashes++;
	}

	crashTimesString = crashTimesToString(crashTimes);

	key.saveString(L"CrashTimes", crashTimesString.m_str);

	return recentCrashes <= RECENT_MAX_CRASHES;
}

bool
hippoCrashReport(const char *crashName)
{
	long crashTimes[MAX_SAVED_CRASH_TIMES] = { 0 };
	long now;
	bool respawn = checkRespawn();
	
	if (respawn) {
		HippoBSTR file = hippoUserDataDir(L"CrashDump");

		file.Append('\\');
		file.appendUTF8(crashName, -1);

		hippoDebugDialog(L"Mugshot crashed. Ouch! Information about the crash was written to\n    %ls\n", file.m_str);

		return true;
	} else {
		hippoDebugDialog(L"Mugshot seems to be repeatedly crashing, please try again later");

		return false;
	}
}

static bool
hippo_crash_callback(const wchar_t      *dump_path,
                     const wchar_t      *minidump_id,
                     void               *context,
                     EXCEPTION_POINTERS *exinfo,
                     bool                succeeded)
{
#define BUF_SIZE 2048
    WCHAR exePath[BUF_SIZE];
	WCHAR dumpArgument[BUF_SIZE];
	const WCHAR dumpSuffix[] = L".dmp";
	int i, j;

	HINSTANCE instance = GetModuleHandle(NULL);
    if (!GetModuleFileName(instance, exePath, BUF_SIZE))
        return succeeded;

	// Handcode strcat here to avoid standard library dependency
	j = 0;
	for (i = 0; j < BUF_SIZE && minidump_id[i]; i++)
		dumpArgument[j++] = minidump_id[i];
	for (i = 0; j < BUF_SIZE && dumpSuffix[i]; i++)
		dumpArgument[j++] = dumpSuffix[i];

	if (j == BUF_SIZE)
		return succeeded;
	dumpArgument[j] = '\0';

    _wspawnl(_P_NOWAIT, exePath, L"HippoUI", L"--crash-dump", dumpArgument, NULL);

	return succeeded;
}


void
hippoCrashInit()
{
	if (IsDebuggerPresent())
		return;

	HippoBSTR dumpDir = hippoUserDataDir(L"CrashDump");
	handler = new ExceptionHandler(std::wstring(dumpDir.m_str),
								   NULL, 
								   hippo_crash_callback, NULL,
								   true); // install a global exception catcher
}

void
hippoCrashDump()
{
	if (handler)
		handler->WriteMinidump();
}
