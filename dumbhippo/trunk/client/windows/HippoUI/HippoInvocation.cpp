/* HippoInvocation.cpp: Make dynamic calls against COM objects in a type-safe fashion
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "StdAfx.h"
#include "HippoInvocation.h"

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

HippoInvocation::HippoInvocation(IDispatch       *script, 
                                 const HippoBSTR &functionName)
{
    script_ = script;
    functionName_ = functionName;
}

HippoInvocation &
HippoInvocation::addBool(bool value)
{
    params_.push_back(variant_t(value));

    return *this;
}

HippoInvocation &
HippoInvocation::addLong(long value)
{
    params_.push_back(variant_t(value));

    return *this;
}

HippoInvocation &
HippoInvocation::add(const HippoBSTR &value)
{
    params_.push_back(variant_t(value.m_str));

    return *this;
}
    
HippoInvocation &
HippoInvocation::addStringVector(const std::vector<HippoBSTR> &value)
{
    // We directly store the string array into the vector element
    // rather than creating an variant and then appending that
    // to the array to avoid an unnecessary deep copy of the
    // array. (SAFEARRAY isn't reference counted.)
    params_.push_back(variant_t());
    params_.back().vt = VT_ARRAY | VT_VARIANT;
    params_.back().parray = stringVectorToSafeArray(value);

    return *this;
}

HRESULT 
HippoInvocation::run()
{
    variant_t dummy;

    return getResult(&dummy);
}

HRESULT 
HippoInvocation::getResult(variant_t *result)
{
    DISPID id = NULL;

    HRESULT hr = script_->GetIDsOfNames(IID_NULL, 
                                        &(functionName_.m_str), 1,
                                        LOCALE_SYSTEM_DEFAULT, 
                                        &id);
    if (FAILED(hr))
        return hr;

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
