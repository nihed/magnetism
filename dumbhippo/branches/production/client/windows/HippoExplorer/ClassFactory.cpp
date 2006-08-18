/* ClassFactory.h: Standard implementation of IClassFactory
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx-hippoexplorer.h"
#include "ClassFactory.h"
#include "Guid.h"
#include "HippoChatControl.h"
#include "HippoExplorerBar.h"
#include "HippoToolbarAction.h"
#include "HippoTracker.h"
#include "HippoEmbed.h"

ClassFactory::ClassFactory(const CLSID &classID)
{
    classID_ = classID;
    refCount_ = 1;
    dllRefCount++;
}

ClassFactory::~ClassFactory()
{
    dllRefCount--;
}

//////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
ClassFactory::QueryInterface(const IID &ifaceID, 
                             void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, IID_IClassFactory)) 
        *result = static_cast<IClassFactory *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}

HIPPO_DEFINE_REFCOUNTING(ClassFactory)

/////////////////////// IClassFactory implementation ////////////////////

template<class T> 
inline HRESULT
createInstance(const IID &ifaceID, void **result)
{
    T *t = new T();
    if (!t)
        return E_OUTOFMEMORY;

    HRESULT hr = t->QueryInterface (ifaceID, result);
    t->Release();

    return hr;
}

STDMETHODIMP 
ClassFactory::CreateInstance (IUnknown  *outer, 
                              const IID &ifaceID, 
                              void     **result)
{
    if (outer)
        return CLASS_E_NOAGGREGATION;

    if (IsEqualCLSID(classID_, CLSID_HippoChatControl)) {
        return createInstance<HippoChatControl>(ifaceID, result);
    } else if (IsEqualCLSID(classID_, CLSID_HippoExplorerBar)) {
        return createInstance<HippoExplorerBar>(ifaceID, result);
    } else if (IsEqualCLSID(classID_, CLSID_HippoToolbarAction)) {
        return createInstance<HippoToolbarAction>(ifaceID, result);
    } else if (IsEqualCLSID(classID_, CLSID_HippoTracker)) {
        return createInstance<HippoTracker>(ifaceID, result);
    } else if (IsEqualCLSID(classID_, CLSID_HippoEmbed)) {
        return createInstance<HippoEmbed>(ifaceID, result);
    } else {
        return E_UNEXPECTED;
    }
}

STDMETHODIMP 
ClassFactory::LockServer (BOOL lock)
{
    return E_NOTIMPL;
}
