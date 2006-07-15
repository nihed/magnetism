/* HippoConnectionPointContainerContainer.h: Generic connection point helper
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"
#include <limits.h>
#include "HippoUtil.h"
#include "HippoConnectionPointContainer.h"

HippoConnectionPointContainer::HippoConnectionPointContainer()
{
}

HippoConnectionPointContainer::~HippoConnectionPointContainer()
{
    for (ULONG i = 0; i < points_.length(); i++) {
        points_[i]->containerGone();
        points_[i]->Release();
    }
}

void
HippoConnectionPointContainer::setWrapper(IUnknown *wrapper)
{
    wrapper_ = wrapper;
}

HRESULT 
HippoConnectionPointContainer::addConnectionPoint(const IID &ifaceID)
{
    HippoConnectionPoint *newPoint = new HippoConnectionPoint(ifaceID, this);
    if (!newPoint)
        return E_OUTOFMEMORY;

    HRESULT hr = points_.append(newPoint);
    if (FAILED(hr)) {
        newPoint->Release();
        return hr;
    }

    return S_OK;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoConnectionPointContainer::QueryInterface(const IID &ifaceID, 
                                              void     **result)
{
    return wrapper_->QueryInterface(ifaceID, result);
}

STDMETHODIMP_(DWORD) 
HippoConnectionPointContainer::AddRef()
{
    return wrapper_->AddRef();
}
   
STDMETHODIMP_(DWORD) 
HippoConnectionPointContainer::Release()
{
    return wrapper_->Release();
}

////////////////// IConnectionPointContainer implementation //////////////////

STDMETHODIMP 
HippoConnectionPointContainer::EnumConnectionPoints(IEnumConnectionPoints **enumConnectionPoints)
{
    return Clone(enumConnectionPoints);
}

STDMETHODIMP
HippoConnectionPointContainer::FindConnectionPoint(const IID         &ifaceID, 
                                                   IConnectionPoint **connectionPoint)
{
    if (!connectionPoint) 
        return E_POINTER;

    for (ULONG i = 0; i < points_.length(); i++) {
        IID pointIfaceID;
        points_[i]->GetConnectionInterface(&pointIfaceID);
        if (IsEqualIID(ifaceID, pointIfaceID)) {
            points_[i]->AddRef();
            *connectionPoint = points_[i];

            return S_OK;
        }
    }

    return CONNECT_E_NOCONNECTION;
}

//////////////////// IEnumConnectionPoints implementation /////////////////////

STDMETHODIMP 
HippoConnectionPointContainer::Next(ULONG              cPoints,
                                    IConnectionPoint **points,
                                    ULONG             *fetched)
{
    if (!points || !fetched)
        return E_POINTER;

    ULONG i;
    for (i = 0; i < cPoints; i++) {
        if (curPoint_ == points_.length())
            break;

        points[i] = points_[curPoint_++];
    }

    *fetched = i;
    
    return i == cPoints ? S_OK : S_FALSE;
}

STDMETHODIMP 
HippoConnectionPointContainer::Skip(ULONG cPoints)
{
    if (cPoints <= ULONG_MAX - points_.length()) {
        curPoint_ += cPoints;
        return S_OK;
    } else {
        curPoint_ = points_.length();
        return S_FALSE;
    }
}

STDMETHODIMP 
HippoConnectionPointContainer::Reset(void)
{
    curPoint_ = 0;

    return S_OK;
}

STDMETHODIMP 
HippoConnectionPointContainer::Clone(IEnumConnectionPoints **clone)
{
    HippoConnectionPointContainer *tmp;

    if (!clone)
        return E_POINTER;

    tmp = new HippoConnectionPointContainer();
    if (!tmp)
        return E_OUTOFMEMORY;

    HRESULT hr = tmp->points_.copyFrom(points_);
    if (FAILED (hr)) {
        tmp->Release();
        return E_OUTOFMEMORY;
    }
    for (ULONG i = 0; i < points_.length(); i++)
        points_[i]->AddRef();
    
    tmp->wrapper_ = wrapper_;
    tmp->curPoint_ = curPoint_;

    *clone = tmp;

    return S_OK;
}