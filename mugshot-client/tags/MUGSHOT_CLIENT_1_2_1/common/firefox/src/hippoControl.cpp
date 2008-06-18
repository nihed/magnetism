/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#ifdef HIPPO_OS_LINUX
#include <glib.h>
#include <gtk/gtk.h>
#include <gdk/gdkx.h>
#include "hippoipc/hippo-dbus-ipc-locator.h"
#define UTF8_VALIDATE g_utf8_validate
#elif defined(HIPPO_OS_WINDOWS)
#include "HippoStdAfx.h"
#include <HippoUtil.h>
#include <hippo-com-ipc-locator.h>
#define UTF8_VALIDATE hippo_utf8_validate
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
#ifdef HAVE_XULRUNNER
#include "nsIClassInfoImpl.h"
#else
#include "nsISupportsImpl.h"
#endif

#ifdef HIPPO_OS_LINUX
// These headers are used for finding the GdkWindow for a DOM window
#include "nsIBaseWindow.h"
#include "nsIDocShell.h"
#include "nsIScriptGlobalObject.h"
#include "nsIWidget.h"
// For Firefox 2, this is "internal API".
#ifdef HAVE_XULRUNNER
// For our usage of nsIDocument, we hit a problem where the
// size of nsString and nsString_external are different on
// some architectures. Since nsIDocument has a nsString
// member, accessing subsequent members then causes a crash
// because of their different location within the structure.
//
//  http://bugzilla.redhat.com/show_bug.cgi?id=441643
//  http://bugzilla.mozilla.org/show_bug.cgi?id=430581

struct hippoDummyString {
    // These are the fields in the internal nsString in Gecko-1.9pre
    PRUnichar *mData;
    PRUint32 mLength;
    PRUint32 mFlags;
};

#undef nsString
#define nsString hippoDummyString
#include "nsIDocument.h"
#undef nsString
#define nsString_external

#include "nsPIDOMWindow.h"
#endif
#endif

