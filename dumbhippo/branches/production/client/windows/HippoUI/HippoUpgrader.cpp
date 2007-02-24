/* HippoUpgader.cpp: Download new versions of the client
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx-hippoui.h"
#include <process.h>
#include "HippoHTTP.h"
#include "HippoUpgrader.h"
#include "HippoUI.h"
#include "HippoUIUtil.h"
#include "HippoRegKey.h"
#include "Version.h"

static const WCHAR *HIPPO_SUBKEY_UPGRADE = HIPPO_REGISTRY_KEY L"\\Upgrade";

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
    progressVersion_ = NULL;
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
    if (state_ == STATE_DOWNLOADING || state_ == STATE_DOWNLOADED) {
        // We got another version response while we were downloading;
        // it's probably the same version as last time, and caused
        // by a temporary interruption, but even if it is something
        // new trying to get it will mess us up. Just ignore the 
        // new version information; cancelling is especially difficult
        // in the STATE_DOWNLOADED version, since the HippoUI is
        // displaying a dialog to the user. We'll pick up the version
        // the next time the user reconnects.
        ui_->debugLogU("HippoUpgrader in state %d, ignoring upgrade until reconnect"); 
        return;
    }

    minVersion_ = g_strdup(minVersion);
    currentVersion_ = g_strdup(currentVersion);
    try {
        downloadUrl_.setUTF8(downloadUrl);
    } catch (std::exception &e) {
        hippoDebugLogU("Failed to convert download url from UTF-8: %s", e.what());
        return;
    }

    ui_->debugLogU("HippoUpgrader comparing running version %s to next version %s", VERSION, currentVersion_); 
    if (compareVersions(VERSION, currentVersion_) < 0) {
        startDownload();
    } else {
        ui_->debugLogU("HippoUpgrader switching to STATE_GOOD"); 
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

    HINSTANCE result = ShellExecute(NULL, L"open", progressFilename_, NULL, L"C:\\", SW_SHOW);
    if (reinterpret_cast<ptrdiff_t>(result) < 32)
        ui_->debugLogW(L"Error starting windows installer: %d", result);
}

void 
HippoUpgrader::handleError(HRESULT result)
{
    ui_->logHresult(L"Error downloading upgrade", result);
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
            ui_->logHresult(L"Error writing upgrade to disk", GetLastError());
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
        if (responseBytes != progressSize_) {
            hippoDebugLogW(L"Upgrader: bad response, only received %ld bytes", responseBytes);
            state_ = STATE_ERROR;
        } else  {
            ui_->debugLogW(L"Successfully downloaded upgrade");
            state_ = STATE_DOWNLOADED;
    
            progressCompleted_ = true;
            saveProgress();
        
            ui_->onUpgradeReady();
        }
    }

    delete http_;  
    http_ = NULL;
}

void
HippoUpgrader::handleGotSize(long responseSize) {
    hippoDebugLogW(L"Upgrader: Content-Length is %ld", responseSize);
    progressSize_ = responseSize;
    saveProgress();
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
	HippoBSTR path = hippoUserDataDir(L"Upgrade");
	if (!path)
		return false;

	path.Append('\\');
	path.Append(basename);

    HANDLE file = CreateFile(path.m_str, FILE_WRITE_DATA, 0, NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (!file)
        return false;

    try {
        HippoBSTR tmp = path;
        tmp.CopyTo(filename);
    } catch (std::bad_alloc) {
        return false;
    }

    downloadFile_ = file;

    return true;
}

void
HippoUpgrader::loadProgress()
{
    HippoRegKey key(HKEY_CURRENT_USER, 
                    HIPPO_SUBKEY_UPGRADE,
                    false);

    progressUrl_ = NULL;
    key.loadString(L"DownloadUrl", &progressUrl_);

    progressVersion_ = NULL;
    key.loadString(L"DownloadVersion", &progressVersion_);

    progressFilename_ = NULL;
    key.loadString(L"DownloadFilename", &progressFilename_);

    progressModified_ = NULL;
    key.loadString(L"DownloadModified", &progressModified_);

    progressSize_ = -1;
    key.loadLong(L"DownloadSize", &progressSize_);

    progressCompleted_ = FALSE;
    key.loadBool(L"DownloadCompleted", &progressCompleted_);
}

void
HippoUpgrader::saveProgress()
{
    HippoRegKey key(HKEY_CURRENT_USER, 
                    HIPPO_SUBKEY_UPGRADE,
                    true);

    key.saveString(L"DownloadUrl", progressUrl_);
    key.saveString(L"DownloadVersion", progressVersion_);
    key.saveString(L"DownloadFilename", progressFilename_);
    key.saveString(L"DownloadModified", progressModified_);
    key.saveLong(L"DownloadSize", progressSize_);
    key.saveBool(L"DownloadCompleted", progressCompleted_);
}

void
HippoUpgrader::startDownload()
{
    HippoBSTR downloadBase;
    HippoBSTR filename;
    getBase(downloadUrl_, &downloadBase, '/');

    ui_->debugLogW(L"HippoUpgrader starting download"); 

    loadProgress();
    if (progressFilename_) {
        HippoBSTR progressBase;
        getBase(progressFilename_, &progressBase, '\\');

        HippoUStr progressVersionU(progressVersion_);

        if (progressUrl_ && wcscmp(progressUrl_, downloadUrl_) == 0 &&
            progressVersionU.c_str() && strcmp(progressVersionU.c_str(), currentVersion_) == 0 &&
            progressBase && wcscmp(progressBase, downloadBase) == 0) 
        {
            if (progressCompleted_) {
                WIN32_FILE_ATTRIBUTE_DATA fileAttributeData;
                if (GetFileAttributesEx(progressFilename_, GetFileExInfoStandard, &fileAttributeData) &&
                    fileAttributeData.nFileSizeHigh == 0 &&
                    fileAttributeData.nFileSizeLow == progressSize_) 
                {
                    state_ = STATE_DOWNLOADED;
                    ui_->onUpgradeReady();

                    return;
                }

                // Either the downloaded file doesn't exist or it has the wrong
                // size, try downloading again.

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
        http_->doGet(downloadUrl_, false, this);

        progressUrl_ = downloadUrl_;
        progressVersion_.setUTF8(currentVersion_);
        progressFilename_ = filename;
        progressModified_ = NULL;
        progressSize_ = -1;
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
