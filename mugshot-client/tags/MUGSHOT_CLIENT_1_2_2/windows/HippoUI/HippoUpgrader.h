/* HippoUpgader.h: Download new versions of the client
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <HippoUtil.h>
#include "HippoHTTP.h"

class HippoUI;

class HippoUpgrader : public HippoHTTPAsyncHandler
{
public:
    enum State {
        STATE_UNKNOWN,
        STATE_GOOD,
        STATE_DOWNLOADING,
        STATE_DOWNLOADED,
        STATE_ERROR
    };

    HippoUpgrader();
    ~HippoUpgrader();

    void setUI(HippoUI *ui);

    void setUpgradeInfo(const char *minVersion,
                        const char *currentVersion,
                        const char *downloadUrl);
    void performUpgrade();

    // HippoHTTPAsyncHandler methods
    void handleError(HRESULT result);
    void handleBytesRead(void *responseData, long responseBytes);
    void handleComplete(void *responseData, long responseBytes);
    void handleGotSize(long responseSize);

private:
    bool openDownloadFile(BSTR  basename,
                          BSTR *filename);
    void loadProgress();
    void saveProgress();
    void startDownload();
    bool resumeDownload();

    HippoUI *ui_;
    State state_;

    char *minVersion_;
    char *currentVersion_;

    HippoBSTR downloadUrl_;
    HANDLE downloadFile_;

    HippoHTTP *http_;

    HippoBSTR progressUrl_;
    HippoBSTR progressVersion_;
    HippoBSTR progressFilename_;
    HippoBSTR progressModified_;
    long progressSize_;
    bool progressCompleted_;
};