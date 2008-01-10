/* HippoListenerProxy.h: track and handle requests for one IHippoUIListener
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <hippo/hippo-endpoint-proxy.h>
#include <HippoUtil.h>

class HippoListenerProxy {
public:
    static HippoListenerProxy *createInstance(HippoDataCache *dataCache, IHippoUIListener *listener);

    virtual UINT64 getId() = 0;
    virtual void unregister() = 0;
    virtual UINT64 registerEndpoint() = 0;
    virtual void unregisterEndpoint(UINT64 endpoint) = 0;

    virtual bool hasEndpoint(UINT64 endpoint) = 0;

    virtual void joinChatRoom(UINT64 endpoint, BSTR chatId, BOOL participant) = 0;
    virtual void leaveChatRoom(UINT64 endpoint, BSTR chatId) = 0;
    
    virtual STDMETHODIMP_(DWORD) AddRef() = 0;
    virtual STDMETHODIMP_(DWORD) Release() = 0;
};