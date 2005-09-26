/* HippoConnectionPointContainer.h: Generic connection point container help
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <olectl.h>
#include "HippoArray.h"
#include "HippoConnectionPoint.h"

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

class DLLEXPORT HippoConnectionPointContainer :
    public IConnectionPointContainer,
    private IEnumConnectionPoints
{
public:
    HippoConnectionPointContainer();
    ~HippoConnectionPointContainer();

    void HippoConnectionPointContainer::setWrapper(IUnknown *wrapper);
    HRESULT addConnectionPoint(const IID &ifaceID);

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IConnectionPointContainer methods
    STDMETHODIMP EnumConnectionPoints(IEnumConnectionPoints **);
    STDMETHODIMP FindConnectionPoint(const IID &, IConnectionPoint **);

private:
    // IEnumConnectionPoints methods
    STDMETHODIMP Next(ULONG, IConnectionPoint **, ULONG *);
    STDMETHODIMP Skip(ULONG);
    STDMETHODIMP Reset(void);        
    STDMETHODIMP Clone(IEnumConnectionPoints **);

private:
    IConnectionPoint * findConnectionPoint(const IID &ifaceID);
    IUnknown *wrapper_;
    DWORD refCount_;
    HippoArray<HippoConnectionPoint *> points_;
    ULONG curPoint_;
};
