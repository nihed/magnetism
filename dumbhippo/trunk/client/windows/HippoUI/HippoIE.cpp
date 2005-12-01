#include "StdAfx.h"
#include ".\hippoie.h"
#include <mshtml.h>
#include <HippoUtil.h>

HippoIE::HippoIE(void)
{
}

HippoIE::~HippoIE(void)
{
}

HRESULT
HippoIE::invokeJavascript(HippoPtr<IWebBrowser2> browser, WCHAR * funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list vap;
    va_start(vap, nargs);
    HRESULT result = HippoIE::invokeJavascript(browser, funcName, invokeResult, nargs, vap);
    va_end(vap);
    return result;
}

HRESULT 
HippoIE::invokeJavascript(HippoPtr<IWebBrowser2> browser, WCHAR * funcName, VARIANT *invokeResult, int nargs, va_list vap)
{    
    HippoBSTR funcNameStr(funcName);
    VARIANT *arg;
    int argc;
    IDispatch *docDispatch;
    browser->get_Document(&docDispatch);
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    IDispatch *script;
    doc->get_Script(&script);

    DISPID id = NULL;
    HRESULT result = script->GetIDsOfNames(IID_NULL,&(funcNameStr.m_str),1,LOCALE_SYSTEM_DEFAULT,&id);
    if (FAILED(result))
        return result;

    DISPPARAMS args;
    ZeroMemory(&args, sizeof(args));
    args.rgvarg = new VARIANT[nargs];
    args.cNamedArgs = 0;

    argc = 0;
    for (int argc = 0; argc < nargs; argc++) {
        arg = va_arg (vap, VARIANT *);
        // This has to be in reverse order for some reason...CRACK
        VARIANT *destArg = &(args.rgvarg[(nargs-argc)-1]);
        VariantInit(destArg);
        result = VariantCopy(destArg, arg);
        if (FAILED(result))
            return result;
    }
    args.cArgs = nargs;

    EXCEPINFO excep;
    ZeroMemory(&excep, sizeof(excep));
    UINT argErr;
    result = script->Invoke(id, IID_NULL, 0, DISPATCH_METHOD,
                            &args, invokeResult, &excep, &argErr);
    delete [] args.rgvarg;
    return result;
}