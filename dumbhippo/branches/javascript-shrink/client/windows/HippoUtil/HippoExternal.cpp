/* HippoExternal.cpp: object made available to Javascript from HippoIE
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoutil.h"

#import <msxml3.dll>  named_guids  // For CLSID_XMLHTTPRequest

#include <ExDispid.h>
#include "HippoUtil.h"
#include "HippoExternal.h"


HippoExternal::HippoExternal(void)
{
    refCount_ = 1;
    application_ = NULL;

    hippoLoadRegTypeInfo(LIBID_HippoUtil, 0, 1, &IID_IHippoExternal, &ifaceTypeInfo_, NULL);
}

HippoExternal::~HippoExternal(void)
{
}

void
HippoExternal::setApplication(IDispatch *application)
{
    application_ = application;
}

// IHippoExternal

STDMETHODIMP
HippoExternal::get_Application(IDispatch **application)
{
    if (!application)
        return E_POINTER;

    *application = application_;
    if (*application) {
        (*application)->AddRef();
        return S_OK;
    } else {
        return E_NOTIMPL;;
    }
}

STDMETHODIMP
HippoExternal::DebugLog(BSTR str)
{
    hippoDebugLogW(L"%s", str);

    return S_OK;
}

STDMETHODIMP 
HippoExternal::GetXmlHttp(IXMLHttpRequest **request)
{
    return CoCreateInstance(CLSID_XMLHTTPRequest, NULL, CLSCTX_INPROC,
                            MSXML2::IID_IXMLHTTPRequest, (void**) request);
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoExternal::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoExternal*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoExternal)) 
        *result = static_cast<IHippoExternal *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoExternal)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

    STDMETHODIMP
HippoExternal::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoExternal::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoExternal::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}
        
STDMETHODIMP
HippoExternal::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    HippoQIPtr<IHippoExternal> hippoExternal(static_cast<IHippoExternal *>(this));
    HRESULT hr = DispInvoke(hippoExternal, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
