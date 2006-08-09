#include "stdafx-hippoui.h"
#include <ExDispid.h>
#include "HippoLogWindow.h"
#include "HippoExternalBrowser.h"

HippoExternalBrowser::HippoExternalBrowser(const WCHAR *url, bool quitOnDelete, HippoExternalBrowserEvents *events)
{
    quitOnDelete_ = quitOnDelete;
    events_ = events;

    // IE specific code
    hippoLoadRegTypeInfo(LIBID_SHDocVw, 1, 1,
                         &DIID_DWebBrowserEvents2, &eventsTypeInfo_, 
                         NULL);

    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
                     IID_IWebBrowser2, (void **)&ie_);
    HippoBSTR urlStr(url);
    VARIANT missing;
    missing.vt = VT_EMPTY;
    ie_->Navigate(urlStr, &missing, &missing, &missing, &missing);
    ie_->put_Visible(VARIANT_TRUE);
    HippoQIPtr<IConnectionPointContainer> container(ie_);
    if (container) {
        if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2, &connectionPoint_))) {
            HippoQIPtr<IUnknown> unknown(static_cast<DWebBrowserEvents2 *>(this));
            connectionPoint_->Advise(unknown, &connectionCookie_);
        } else {
            hippoDebugLogW(L"failed to find browser connection point");
        }
    } else {
        hippoDebugLogW(L"failed to cast browser to IConnectionPointContainer");
    }
}

HippoExternalBrowser::~HippoExternalBrowser(void)
{
    disconnect();
    if (quitOnDelete_)
        quit();
}

void
HippoExternalBrowser::disconnect()
{
    if (connectionPoint_) {
        if (connectionCookie_) {
            connectionPoint_->Unadvise(connectionCookie_);
            connectionCookie_ = 0;
        }
        connectionPoint_ = NULL;
    }
}

void
HippoExternalBrowser::quit()
{
    if (ie_ != NULL) {
        disconnect();
        ie_->Quit();
        ie_->Release();
        ie_ = NULL;
    }
}

void
HippoExternalBrowser::navigate(WCHAR * url)
{
    if (ie_) {
        HippoBSTR urlStr(url);
        VARIANT missing;
        missing.vt = VT_EMPTY;
        ie_->Navigate(urlStr, &missing, &missing, &missing, &missing);
    }
}

void
HippoExternalBrowser::injectBrowserBar()
{
    if (ie_ != NULL) {
#if 0
        HippoBSTR barIDString(L"{4D5C8C25-D075-11d0-B416-00C04FB90376}");
        VARIANT barID;
        barID.vt = VT_BSTR;
        barID.bstrVal = barIDString;

        VARIANT show;
        show.vt = VT_BOOL;
        show.boolVal = VARIANT_TRUE;

        VARIANT unused;
        unused.vt = VT_EMPTY;

        HRESULT hr = webBrowser->ShowBrowserBar(&barID, &show, &unused);
        if (!SUCCEEDED (hr)) 
            hippoDebug(L"Couldn't show browser bar: %X", hr);
#endif
    }
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoExternalBrowser::QueryInterface(const IID &ifaceID, 
                                     void    **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch) || IsEqualIID(ifaceID, DIID_DWebBrowserEvents2))
        *result = static_cast<IDispatch *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoExternalBrowser)

//////////////////////// IDispatch implementation ///////////////////

STDMETHODIMP
HippoExternalBrowser::GetIDsOfNames (const IID   &iid,
                             OLECHAR    **names,  
                             unsigned int cNames,          
                             LCID         lcid,                   
                             DISPID *     dispID)
{
    return DispGetIDsOfNames(eventsTypeInfo_, names, cNames, dispID);
}

STDMETHODIMP
HippoExternalBrowser::GetTypeInfo (unsigned int infoIndex,  
                           LCID         lcid,                  
                           ITypeInfo  **ppTInfo)
{
   if (ppTInfo == NULL)
      return E_INVALIDARG;

   *ppTInfo = NULL;

   if (infoIndex != 0)
      return DISP_E_BADINDEX;

   eventsTypeInfo_->AddRef();
   *ppTInfo = eventsTypeInfo_;

   return S_OK;
}

 STDMETHODIMP 
 HippoExternalBrowser::GetTypeInfoCount (unsigned int *pcTInfo)
 {
    if (pcTInfo == NULL)
      return E_INVALIDARG;

    *pcTInfo = 1;

    return S_OK;
 }

STDMETHODIMP
HippoExternalBrowser::Invoke (DISPID        member,
                      const IID    &iid,
                      LCID          lcid,              
                      WORD          flags,
                      DISPPARAMS   *dispParams,
                      VARIANT      *result,
                      EXCEPINFO    *excepInfo,  
                      unsigned int *argErr)
 {
     if (events_ == NULL)
         return S_OK;
     switch (member) {
         case DISPID_NAVIGATECOMPLETE2:
            if (dispParams->cArgs == 2 &&
                dispParams->rgvarg[1].vt == VT_DISPATCH &&
                dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT)) {
                    HippoQIPtr<IDispatch> dispatch(ie_);
                    if (dispatch == dispParams->rgvarg[1].pdispVal) {
                        VARIANT *refvar = dispParams->rgvarg[0].pvarVal;
                        events_->onNavigate(this, refvar->bstrVal);
                    }
                    return S_OK;
                } else {
                    return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
                }
        case DISPID_DOCUMENTCOMPLETE:
            if (dispParams->cArgs == 2 &&
                dispParams->rgvarg[1].vt == VT_DISPATCH &&
                dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT))
            {
                HippoQIPtr<IDispatch> dispatch(ie_);
                if (dispatch == dispParams->rgvarg[1].pdispVal)
                    events_->onDocumentComplete(this);
                return S_OK;
            } else {
                return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
            }
        case DISPID_ONQUIT:
            if (dispParams->cArgs == 0)
            {
                events_->onQuit(this);    
                return S_OK;
            } else {
                return DISP_E_BADPARAMCOUNT;
            }
        default:
            return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
 }