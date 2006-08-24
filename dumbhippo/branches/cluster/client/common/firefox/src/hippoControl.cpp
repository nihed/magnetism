/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <glib.h>

#ifdef HIPPO_OS_LINUX
#include "hippoipc/hippo-dbus-ipc-locator.h"
#else
#error "Unknown platform; don't know how to get a HippoIpcLocator"
#endif

#include "nspr.h"
#include "nsMemory.h"
#include "nsNetCID.h"
#include "nsISupportsUtils.h"
#include "nsIIOService.h"
#include "nsIObserverService.h"
#include "nsIURI.h"
#include "nsIScriptSecurityManager.h"
#include "nsServiceManagerUtils.h"
#include "nsStringAPI.h"
#include "hippoControl.h"

hippoControl::hippoControl()
{
#ifdef HIPPO_OS_LINUX
    locator_ = HippoDBusIpcLocator::getInstance();
#endif

    listener_ = 0;
    controller_ = 0;
    endpoint_ = 0;
}

hippoControl::~hippoControl()
{
    Stop();
    
    if (listener_)
	listener_->Release();
}

NS_IMPL_ISUPPORTS1_CI(hippoControl, hippoIControl);


/* attribute AUTF8String serverUrl; */
NS_IMETHODIMP hippoControl::GetServerUrl(nsACString &aServerUrl)
{
    aServerUrl.Assign(serverUrl_);
    
    return NS_OK;
}

/* void start(AUTF8String serverUrl); */
NS_IMETHODIMP hippoControl::Start(const nsACString &serverUrl)
{
    nsresult rv;

    nsCString hostPort;

    rv = checkServerUrl(serverUrl, hostPort);
    if (NS_FAILED(rv))
        return rv;

    serverUrl_.Assign(serverUrl);

    controller_ = locator_->getController(hostPort.BeginReading());
    controller_->addListener(this);

    endpoint_ = controller_->registerEndpoint(this);

    return NS_OK;
}

/* void stop(); */
NS_IMETHODIMP hippoControl::Stop()
{
    if (controller_) {
        if (endpoint_ != 0) {
            controller_->unregisterEndpoint(endpoint_);
        }
        controller_->removeListener(this);
        locator_->releaseController(controller_);
        controller_ = NULL;
    }

    return NS_OK;
}

/* boolean isConnected (); */
NS_IMETHODIMP hippoControl::IsConnected(PRBool *_retval)
{
    *_retval = endpoint_ != 0;

    return NS_OK;
}

/* void setListener (in hippoIControlListener listener); */
NS_IMETHODIMP hippoControl::SetListener(hippoIControlListener *listener)
{
    listener->AddRef();
    if (listener_)
	listener_->Release();
    listener_ = listener;
    
    return NS_OK;
}

/* void joinChatRoom (in AUTF8String chatId, in boolean participant); */
NS_IMETHODIMP hippoControl::JoinChatRoom(const nsACString &chatId, PRBool participant)
{
    nsresult rv;

    rv = checkGuid(chatId);
    if (NS_FAILED(rv))
        return rv;

    if (controller_ && endpoint_)
        controller_->joinChatRoom(endpoint_, chatId.BeginReading(), participant);
    
    return NS_OK;
}

/* void leaveChatRoom (in AUTF8String chatId); */
NS_IMETHODIMP hippoControl::LeaveChatRoom(const nsACString &chatId)
{
    nsresult rv;

    rv = checkGuid(chatId);
    if (NS_FAILED(rv))
        return rv;

    if (controller_ && endpoint_)
        controller_->leaveChatRoom(endpoint_, chatId.BeginReading());
    
    return NS_OK;
}

/* void showChatWindow (in AUTF8String chatId); */
NS_IMETHODIMP hippoControl::ShowChatWindow(const nsACString &chatId)
{
    nsresult rv;

    rv = checkGuid(chatId);
    if (NS_FAILED(rv))
        return rv;

    if (controller_)
        controller_->showChatWindow(chatId.BeginReading());
    
    return NS_OK;
}

/* void sendChatMessage (in AUTF8String chatId, in AUTF8String text); */
NS_IMETHODIMP hippoControl::SendChatMessage(const nsACString &chatId, const nsACString &text)
{
    nsresult rv;

    rv = checkGuid(chatId);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(text);
    if (NS_FAILED(rv))
        return rv;

    if (controller_)
        controller_->sendChatMessage(chatId.BeginReading(), text.BeginReading());
    
    return NS_OK;
}

