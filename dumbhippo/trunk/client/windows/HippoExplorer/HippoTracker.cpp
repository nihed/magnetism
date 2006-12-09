/* HippoTracker.cpp: Browser helper object to track user visited URL
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoexplorer.h"
#include "HippoTracker.h"
#include "Guid.h"
#include "Globals.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

HippoTracker::HippoTracker(void)
{
    refCount_ = 1;
    dllRefCount++;
    updater_ = NULL;

    hippoLoadRegTypeInfo(LIBID_SHDocVw, 1, 1,
                         &DIID_DWebBrowserEvents2, &eventsTypeInfo_, 
                         NULL);
}

HippoTracker::~HippoTracker(void)
{
    clearUpdater();

    // In case setSite(NULL) wasn't called
    clearSite();
        
    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoTracker::QueryInterface(const IID &ifaceID, 
                             void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IHippoTracker)) 
        *result = static_cast<IHippoTracker *>(this);
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(static_cast<IHippoTracker*>(this));
    else if (IsEqualIID(ifaceID, DIID_DWebBrowserEvents2))
        *result = static_cast<DWebBrowserEvents2 *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoTracker)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoTracker::SetSite(IUnknown *site)
{
    clearSite();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IWebBrowser2>(&site_)))
            return E_FAIL;
        
        /* We could call createUpdater() here, but IE is a little touchy
         * during initialization, so we wait until we find out the title/url
         * for the first time. I think it would be safe to start registration
         * now we do it in a separate thread, but we'll avoid taking the chance
         */
        HippoQIPtr<IConnectionPointContainer> container(site);
        if (container)
        {
            if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2,
                                                         &connectionPoint_))) 
            {
                // The COM-safe downcast here is a little overkill ... 
                // we actually just need to disambiguate
                HippoQIPtr<IUnknown> unknown(static_cast<DWebBrowserEvents2 *>(this));
                connectionPoint_->Advise(unknown, &connectionCookie_);
            }
        }
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoTracker::GetSite(const IID &iid, 
                      void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}

//////////////////////// IDispatch implementation ///////////////////

STDMETHODIMP
HippoTracker::GetIDsOfNames (const IID   &iid,
                             OLECHAR    **names,  
                             unsigned int cNames,          
                             LCID         lcid,                   
                             DISPID *     dispID)
{
    return DispGetIDsOfNames(eventsTypeInfo_, names, cNames, dispID);
}

STDMETHODIMP
HippoTracker::GetTypeInfo (unsigned int infoIndex,  
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
 HippoTracker::GetTypeInfoCount (unsigned int *pcTInfo)
 {
    if (pcTInfo == NULL)
      return E_INVALIDARG;

    *pcTInfo = 1;

    return S_OK;
 }
  
 STDMETHODIMP
 HippoTracker::Invoke (DISPID        member,
                       const IID    &iid,
                       LCID          lcid,              
                       WORD          flags,
                       DISPPARAMS   *dispParams,
                       VARIANT      *result,
                       EXCEPINFO    *excepInfo,  
                       unsigned int *argErr)
 {
      /* There are various events that occur as the user navigates to a
       * new page. While DocumentComplete is most authoritative, it
       * can take a very long time to occur - it won't happen until
       * all images in the page have been loaded, and so forth. On
       * the other hand, BeginNavigate2 is too early; the navigation
       * could be blocked, handled in a new window, and so forth.
       * What we do is look for DocumentComplete, but also check for
       * a change (for title *and* URL) when we get a TitleChange 
       * notification. The assumption is that the title will change
       * briefly even if we are navigating to a page with the same
       * title. If it doesn't, then we'll eventually get DocumentComplete
       * anyways.
       */
      switch (member) {
        case DISPID_DOCUMENTCOMPLETE:
             if (dispParams->cArgs == 2 &&
                 dispParams->rgvarg[1].vt == VT_DISPATCH &&
                 dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT))
             {
                 createUpdater();
                 update();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
        case DISPID_TITLECHANGE:
             if (dispParams->cArgs == 1 &&
                 dispParams->rgvarg[0].vt == VT_BSTR)
             {
                 createUpdater();
                 update();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
         default:
             return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
}

/////////////////////////////////////////////////////////////////////

void
HippoTracker::update()
{
    HippoBSTR url;
    HippoBSTR name;

    if (updater_ && site_ &&
        SUCCEEDED(site_->get_LocationURL(&url)) &&
        SUCCEEDED(site_->get_LocationName(&name)) &&
        url && ((WCHAR *)url)[0] && name && ((WCHAR *)name)[0]) 
    {
        updater_->setInfo(url, name);
    }
}

void
HippoTracker::clearSite()
{
    clearUpdater();

    if (site_)
        site_ = NULL;
  
    if (connectionPoint_) {
        if (connectionCookie_) {
            connectionPoint_->Unadvise(connectionCookie_);
            connectionCookie_ = 0;
        }
           
        connectionPoint_ = NULL;
    }
}

void
HippoTracker::createUpdater()
{
    if (!updater_ && site_) {
        updater_ = new HippoTrackerUpdater(static_cast<IHippoTracker*>(this), site_);
    }
}

void
HippoTracker::clearUpdater()
{
    if (updater_) {
        delete updater_;
        updater_ = NULL;
    }
}

STDMETHODIMP 
HippoTracker::Navigate(BSTR url){
    VARIANT missing;
    missing.vt = VT_EMPTY;
    return site_->Navigate(url, &missing, &missing, &missing, &missing);
}
