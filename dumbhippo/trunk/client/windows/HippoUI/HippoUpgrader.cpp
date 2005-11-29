/* HippoUpgader.cpp: Download new versions of the client
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include <process.h>
#include "HippoHTTP.h"
#include "HippoUpgrader.h"
#include "HippoUI.h"
#include "HippoRegKey.h"
#include "Version.h"

static const WCHAR *DUMBHIPPO_SUBKEY_UPGRADE = L"Software\\DumbHippo\\Upgrade";

// Parse a positive integer, return false if parsing fails
static bool
parseInt(const char *str, 
         size_t     len,
         int        *result)
{
    if (len == 0)
        return false;
    if (str[0] < '0' || str[0] > '9') // don't support white space, +, -
        return false;

    char *end;
    unsigned long val = strtoul(str, &end, 10);
    if (end != str + len)
        return false;
    if (val > INT_MAX)
        return false;
    *result = (int)val;

    return true;
}

// parse a major.minor.micro triplet. Returns false and sets
// major, minor, micro to 0 if parsing fails
static bool
parseVersion(const char *version,
             int        *major,
             int        *minor,
             int        *micro)
{
    const char *firstPeriod = strchr(version, '.');
    const char *secondPeriod;
    
    if (firstPeriod)
        secondPeriod = strchr(firstPeriod + 1, '.');

    if (!firstPeriod || !secondPeriod || 
        !parseInt(version, firstPeriod - version, major) ||
        !parseInt(firstPeriod + 1, secondPeriod - (firstPeriod + 1), minor) ||
        !parseInt(secondPeriod + 1, strlen(secondPeriod + 1), micro))
    {
        *major = *minor = *micro = 0;
        return false;
    }

    return true;
}

// compare to major.minor.micro version strings. Unparseable
// strings are treated the same as 0.0.0
static int 
compareVersions(const char *versionA, const char *versionB)
{
    int majorA, minorA, microA;
    int majorB, minorB, microB;

    parseVersion(versionA, &majorA, &minorA, &microA);
    parseVersion(versionB, &majorB, &minorB, &microB);

    if (majorA < majorB)
        return -1;
    else if (majorA > majorB)
        return 1;
    else if (minorA < minorB)
        return -1;
    else if (minorA > minorB)
        return 1;
    else if (microA < microB)
        return -1;
    else if (microA > microB)
        return 1;
    else
        return 0;
}

HippoUpgrader::HippoUpgrader()
{
    state_ = STATE_UNKNOWN;
    currentVersion_ = NULL;
    minVersion_ = NULL;
    downloadFile_ = NULL;
 }

HippoUpgrader::~HippoUpgrader()
{
    if (downloadFile_)
        CloseHandle(downloadFile_);

    g_free(currentVersion_);
    g_free(minVersion_);
}


void
HippoUpgrader::setUI(HippoUI *ui)
{
    ui_ = ui;
}

void 
HippoUpgrader::setUpgradeInfo(const char *minVersion,
                              const char *currentVersion,
                              const char *downloadUrl)
{
    minVersion_ = g_strdup(minVersion);
    currentVersion_ = g_strdup(currentVersion);
    downloadUrl_.setUTF8(downloadUrl);

    if (compareVersions(VERSION, currentVersion_) < 0) {
        startDownload();
    } else {
        state_ = STATE_GOOD;
    }
}

void 
HippoUpgrader::performUpgrade()
{
    if (state_ != STATE_DOWNLOADED || !progressFilename_ || !progressCompleted_) {
        ui_->debugLogW(L"HippoUpgrader::performUpgrade() called unexpectedly");
        return;
    }

    int result = (int)ShellExecute(NULL, L"open", progressFilename_, NULL, L"C:\\", SW_SHOW);
    if (result < 32)
        ui_->debugLogW(L"Error starting windows installer: %d", result);
}

void 
HippoUpgrader::handleError(HRESULT result)
{
    ui_->logError(L"Error downloading upgrade", result);
    state_ = STATE_ERROR;
    delete http_;  
    http_ = NULL;
}

void 
HippoUpgrader::handleBytesRead(void *responseData, long responseBytes)
{
    DWORD bytesWritten;

    while (responseBytes > 0) {
        if (!WriteFile(downloadFile_, responseData, responseBytes, &bytesWritten, NULL)) {
            ui_->logError(L"Error writing upgrade to disk", GetLastError());
            state_ = STATE_ERROR;
            return;
        }
        responseBytes -= bytesWritten;
    }
}

void 
HippoUpgrader::handleComplete(void *responseData, long responseBytes)
{
    CloseHandle(downloadFile_);
    downloadFile_ = NULL;

    if (state_ != STATE_ERROR) {
        ui_->debugLogW(L"Successfully downloaded upgrade");
        state_ = STATE_DOWNLOADED;

        progressCompleted_ = true;
        saveProgress();
        
        ui_->onUpgradeReady();
    } else {
    }

    delete http_;  
    http_ = NULL;
}

static void
getBase(BSTR  str, 
        BSTR *out,
        WCHAR separator)
{
    WCHAR *p = str + wcslen(str);
    while (p > str && *(p - 1) != separator)
        p--;
    HippoBSTR result(p);
    result.CopyTo(out);
}

bool
HippoUpgrader::openDownloadFile(BSTR  basename,
                                BSTR *filename)
{
    WCHAR path[MAX_PATH];
    SHGetFolderPath(NULL, CSIDL_LOCAL_APPDATA, NULL, SHGFP_TYPE_CURRENT, path);
    if (StringCchCat(path, MAX_PATH, L"\\DumbHippo") != S_OK ||
        (!CreateDirectory(path, NULL) &&
         GetLastError() != ERROR_ALREADY_EXISTS))
    {
        hippoDebug(L"Error creating local data directory: %ls", path);
        return false;
    }
    if (StringCchCat(path, MAX_PATH, L"\\Upgrade") != S_OK ||
        (!CreateDirectory(path, NULL) &&
         GetLastError() != ERROR_ALREADY_EXISTS))
    {
        hippoDebug(L"Error creating download directory: %ls", path);
        return false;
    }
    if (StringCchCat(path, MAX_PATH, L"\\") != S_OK ||
        StringCchCat(path, MAX_PATH, basename) != S_OK)
        return false;

    HANDLE file = CreateFile(path, FILE_WRITE_DATA, 0, NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (!file)
        return false;

    HippoBSTR tmp = path;
    HRESULT hr = tmp.CopyTo(filename);
    if (!SUCCEEDED(hr))
        return false;

    downloadFile_ = file;

    return true;
}

void
HippoUpgrader::loadProgress()
{
    HippoRegKey key(HKEY_CURRENT_USER, 
                    DUMBHIPPO_SUBKEY_UPGRADE,
                    false);

    progressUrl_ = NULL;
    key.loadString(L"DownloadUrl", &progressUrl_);

    g_free(progressVersion_);
    progressVersion_ = NULL;
    HippoBSTR tmp;
    key.loadString(L"DownloadVersion", &tmp);
    if (tmp)
        progressVersion_ = g_utf16_to_utf8(tmp, -1, NULL, NULL, NULL);

    progressFilename_ = NULL;
    key.loadString(L"DownloadFilename", &progressFilename_);

    progressModified_ = NULL;
    key.loadString(L"DownloadModified", &progressModified_);

    progressCompleted_ = FALSE;
    key.loadBool(L"DownloadCompleted", &progressCompleted_);
}

void
HippoUpgrader::saveProgress()
{
    HippoRegKey key(HKEY_CURRENT_USER, 
                    DUMBHIPPO_SUBKEY_UPGRADE,
                    true);

    key.saveString(L"DownloadUrl", progressUrl_);
    HippoBSTR tmp;
    if (progressVersion_)
        tmp.setUTF8(progressVersion_);
    key.saveString(L"DownloadVersion", tmp);
    key.saveString(L"DownloadFilename", progressFilename_);
    key.saveString(L"DownloadModified", progressModified_);
    key.saveBool(L"DownloadCompleted", progressCompleted_);
}

void
HippoUpgrader::startDownload()
{
    HippoBSTR downloadBase;
    HippoBSTR filename;
    getBase(downloadUrl_, &downloadBase, '/');

    loadProgress();
    if (progressFilename_) {
        HippoBSTR progressBase;
        getBase(progressFilename_, &progressBase, '\\');

        if (progressUrl_ && wcscmp(progressUrl_, downloadUrl_) == 0 &&
            progressVersion_ && strcmp(progressVersion_, currentVersion_) == 0 &&
            progressBase && wcscmp(progressBase, downloadBase) == 0) 
        {
            if (progressCompleted_) {
                state_ = STATE_DOWNLOADED;
                ui_->onUpgradeReady();
                return;
            } else {
                if (resumeDownload())
                    return;
                // if resuming fails, try a fresh download
            }
        }
        
        // Stale download, delete 
        DeleteFile(progressFilename_);
        progressFilename_ = NULL;
        saveProgress();
    }

    if (openDownloadFile(downloadBase, &filename)) {
        // start the download

        ui_->debugLogW(L"Downloading %ls to %ls", downloadUrl_, filename);

        http_ = new HippoHTTP();
        http_->doGet(downloadUrl_, this);

        progressUrl_ = downloadUrl_;
        g_free(progressVersion_);
        progressVersion_ = g_strdup(currentVersion_);
        progressFilename_ = filename;
        progressModified_ = NULL;
        progressCompleted_ = false;
        saveProgress();

        state_ = STATE_DOWNLOADING;
    } else {
        state_ = STATE_ERROR;
    }
}

bool
HippoUpgrader::resumeDownload()
{
    return false;
}
