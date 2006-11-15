/* HippoInvocation.cpp: Make dynamic calls against COM objects in a type-safe fashion
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include <vector>

#include <HippoUtil.h>
#include <HippoUtilExport.h>

class HippoInvocationImpl;

class DLLEXPORT HippoInvocation
{
public:
    /**
     * Create a helper object to use to call the specified function
     * @param script the object to make the call against. Normally something
     *    like the Javascript script object for an embedded Internet Explorer
     *    instance, but actually can be any IDispatch.
     * @param functionName the name of the function to call
     */
    HippoInvocation(IDispatch *script, const HippoBSTR &functionName);

    HippoInvocation(const HippoInvocation &other);
    ~HippoInvocation();

    HippoInvocation &operator=(const HippoInvocation &other);
    
    /**
     * Add a string parameter to the list of parameters to provide to the function
     * @param value parameter value
     **/
    HippoInvocation &add(const HippoBSTR &value); // Common, so gets the simple name

    /**
     * Add a boolean parameter to the list of parameters to provide to the function
     * @param value parameter value
     **/
    HippoInvocation &addBool(bool value);

    /**
     * Add a long parameter to the list of parameters to provide to the function
     * @param value parameter value
     **/
    HippoInvocation &addLong(long value);

    /**
     * Add a double parameter to the list of parameters to provide to the function
     * @param value parameter value
     **/
    HippoInvocation &addDouble(double value);

    /**
     * Add a IDispatch parameter to the list of parameters to provide to the function
     * @param value parameter value
     **/
    HippoInvocation &addDispatch(IDispatch *dispatch);

    /**
     * Add a vector of HippoBSTR the list of parameters to provide to the function
     * This will be converted to a SAFEARRAY of VARIANT where each variant holds
     * a string. (It might be possible to use a SAFEARRAY of BSTR instead, but 
     * that hasn't been tested.)
     * @param value parameter value
     **/
    HippoInvocation &addStringVector(const std::vector<HippoBSTR> &value);

    /**
     * Execute the function, ignoring any return value.
     * 
     * @return the status from the COM call; test with SUCCEEDED() or FAILED()
     */
    HRESULT run();

    /**
     * Execute the function retrieving a return value from the call.
     *
     * @param result location to store the return value from the call; this 
     *   corresponds to the out parameter from IDispatch::Invoke() and
     *   thus the return value from a Javascript function, instead of the
     *   HRESULT return from IDispatch::Invoke().
     * @return the status from the COM call; test with SUCCEEDED() or FAILED()
     */
    HRESULT getResult(variant_t *result);

    // The default copy constructor and operator work for this class

private:
    HippoInvocationImpl *impl_;
};