hippoControl::hippoControl()
{
#ifdef HIPPO_OS_LINUX
    locator_ = HippoDBusIpcLocator::getInstance();
#elif HIPPO_OS_WINDOWS
    locator_ = HippoComIpcLocator::getInstance();
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

/* attribute AUTF8String serverUrl; */
NS_IMETHODIMP hippoControl::GetVersion(nsACString &aVersion)
{
    aVersion.Assign(HIPPO_FIREFOX_CONTROL_VERSION);
    
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

#ifdef HIPPO_OS_LINUX
static nsIWidget* GetMainWidget(nsIDOMWindow* aWindow)
{
    /* The window ID that we want to pass to the Mugshot client is 
     * the window ID that corresponds to the frame that the chat is
     * in. If the Mugshot client needs the toplevel, it can walk up
     * the hierarchy and find it, but passing the inner window allows
     * more intelligence in knowing if the chat is actually visible.
     *
     * To switch to passing the toplevel, simply insert a call to
     * gdk_window_toplevel() ... much easier than walking up the 
     * Mozilla hierarchy.
     */	
#ifndef WITH_MAEMO	
#ifdef HAVE_XULRUNNER
	// get the native window for this instance
	nsCOMPtr<nsPIDOMWindow> window(do_QueryInterface(aWindow));
	NS_ENSURE_TRUE(window, nsnull);
	nsCOMPtr<nsIDocument> doc(do_QueryInterface(window->GetExtantDocument()));
	NS_ENSURE_TRUE(doc, nsnull);
	nsCOMPtr<nsISupports> container = doc->GetContainer();
	nsCOMPtr<nsIBaseWindow> baseWindow(do_QueryInterface(container));
	NS_ENSURE_TRUE(baseWindow, nsnull);
	 
	nsCOMPtr<nsIWidget> mainWidget;
	baseWindow->GetMainWidget(getter_AddRefs(mainWidget));
	return mainWidget;	
#else
	nsCOMPtr<nsIScriptGlobalObject> global = do_QueryInterface(aWindow);
	 
	nsCOMPtr<nsIBaseWindow> baseWindow;
	if (global)
	    baseWindow = do_QueryInterface(global->GetDocShell());   
	nsCOMPtr<nsIWidget> widget;
	if (baseWindow)
	    baseWindow->GetMainWidget(getter_AddRefs(widget));
	return widget;
#endif /* HAVE_XULRUNNER */
#else
  return NULL;
#endif
}
#endif

/* void setListener (in hippoIControlListener listener); */
NS_IMETHODIMP hippoControl::SetWindow(nsIDOMWindow *window)
{
    window_ = do_GetWeakReference(window);

#ifndef WITH_MAEMO

#ifdef HIPPO_OS_LINUX
    HippoWindowId windowId = 0;

    nsCOMPtr<nsIWidget> widget = GetMainWidget(window);

    GdkWindow *nativeWindow = NULL;
    if (widget)
        nativeWindow = (GdkWindow *)widget->GetNativeData(NS_NATIVE_WINDOW);
    
    if (nativeWindow)
        windowId = (HippoWindowId)GDK_DRAWABLE_XID(nativeWindow);
    
    if (controller_ && endpoint_ && windowId)
        controller_->setWindowId(endpoint_, windowId);
#endif /* HIPPO_OS_LINUX */

#endif /* WITH_MAEMO */

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
        controller_->joinChatRoom(endpoint_, chatId.BeginReading(), participant ? true : false);
    
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
    return SendChatMessageSentiment(chatId, text, 0); // 0 == INDIFFERENT
}

/* void sendChatMessageSentiment (in AUTF8String chatId, in AUTF8String text, int PRUint32 sentiment); */
NS_IMETHODIMP hippoControl::SendChatMessageSentiment(const nsACString &chatId, const nsACString &text, PRUint32 sentiment)
{
    nsresult rv;

    rv = checkGuid(chatId);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(text);
    if (NS_FAILED(rv))
        return rv;

    if (sentiment < 0 || sentiment > 2)
        return NS_ERROR_INVALID_ARG;

    if (controller_)
        controller_->sendChatMessage(chatId.BeginReading(), text.BeginReading(), sentiment);
    
    return NS_OK;
}

/* void getApplicationInfo (in AUTF8String applicationId, in AUTF8String packageNames, in AUTF8String desktopNames); */
NS_IMETHODIMP hippoControl::GetApplicationInfo(const nsACString &applicationId, const nsACString &packageNames, const nsACString &desktopNames)
{
    nsresult rv;

    rv = checkString(applicationId);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(packageNames);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(desktopNames);
    if (NS_FAILED(rv))
        return rv;

    if (controller_ && endpoint_)
        controller_->getApplicationInfo(endpoint_, applicationId.BeginReading(), packageNames.BeginReading(), desktopNames.BeginReading());

    return NS_OK;
}

/* void installApplication (in AUTF8String applicationId, in AUTF8String packageNames, in AUTF8String desktopNames); */
NS_IMETHODIMP hippoControl::InstallApplication(const nsACString &applicationId, const nsACString &packageNames, const nsACString &desktopNames)
{
    nsresult rv;

    rv = checkString(applicationId);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(packageNames);
    if (NS_FAILED(rv))
        return rv;

    rv = checkString(desktopNames);
    if (NS_FAILED(rv))
        return rv;

    if (controller_ && endpoint_)
        controller_->installApplication(endpoint_, applicationId.BeginReading(), packageNames.BeginReading(), desktopNames.BeginReading());

    return NS_OK;
}

/* void runApplication (in AUTF8String desktopNames); */
NS_IMETHODIMP hippoControl::RunApplication (const nsACString &desktopNames)
{
    nsresult rv;

    rv = checkString(desktopNames);
    if (NS_FAILED(rv))
        return rv;

    unsigned int timestamp;
#ifdef HIPPO_OS_LINUX
    timestamp = gtk_get_current_event_time();
#else
    timestamp = 0;
#endif    
    
    if (controller_)
        controller_->runApplication(desktopNames.BeginReading(), timestamp);

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

NS_IMETHODIMP
hippoControl::OpenBrowserBar()
{
    return showHideBrowserBar(true, nsnull);
}

NS_IMETHODIMP hippoControl::CloseBrowserBar(const nsACString & nextUrl)
{
    nsresult rv;

    rv = checkString(nextUrl);
    if (NS_FAILED(rv))
        return rv;

    return showHideBrowserBar(false, NS_ConvertUTF8toUTF16(nextUrl).BeginReading());
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
hippoControl::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment, double timestamp, long serial)
{
    if (listener_)
        listener_->OnMessage(nsCString(chatId), nsCString(userId), nsCString(message), timestamp, serial, sentiment);
}

void 
hippoControl::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    if (listener_)
        listener_->UserInfo(nsCString(userId), nsCString(name), nsCString(smallPhotoUrl),
                            nsCString(currentSong), nsCString(currentArtist), musicPlaying);
}

void
hippoControl::applicationInfo(HippoEndpointId endpoint, const char *applicationId, bool canInstall, bool canRun, const char *version)
{
    if (listener_)
        listener_->ApplicationInfo(nsCString(applicationId), canInstall, canRun, nsCString(version));
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

    if (!UTF8_VALIDATE(start, end - start, NULL))
        return NS_ERROR_INVALID_ARG;

    return NS_OK;
}

nsresult
hippoControl::showHideBrowserBar(bool doShow, const PRUnichar *data)
{
    nsresult rv;

    nsCOMPtr<nsIDOMWindow> window = do_QueryReferent(window_);
    if (!window)
        return NS_ERROR_NOT_INITIALIZED;

    nsCOMPtr<nsIObserverService> observerService;
    observerService = do_GetService("@mozilla.org/observer-service;1", &rv);
    if (NS_FAILED(rv))
        return rv;

    observerService->NotifyObservers(window, doShow ? "hippo-open-bar" : "hippo-close-bar", data);
    
    return NS_OK;
}
