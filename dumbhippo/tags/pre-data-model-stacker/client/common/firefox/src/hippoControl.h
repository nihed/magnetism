/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippoIControl.h"
#include "hippo-ipc.h"
#include "nsCOMPtr.h"
#include "nsIWeakReference.h"
#include "nsIWeakReferenceUtils.h"

#define HIPPO_FIREFOX_CONTROL_VERSION "1.3.0"

class hippoControl: public hippoIControl, public HippoIpcListener
{
public:
    hippoControl();

    NS_DECL_ISUPPORTS
    NS_DECL_HIPPOICONTROL

    // HippoIpcListener methods

    virtual void onConnect();
    virtual void onDisconnect();

    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment, double timestamp, long serial);

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);
    virtual void applicationInfo(HippoEndpointId endpoint, const char *applicationId, bool canInstall, bool canRun, const char *version);
    
private:
    nsresult checkServerUrl(const nsACString &serverUrl, nsACString &hostPort);
    nsresult checkGuid(const nsACString &guid);
    nsresult checkString(const nsACString &str);
    nsresult showHideBrowserBar(bool doShow, const PRUnichar *data);
    
    ~hippoControl();
    
    nsCString serverUrl_;
    hippoIControlListener *listener_;
    nsWeakPtr window_;
    HippoIpcLocator *locator_;
    HippoIpcController *controller_;
    HippoEndpointId endpoint_;
};
