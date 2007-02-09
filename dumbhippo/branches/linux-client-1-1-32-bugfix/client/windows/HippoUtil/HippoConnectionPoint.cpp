/* HippoConnectionPoint.h: Generic connection point helper
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoutil.h"
#include <limits.h>
#include "HippoUtil.h"
#include "HippoConnectionPoint.h"

HippoConnectionPoint::HippoConnectionPoint(const IID                 &ifaceID,
                                           IConnectionPointContainer *container)
{
    ifaceID_ = ifaceID;
    refCount_ = 1;
    curConnection_ = 0;
    nextCookie_ = 1;
}

HippoConnectionPoint::~HippoConnectionPoint()
{
    for (ULONG i = 0; i < connections_.length(); i++)
        connections_[i].pUnk->Release();
}

void
HippoConnectionPoint::containerGone()
{
    container_ = NULL;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoConnectionPoint::QueryInterface(const IID &ifaceID, 
                                     void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IConnectionPoint *>(this));
    else if (IsEqualIID(ifaceID, IID_IConnectionPoint))
        *result = static_cast<IConnectionPoint *>(this);
    else if (IsEqualIID(ifaceID, IID_IEnumConnections))
        *result = static_cast<IEnumConnections *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}

HIPPO_DEFINE_REFCOUNTING(HippoConnectionPoint)

////////////////////// IConnectionPoint implementation //////////////////////

STDMETHODIMP 
HippoConnectionPoint::GetConnectionPointContainer(IConnectionPointContainer **container)
{
    if (!container)
        return E_POINTER;

    container_->AddRef();
    *container = container_;

    return S_OK;
}

STDMETHODIMP 
HippoConnectionPoint::GetConnectionInterface(IID *ifaceID)
{
    if (!ifaceID)
        return E_POINTER;

    *ifaceID = ifaceID_;

    return S_OK;
}

STDMETHODIMP 
HippoConnectionPoint::Advise(IUnknown *sink, 
                             DWORD    *cookie)
{
    CONNECTDATA newData;

    if (!cookie)
        return E_POINTER;

    newData.pUnk = sink;
    newData.dwCookie = nextCookie_++;

    HRESULT hr = connections_.append(newData);
    if (FAILED(hr)) {
        *cookie = 0;
        return hr;
    } else {
        *cookie = newData.dwCookie;
        sink->AddRef();

        return S_OK;
    }
}

STDMETHODIMP 
HippoConnectionPoint::Unadvise(DWORD cookie)
{
    for (ULONG i = 0; i < connections_.length(); i++) {
        if (connections_[i].dwCookie == cookie) {
            connections_.remove(i);
            return S_OK;
        }
    }

    return CONNECT_E_NOCONNECTION;
}

STDMETHODIMP 
HippoConnectionPoint::EnumConnections(IEnumConnections **enumConnections)
{
    return Clone(enumConnections);
}

//////////////// IEnumConnections implementation //////////////////////

STDMETHODIMP 
HippoConnectionPoint::Next(ULONG        cConnections,
                           CONNECTDATA *connections,
                           ULONG       *fetched)
{
    if (!connections || !fetched)
        return E_POINTER;

    ULONG i;
    for (i = 0; i < cConnections; i++) {
        if (curConnection_ == connections_.length())
            break;

        connections[i] = connections_[curConnection_++];
    }

    *fetched = i;
    
    return i == cConnections ? S_OK : S_FALSE;
}

STDMETHODIMP 
HippoConnectionPoint::Skip(ULONG cConnections)
{
    if (cConnections <= ULONG_MAX - connections_.length()) {
        curConnection_ += cConnections;
        return S_OK;
    } else {
        curConnection_ = connections_.length();
        return S_FALSE;
    }
}

STDMETHODIMP 
HippoConnectionPoint::Reset(void)
{
    curConnection_ = 0;

    return S_OK;
}

STDMETHODIMP 
HippoConnectionPoint::Clone(IEnumConnections **clone)
{
    HippoConnectionPoint *tmp;

    if (!clone)
        return E_POINTER;

    tmp = new HippoConnectionPoint(ifaceID_, NULL);
    if (!tmp)
        return E_OUTOFMEMORY;

    HRESULT hr = tmp->connections_.copyFrom(connections_);
    if (FAILED (hr)) {
        tmp->Release();
        return E_OUTOFMEMORY;
    }
    for (ULONG i = 0; i < connections_.length(); i++)
        connections_[i].pUnk->AddRef();
    
    tmp->curConnection_ = curConnection_;
    tmp->nextCookie_ = nextCookie_;

    *clone = tmp;

    return S_OK;
}