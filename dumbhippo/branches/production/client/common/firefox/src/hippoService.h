/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippoIService.h"
#include "hippoipc/hippo-ipc.h"
#include "nsCOMPtr.h"

class hippoService: public hippoIService, public HippoIpcListener
{
public:
    hippoService();

    NS_DECL_ISUPPORTS
    NS_DECL_HIPPOISERVICE

    // HippoIpcListener methods

    virtual void onConnect();
    virtual void onDisconnect();

    virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
    virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
    virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial);

    virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);
    
private:
    nsresult checkServerUrl(const nsACString &serverUrl, nsACString &hostPort);
    nsresult checkGuid(const nsACString &guid);
    nsresult checkString(const nsACString &str);
    
    ~hippoService();
    
    nsCString serverUrl_;
    hippoIServiceListener *listener_;
    HippoIpcLocator *locator_;
    HippoIpcController *controller_;
    HippoEndpointId endpoint_;
};
