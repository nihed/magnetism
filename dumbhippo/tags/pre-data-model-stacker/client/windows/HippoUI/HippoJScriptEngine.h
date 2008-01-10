/* HippoJScriptEngine.h: Wrapper for the JScript engine, used to force garbage collection
 *
 * Copyright Red Hat, Inc. 2006
 **/

// The entire reason for the existance of this class is a hack; once
// we close a HippoChatWindow, we need to force the JScript garbage
// collector for the HippoChatManager thread to run, and the only way
// I know to do that is to create a new Javascript engine.
// 
// This class could be extended if we actually wanted to run Javascript
// code outside a web browser for some reason, but that's not in any
// current plans.

class HippoJScriptEngine
{
public:
    static void createInstance(HippoJScriptEngine **engine);

    virtual STDMETHODIMP_(DWORD) AddRef() = 0;
    virtual STDMETHODIMP_(DWORD) Release() = 0;

    /* Run the garbage collector; this is useful because a single
     * garbage collector is shared for all Javascript engines created
     * by a single host thread.
     */
    virtual void forceGC() = 0;

    /**
     * Close the underlying Javascript engine and release resources;
     * this *must* be called or the object will leak. (To fix that
     * problem, the script site object would need to be split from
     * the wrapper.)
     */
    virtual void close() = 0;
};
