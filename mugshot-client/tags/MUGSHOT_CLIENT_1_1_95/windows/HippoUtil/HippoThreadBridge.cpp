/* HippoThreadBridge.cpp: Execute callbacks across threads
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "stdafx-hippoutil.h"
#include "HippoDispatchableObject.h"
#include "HippoThreadBridge.h"
#include "HippoThreadExecutor.h"
#include "HippoUtil.h"

class HippoThreadBridgeTask : public HippoThreadTask
{
public:
    HippoThreadBridgeTask(HippoThreadBridgeImpl *impl, void *param) {
        impl_ = impl;
        param_ = param;
    }

    virtual void call();
    virtual void cancel();

private:
    HippoThreadBridgeImpl *impl_;
    void *param_;
};

// The COM object to which the callbacks are marshalled
class HippoThreadBridgeImpl : 
    public HippoDispatchableObject<IHippoCallable, HippoThreadBridgeImpl>,
    public HippoThreadExecutorHelper
{
public:
    HippoThreadBridgeImpl(HippoThreadBridge *bridge_);
    ~HippoThreadBridgeImpl();
    static ITypeInfo *getTypeInfo();

    STDMETHODIMP call(UINT64 param, UINT64 *retval);

    HRESULT invoke(void *param, void **retval);
    HRESULT invokeAsync(void *param);
    void clear();

    // HippoThreadExecutorHelper methods
    void init();
    void shutdown();

private:
    HippoThreadExecutor *executor_;
    HippoThreadBridge *bridge_;
    HippoPtr<IGlobalInterfaceTable> git_;
    DWORD cookie_;
};

HippoThreadBridge::HippoThreadBridge()
{
    impl_ = new HippoThreadBridgeImpl(this);
}

HippoThreadBridge::~HippoThreadBridge()
{
    impl_->clear();
    impl_->Release();
    impl_ = 0;
}

HRESULT
HippoThreadBridge::invoke(void *param, void **retval)
{
    return impl_->invoke(param, retval);
}

HRESULT 
HippoThreadBridge::invokeAsync(void *param)
{
    return impl_->invokeAsync(param);
}

void
HippoThreadBridgeTask::call() 
{
    impl_->invoke(param_, NULL);
    delete this;
}

void
HippoThreadBridgeTask::cancel() 
{
    delete this;
}

HippoThreadBridgeImpl::HippoThreadBridgeImpl(HippoThreadBridge *bridge)
{
    bridge_ = bridge;
    
    executor_ = HippoThreadExecutor::createInstance(this);
    
    CoCreateInstance(CLSID_StdGlobalInterfaceTable,
                     NULL,
                     CLSCTX_INPROC_SERVER,
                     IID_IGlobalInterfaceTable,
                     (void **)&git_);

    if (!git_)
        return;

    cookie_ = 0;
    git_->RegisterInterfaceInGlobal((IHippoCallable *)this, __uuidof(IHippoCallable), &cookie_);

}

HippoThreadBridgeImpl::~HippoThreadBridgeImpl()
{
    // If the executor thread is currently trying to make a call against the thread that
    // deletes the HippoThreadBridge object, then we have a cycle:
    //
    //  Executor thread waiting for Deleting thread
    //  Deleting thread waiting for Executor thread
    //
    // In this case, we'll trigger the 250ms timeout in the HippoThreadExecutor and the
    // executor thread will be forcibly killed; less than ideal but better than nothing.
    // I don't know a reasonable solution.

    delete executor_;
}

ITypeInfo *
HippoThreadBridgeImpl::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;
    if (!typeInfo)
        hippoLoadTypeInfo(L"HippoUtil.dll", __uuidof(IHippoCallable), &typeInfo, NULL);

    return typeInfo;
}

STDMETHODIMP 
HippoThreadBridgeImpl::call(UINT64 param, UINT64 *retval)
{
    if (!bridge_)
        return E_INVALIDARG;

    void *result = bridge_->call((void *)param);
    if (retval)
        *retval = (UINT64)result;
    
    return S_OK;
}

HRESULT
HippoThreadBridgeImpl::invoke(void *param, void **retval)
{
    UINT64 retval64;

    if (!cookie_)
        return E_INVALIDARG;

    HippoPtr<IHippoCallable> callable;
    HRESULT hr = git_->GetInterfaceFromGlobal(cookie_, __uuidof(IHippoCallable), (void **)&callable);
    if (FAILED(hr))
        return hr;

    if (!callable) // paranoia?
        return E_INVALIDARG;
    
    hr = callable->call((UINT64)param, &retval64);
    if (FAILED(hr))
        return hr;

    if (retval)
        *retval = (void *)retval64;

    return S_OK;
}

HRESULT 
HippoThreadBridgeImpl::invokeAsync(void *param)
{
    executor_->doAsync(new HippoThreadBridgeTask(this, param));

    return S_OK;
}

void
HippoThreadBridgeImpl::clear()
{
    bridge_ = NULL;
    if (cookie_) {
        git_->RevokeInterfaceFromGlobal(cookie_);    
        cookie_ = NULL;
    }
}

void
HippoThreadBridgeImpl::init()
{
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
}

void
HippoThreadBridgeImpl::shutdown()
{
    CoUninitialize();
}