/* void notifyPageShared (in AUTF8String postId, in AUTF8String url); */
NS_IMETHODIMP hippoControl::NotifyPageShared(const nsACString & postId, const nsACString & url)
{
    nsresult rv;

    rv = checkGuid(postId);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(url);
    if (NS_FAILED(rv))
        return rv;

    nsCOMPtr<nsIObserverService> observerService;
    observerService = do_GetService("@mozilla.org/observer-service;1", &rv);
    if (NS_FAILED(rv))
 	return rv;

    nsCString notifyData(postId);
    notifyData.Append(",");
    notifyData.Append(url);
    observerService->NotifyObservers(NULL, "hippo-page-shared", NS_ConvertUTF8toUTF16(notifyData).BeginReading());
    
    return NS_OK;
}

void 
hippoControl::onConnect()
{
    if (endpoint_ == 0) {
        endpoint_ = controller_->registerEndpoint(this);
        if (endpoint_ && listener_)
            listener_->OnConnect();
    }
}
 
void 
hippoControl::onDisconnect()
{
    if (endpoint_ != 0) {
        endpoint_ = 0;

        if (listener_)
            listener_->OnDisconnect();
    }
}

void 
hippoControl::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant)
{
    if (listener_)
        listener_->OnUserJoin(nsCString(chatId), nsCString(userId), participant);
}

void 
hippoControl::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    if (listener_)
        listener_->OnUserLeave(nsCString(chatId), nsCString(userId));
}

void 
hippoControl::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, double timestamp, long serial)
{
    if (listener_)
        listener_->OnMessage(nsCString(chatId), nsCString(userId), nsCString(message), timestamp, serial);
}

void 
hippoControl::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    if (listener_)
        listener_->UserInfo(nsCString(userId), nsCString(name), nsCString(smallPhotoUrl),
                            nsCString(currentSong), nsCString(currentArtist), musicPlaying);
}
 
nsresult 
hippoControl::checkServerUrl(const nsACString &serverUrl, nsACString &hostPort)
{
    static NS_DEFINE_CID(kIOServiceCID, NS_IOSERVICE_CID);

    nsresult rv;
    
    nsCOMPtr<nsIIOService> ioservice;
    ioservice = do_GetService(kIOServiceCID, &rv);
    if (NS_FAILED(rv))
 	return rv;

    nsCOMPtr<nsIURI> uri;
    ioservice->NewURI(serverUrl, NULL, NULL, getter_AddRefs(uri));
    if (NS_FAILED(rv))
 	return rv;

    nsCString scheme;

    // We can only handle http URIs
    rv = uri->GetScheme(scheme);
    if (NS_FAILED(rv))
        return rv;
    
    if (!scheme.Equals(NS_LITERAL_CSTRING("http")))
        return NS_ERROR_INVALID_ARG;

    nsCOMPtr<nsIScriptSecurityManager> secMan;
    secMan = do_GetService(NS_SCRIPTSECURITYMANAGER_CONTRACTID, &rv);
    if (NS_FAILED(rv))
 	return rv;

    // Find out if we should bypass our checks 
    PRBool crossSiteMugshotEnabled;
    rv = secMan->IsCapabilityEnabled("UniversalMugshotControl",
                                     &crossSiteMugshotEnabled);
    if (NS_FAILED(rv))
 	return rv;

    if (!crossSiteMugshotEnabled) {
        rv = secMan->CheckSameOrigin(NULL, uri);
        if (NS_FAILED(rv))
            return rv;
    }

    rv = uri->GetHostPort(hostPort);
    if (NS_FAILED(rv))
        return rv;
    
    return NS_OK;
}

nsresult 
hippoControl::checkGuid(const nsACString &guid)
{
    const char *start = guid.BeginReading();
    const char *p;

    // Contents are alphanumeric (we don't generate a,e,i,o,u,E,I,O,U in our
    // GUID's at the moment, but there is no harm in allowing them)
    for (p = start; *p; p++) {
        if (!((*p >= '0' && *p <= '9') ||
              (*p >= 'A' && *p <= 'Z') ||
              (*p >= 'a' && *p <= 'z')))
            return NS_ERROR_INVALID_ARG;
    }

    // Length is 14
    if (p - start != 14) 
        return NS_ERROR_INVALID_ARG;
    
    return NS_OK;
}

nsresult
hippoControl::checkString(const nsACString &str)
{
    // This is a bit paranoid, but check that the valid we got from Javascript
    // is valid UTF-8 and doesn't contain any embedded NUL characters.
    
    const char *start = str.BeginReading();
    const char *end = str.EndReading();

    if (!g_utf8_validate(start, end - start, NULL))
        return NS_ERROR_INVALID_ARG;

    return NS_OK;
}
