/* HippoJScriptEngine.cpp: Wrapper for the JScript engine, used only to force garbage collection
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <ActivScp.h>
#include <HippoUtil.h>
#include "HippoJScriptEngine.h"

class HippoJScriptEngineImpl : public IActiveScriptSite, public HippoJScriptEngine
{
public:
    HippoJScriptEngineImpl();
    ~HippoJScriptEngineImpl();

    virtual void forceGC();
    virtual void close();

    HIPPO_DECLARE_REFCOUNTING;
    STDMETHODIMP QueryInterface(const IID &ifaceID, 
                                void   **result);

    STDMETHODIMP GetLCID(LCID *lcid);
    STDMETHODIMP GetItemInfo(LPCOLESTR name, DWORD returnMask, IUnknown **item, ITypeInfo **typeInfo);
    STDMETHODIMP GetDocVersionString(BSTR *version);
    STDMETHODIMP OnScriptTerminate(const VARIANT *result, const EXCEPINFO *exceptionInfo);
    STDMETHODIMP OnStateChange(SCRIPTSTATE scriptState);
    STDMETHODIMP OnScriptError(IActiveScriptError *error);
    STDMETHODIMP OnEnterScript();
    STDMETHODIMP OnLeaveScript();

private:
    HippoPtr<IActiveScript> engine_;
    DWORD refCount_;
};

HippoJScriptEngineImpl::HippoJScriptEngineImpl()
{
    CLSID clsid;
    HRESULT hr;

    refCount_ = 1;

    HippoPtr<IActiveScript> engine;

    hr = CLSIDFromProgID(L"JScript", &clsid);
    if (FAILED(hr))
        return;

    hr = CoCreateInstance(clsid, NULL, CLSCTX_INPROC_SERVER, IID_IActiveScript, (void **)&engine);
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't get create JScript engine");
        return;
    }

    HippoQIPtr<IActiveScriptParse> parser(engine);
    if (!parser) {
        hippoDebugLogW(L"Couldn't get IActiveScriptParse");
        return;
    }

    hr = engine->SetScriptSite(this);
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't set script site: %x", hr);
    }

    hr = parser->InitNew();
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't initialize script engine: %x", hr);
        return;
    }

    hr = engine->SetScriptState(SCRIPTSTATE_STARTED);
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't start script engine: %x", hr);
        return;
    }

    engine_= engine;
}

HippoJScriptEngineImpl::~HippoJScriptEngineImpl()
{
    close();
}

void 
HippoJScriptEngineImpl::forceGC()
{
    HippoQIPtr<IActiveScriptGarbageCollector> gc(engine_);
    if (gc)
        gc->CollectGarbage(SCRIPTGCTYPE_NORMAL);
}

void 
HippoJScriptEngineImpl::close()
{
    if (engine_) {
        engine_->Close();
        engine_ = NULL;
    }
}

HIPPO_DEFINE_REFCOUNTING(HippoJScriptEngineImpl);

STDMETHODIMP 
HippoJScriptEngineImpl::QueryInterface(const IID &ifaceID, 
                                       void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, IID_IActiveScriptSite)) 
        *result = static_cast<IActiveScriptSite *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

STDMETHODIMP HippoJScriptEngineImpl::GetLCID(LCID *lcid)
{
    return E_NOTIMPL;
}

STDMETHODIMP 
HippoJScriptEngineImpl::GetItemInfo(LPCOLESTR name, DWORD returnMask, IUnknown **item, ITypeInfo **typeInfo)
{
    return TYPE_E_ELEMENTNOTFOUND;
}

STDMETHODIMP 
HippoJScriptEngineImpl::GetDocVersionString(BSTR *version)
{
    return E_NOTIMPL;
}

STDMETHODIMP 
HippoJScriptEngineImpl::OnScriptTerminate(const VARIANT *result, const EXCEPINFO *exceptionInfo)
{
    return S_OK;
}

STDMETHODIMP 
HippoJScriptEngineImpl::OnStateChange(SCRIPTSTATE scriptState)
{
    return S_OK;
}

STDMETHODIMP 
HippoJScriptEngineImpl::OnScriptError(IActiveScriptError *error)
{
    return S_OK;
}

STDMETHODIMP 
HippoJScriptEngineImpl::OnEnterScript()
{
    return S_OK;
}

STDMETHODIMP
HippoJScriptEngineImpl::OnLeaveScript()
{
    return S_OK;
}

void
HippoJScriptEngine::createInstance(HippoJScriptEngine **engine)
{
    *engine = new HippoJScriptEngineImpl();
}
