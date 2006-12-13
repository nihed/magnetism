/* HippoInvocation.cpp: Make dynamic calls against COM objects in a type-safe fashion
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "stdafx-hippoutil.h"
#include "HippoInvocation.h"

class HippoInvocationImpl
{
public:
    HippoInvocationImpl(IDispatch *script, const HippoBSTR &functionName);
    void add(const HippoBSTR &value); // Common, so gets the simple name
    void addBool(bool value);
    void addLong(long value);
    void addDouble(double value);
    void addDispatch(IDispatch *dispatch);
    void addStringVector(const std::vector<HippoBSTR> &value);
    HRESULT run();
    HRESULT getResult(variant_t *result);

    HIPPO_DECLARE_REFCOUNTING;

private:
    unsigned refCount_;
    HippoPtr<IDispatch> script_;
    HippoBSTR functionName_;
    std::vector<variant_t> params_;
};

HippoInvocation::HippoInvocation(IDispatch *script, const HippoBSTR &functionName)
{
    impl_ = new HippoInvocationImpl(script, functionName);
}

HippoInvocation::HippoInvocation(const HippoInvocation &other)
{
    impl_ = other.impl_;
    impl_->AddRef();
}

HippoInvocation::~HippoInvocation()
{
    impl_->Release();
}

HippoInvocation &
HippoInvocation::operator=(const HippoInvocation &other)
{
    impl_->Release();
    impl_ = other.impl_;
    impl_->AddRef();

    return *this;
}


HippoInvocation &
HippoInvocation::add(const HippoBSTR &value)
{
    impl_->add(value);

    return *this;
}

HippoInvocation &
HippoInvocation::addBool(bool value)
{
    impl_->addBool(value);

    return *this;
}

HippoInvocation &
HippoInvocation::addLong(long value)
{
    impl_->addLong(value);

    return *this;
}

HippoInvocation &
HippoInvocation::addDouble(double value)
{
    impl_->addDouble(value);

    return *this;
}

HippoInvocation &
HippoInvocation::addDispatch(IDispatch *value)
{
    impl_->addDispatch(value);
    
    return *this;
}

HippoInvocation &
HippoInvocation::addStringVector(const std::vector<HippoBSTR> &value)
{
    impl_->addStringVector(value);
    
    return *this;
}

HRESULT 
HippoInvocation::run()
{
    return impl_->run();
}

HRESULT 
HippoInvocation::getResult(variant_t *result)
{
    return impl_->getResult(result);
}


static SAFEARRAY *
stringVectorToSafeArray(const std::vector<HippoBSTR> &args)
{
    SAFEARRAY *result = SafeArrayCreateVector(VT_VARIANT, 0, (ULONG)args.size());
    if (result == 0)
        throw std::bad_alloc();

    VARIANT *data;
    SafeArrayAccessData(result, (void**)&data);

    std::vector<HippoBSTR>::const_iterator iter = args.begin();
    for (unsigned int i = 0; i < args.size(); i++) {
        data[i] = variant_t((*iter).m_str).Detach();
        iter++;
    }

    SafeArrayUnaccessData(result);

    return result;
}

HIPPO_DEFINE_REFCOUNTING(HippoInvocationImpl)

HippoInvocationImpl::HippoInvocationImpl(IDispatch       *script, 
                                         const HippoBSTR &functionName)
{
    refCount_ = 1;
    script_ = script;
    functionName_ = functionName;
}

void
HippoInvocationImpl::addBool(bool value)
{
    params_.push_back(variant_t(value));
}

void
HippoInvocationImpl::addLong(long value)
{
    params_.push_back(variant_t(value));
}

void
HippoInvocationImpl::addDouble(double value)
{
    params_.push_back(variant_t(value));
}

void
HippoInvocationImpl::addDispatch(IDispatch *value)
{
    params_.push_back(variant_t(value));
}

void
HippoInvocationImpl::add(const HippoBSTR &value)
{
    params_.push_back(variant_t(value.m_str));
}
    
void
HippoInvocationImpl::addStringVector(const std::vector<HippoBSTR> &value)
{
    // We directly store the string array into the vector element
    // rather than creating an variant and then appending that
    // to the array to avoid an unnecessary deep copy of the
    // array. (SAFEARRAY isn't reference counted.)
    params_.push_back(variant_t());
    params_.back().vt = VT_ARRAY | VT_VARIANT;
    params_.back().parray = stringVectorToSafeArray(value);
}

HRESULT 
HippoInvocationImpl::run()
{
    variant_t dummy;

    return getResult(&dummy);
}

HRESULT 
HippoInvocationImpl::getResult(variant_t *result)
{
    DISPID id = NULL;

    HRESULT hr = script_->GetIDsOfNames(IID_NULL, 
                                        &(functionName_.m_str), 1,
                                        LOCALE_SYSTEM_DEFAULT, 
                                        &id);

    if (FAILED(hr)) {
        hippoDebugLogW(L"HippoInvocationImpl::getResult: GetIDsOfNames failed, %x", hr);
        return hr;
    }

    DISPPARAMS args;
    ZeroMemory(&args, sizeof(args));
    args.cArgs = (UINT)params_.size();
    args.rgvarg = new VARIANT[args.cArgs];
    args.cNamedArgs = 0;

    // Parameters to Invoke() are in reverse order
    std::vector<variant_t>::reverse_iterator iter = params_.rbegin();
    for (unsigned int i = 0; i < args.cArgs; i++)
        args.rgvarg[i] = *iter++;

    EXCEPINFO excep;
    ZeroMemory(&excep, sizeof(excep));
    UINT argErr;
    hr = script_->Invoke(id, IID_NULL, 0, DISPATCH_METHOD,
                         &args, result, &excep, &argErr);

    delete [] args.rgvarg;

    return hr;
}
