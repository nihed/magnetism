/* HippoCrash.cpp: Crash dump management
 *
 * Copyright Red Hat, Inc. 2007
 **/

#include "stdafx-hippoui.h"
#include <process.h>
#include <handler/exception_handler.h>
#include "HippoCrash.h"
#include <HippoRegKey.h>
#include "HippoUIUtil.h"
#include "HippoPreferences.h"
#include "Resource.h"
#include "Version.h"
#include <glib.h>

using namespace google_airbag;

static HippoInstanceType hippoInstanceType;
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

static void
setWindowIcons(HWND window)
{
    HINSTANCE instance = GetModuleHandle(NULL);
    HICON smallIcon = (HICON)LoadImage(instance, MAKEINTRESOURCE(IDI_MUGSHOT),
                                       IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    HICON bigIcon = (HICON)LoadImage(instance, MAKEINTRESOURCE(IDI_MUGSHOT),
                                     IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
    SendMessage(window, WM_SETICON, (WPARAM)ICON_SMALL, (LPARAM)smallIcon);
    SendMessage(window, WM_SETICON, (WPARAM)ICON_BIG, (LPARAM)bigIcon);
}

// There must be some easier way to get a dialog centered on the screen, but 
// I don't know what it is, so we just do it manually in our WM_INITDIALOG
static void
centerWindow(HWND window)
{
    RECT windowRect;
    int height, width;

    GetWindowRect(window, &windowRect);
    width = windowRect.right - windowRect.left;
    height = windowRect.bottom - windowRect.top;

    POINT cursorPos;
    GetCursorPos(&cursorPos);
    HMONITOR monitor = MonitorFromPoint(cursorPos, MONITOR_DEFAULTTOPRIMARY);

    MONITORINFO monitorInfo;
    monitorInfo.cbSize = sizeof(monitorInfo);
    if (GetMonitorInfo(monitor, &monitorInfo)) {
        int x = (monitorInfo.rcWork.left + monitorInfo.rcWork.right - width) / 2;
        int y = (monitorInfo.rcWork.top + monitorInfo.rcWork.bottom - height) / 2;
        MoveWindow(window, x, y, width, height, FALSE);
    }
}

static INT_PTR CALLBACK
crashDialogProc(HWND   dialogWindow,
                UINT   message,
                WPARAM wParam,
                LPARAM lParam)
{
    switch (message) {
    case WM_INITDIALOG:
        setWindowIcons(dialogWindow);
        centerWindow(dialogWindow);
        return TRUE;
    case WM_CLOSE:
        EndDialog(dialogWindow, IDCANCEL);
        return TRUE;
    case WM_COMMAND:
        int control = LOWORD(wParam);
        int message = HIWORD(wParam);
        switch (control) {
        case IDOK:
        case IDCANCEL:
            if (HIWORD(wParam) == BN_CLICKED) {
                EndDialog(dialogWindow, control);
                return TRUE;
            }
            break;
        }
    }

    return FALSE;
}

static INT_PTR CALLBACK
crashReportedDialogProc(HWND   dialogWindow,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    switch (message) {
    case WM_INITDIALOG:
        {
            const char *crashIdU = (const char *)lParam;

            setWindowIcons(dialogWindow);
            centerWindow(dialogWindow);

            HippoBSTR crashId = HippoBSTR::fromUTF8(crashIdU);
            SendDlgItemMessage(dialogWindow, IDC_CRASH_ID, WM_SETTEXT,
                               0, (LPARAM)crashId.m_str);

            return TRUE;
        }
    case WM_CLOSE:
        EndDialog(dialogWindow, IDCANCEL);
        return TRUE;
    case WM_COMMAND:
        int control = LOWORD(wParam);
        int message = HIWORD(wParam);
        switch (control) {
        case IDOK:
            if (HIWORD(wParam) == BN_CLICKED) {
                EndDialog(dialogWindow, control);
                return TRUE;
            }
            break;
        }
    }

    return FALSE;
}

static INT_PTR CALLBACK
repeatCrashDialogProc(HWND   dialogWindow,
                      UINT   message,
                      WPARAM wParam,
                      LPARAM lParam)
{
    switch (message) {
    case WM_INITDIALOG:
        {
            setWindowIcons(dialogWindow);
            centerWindow(dialogWindow);
        
            // Substitute the current version into the dialog text in the right place
            WCHAR buffer[1024];
            SendDlgItemMessage(dialogWindow, IDC_VERSION, WM_GETTEXT,
                               (WPARAM)(sizeof(buffer) / sizeof(buffer[0])),
                               (LPARAM)buffer);
            WCHAR *pos = wcsstr(buffer, L"@VERSION@");
            if (pos) {
                HippoBSTR substituted = HippoBSTR((unsigned int)(pos - buffer), buffer);
                substituted.appendUTF8(VERSION, -1);
                substituted.Append(pos + wcslen(L"@VERSION@"));
                SendDlgItemMessage(dialogWindow, IDC_VERSION, WM_SETTEXT,
                                   0, (LPARAM)substituted.m_str);
            }

            return TRUE;
        }
    case WM_CLOSE:
        EndDialog(dialogWindow, IDCLOSE);
        return TRUE;
    case WM_COMMAND:
        int control = LOWORD(wParam);
        int message = HIWORD(wParam);
        switch (control) {
        case IDCLOSE:
            if (HIWORD(wParam) == BN_CLICKED) {
                EndDialog(dialogWindow, control);
                return TRUE;
            }
            break;
        }
    }

    return FALSE;
}

bool
hippoCrashReport(HippoInstanceType instanceType, const char *crashName)
{
    HippoBSTR webServer;
    HINSTANCE instance = GetModuleHandle(NULL);
    bool respawn = checkRespawn();

    HippoPreferences::getWebServer(instanceType, &webServer);
    
    if (respawn) {
        HippoBSTR file = hippoUserDataDir(L"CrashDump");

        file.Append('\\');
        file.appendUTF8(crashName, -1);
        file.Append(L".dmp");

        INT_PTR result = DialogBoxParam(instance, MAKEINTRESOURCE(IDD_CRASH), NULL, 
                                        crashDialogProc, NULL);

        if (result != IDOK) // User cancelled
            return true;

        DialogBoxParam(instance, MAKEINTRESOURCE(IDD_CRASH_REPORTED), NULL, 
                       crashReportedDialogProc, (LPARAM)crashName);

        return true;
    } else {
        INT_PTR result = DialogBoxParam(instance, MAKEINTRESOURCE(IDD_REPEAT_CRASH), NULL, 
                                        repeatCrashDialogProc, NULL);

        return false;
    }
}

static bool
hippoCrashCallback(const wchar_t      *dump_path,
                   const wchar_t      *minidump_id,
                   void               *context,
                   EXCEPTION_POINTERS *exinfo,
                   bool                succeeded)
{
    WCHAR *instanceArgument = NULL;
    WCHAR exePath[1024];

    HINSTANCE instance = GetModuleHandle(NULL);
    if (!GetModuleFileName(instance, exePath, sizeof(exePath) / sizeof(exePath[0])))
        return succeeded;

    switch (hippoInstanceType) {
    case HIPPO_INSTANCE_NORMAL:
        break;
    case HIPPO_INSTANCE_DEBUG:
        instanceArgument = L"--debug";
        break;
    case HIPPO_INSTANCE_DOGFOOD:
        instanceArgument = L"--dogfood";
        break;
    }

    _wspawnl(_P_NOWAIT, exePath, L"HippoUI", L"--crash-dump", minidump_id, instanceArgument, NULL);

    return succeeded;
}


void
hippoCrashInit(HippoInstanceType instanceType)
{
    if (IsDebuggerPresent())
        return;

    hippoInstanceType = instanceType;

    HippoBSTR dumpDir = hippoUserDataDir(L"CrashDump");
    handler = new ExceptionHandler(std::wstring(dumpDir.m_str),
                                   NULL, 
                                   hippoCrashCallback, NULL,
                                   true); // install a global exception catcher
}

void
hippoCrashDump()
{
    if (handler)
        handler->WriteMinidump();
}
