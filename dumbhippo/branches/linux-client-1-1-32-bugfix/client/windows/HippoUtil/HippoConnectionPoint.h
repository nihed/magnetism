/* HippoConnectionPoint.h: Generic connection point helper
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <olectl.h>
#include "HippoArray.h"

template <class T> class HippoArray;

class HippoConnectionPoint :
    public IConnectionPoint,
    private IEnumConnections
{
public:
    HippoConnectionPoint(const IID                 &ifaceID, 
                         IConnectionPointContainer *container);
    ~HippoConnectionPoint(void);

    // Called when the container is freed
    void containerGone();

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IConnectionPoint methods
    STDMETHODIMP GetConnectionPointContainer(IConnectionPointContainer **);
    STDMETHODIMP GetConnectionInterface(IID *);
    STDMETHODIMP Advise(IUnknown *pUnkSink, DWORD *);
    STDMETHODIMP Unadvise(DWORD);
    STDMETHODIMP EnumConnections(IEnumConnections **);

private:
    // IEnumConnections methods
    STDMETHODIMP Next(ULONG, LPCONNECTDATA, ULONG *);
    STDMETHODIMP Skip(ULONG);
    STDMETHODIMP Reset(void);        
    STDMETHODIMP Clone(IEnumConnections **);

private:
    IID ifaceID_;
    DWORD refCount_;
    HippoArray<CONNECTDATA> connections_;
    ULONG curConnection_; // For enumeration
    DWORD nextCookie_;
    IConnectionPointContainer *container_;
};
