/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <glib.h>

#include "nspr.h"
#include "nsMemory.h"
#include "nsNetCID.h"
#include "nsISupportsUtils.h"
#include "nsIIOService.h"
#include "nsIURI.h"
#include "nsIScriptSecurityManager.h"
#include "nsServiceManagerUtils.h"
#include "nsStringAPI.h"
#include "hippoService.h"

hippoService::hippoService()
{
    listener_ = NULL;
}

hippoService::~hippoService()
{
    if (listener_)
	listener_->Release();
}

NS_IMPL_ISUPPORTS1_CI(hippoService, hippoIService);


/* readonly attribute string serverUrl; */
NS_IMETHODIMP hippoService::GetServerUrl(nsAString &aServerUrl)
{
    aServerUrl.Assign(NS_LITERAL_STRING("Foo"));
    
    return NS_OK;
}

/* void connect (in string serverUrl); */
NS_IMETHODIMP hippoService::Connect(const nsACString &serverUrl)
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

     nsCOMPtr<nsIScriptSecurityManager> secMan;
     secMan = do_GetService(NS_SCRIPTSECURITYMANAGER_CONTRACTID, &rv);
     if (NS_FAILED(rv))
 	return rv;

     rv = secMan->CheckSameOrigin(NULL, uri);
     if (NS_FAILED(rv))
 	return rv;

    return NS_OK;
}

/* void disconnect (); */
NS_IMETHODIMP hippoService::Disconnect()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void setListener (in hippoIServiceListener listener); */
NS_IMETHODIMP hippoService::SetListener(hippoIServiceListener *listener)
{
    listener->AddRef();
    if (listener_)
	listener_->Release();
    listener_ = listener;
    
    return NS_OK;
}

/* void joinChatRoom (in string chatId); */
NS_IMETHODIMP hippoService::JoinChatRoom(const nsAString &chatId)
{
    if (listener_ != NULL) {
	listener_->OnReconnect(chatId);
    }

    return NS_OK;
}

/* void leaveChatRoom (in string chatId); */
NS_IMETHODIMP hippoService::LeaveChatRoom(const nsAString &chatId)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void showChatWindow (in string chatId); */
NS_IMETHODIMP hippoService::ShowChatWindow(const nsAString &chatId)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void sendChatMessage (in AString chatId, in AString text); */
NS_IMETHODIMP hippoService::SendChatMessage(const nsAString &chatId, const nsAString &text)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}


