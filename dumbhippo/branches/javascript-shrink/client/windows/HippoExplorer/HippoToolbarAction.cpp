/* HippoToolbarAction.cpp: COM Object called by toolbar button
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoexplorer.h"
#include "HippoToolbarAction.h"
#include "Guid.h"
#include "Globals.h"
#include <process.h>
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

HippoToolbarAction::HippoToolbarAction(void)
{
    refCount_ = 1;
    dllRefCount++;
    launcher_.setListener(this);
}

HippoToolbarAction::~HippoToolbarAction(void)
{
    // In case setSite(NULL) wasn't called
    clearSite();
        
    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoToolbarAction::QueryInterface(const IID &ifaceID, 
                                   void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IOleCommandTarget)) 
        *result = static_cast<IOleCommandTarget *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoToolbarAction)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoToolbarAction::SetSite(IUnknown *site)
{
    clearSite();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IServiceProvider>(&site_)))
            return E_FAIL;

        if (FAILED(site_->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &browser_))) {
            site_ = NULL;
            browser_ = NULL;

            return E_FAIL;
        }
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoToolbarAction::GetSite(const IID &iid, 
                            void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}

/////////////////// IOleCommandTarget implementation ///////////////////

STDMETHODIMP
HippoToolbarAction::QueryStatus (const GUID *commandGroup,
                                 ULONG       nCommands,
                                 OLECMD     *commands,
                                 OLECMDTEXT *commandText)
{
    // The docs are inconsistent about what the different command IDs are,
    // and don't define the command group. Treat everything the same

    for (ULONG i = 0; i < nCommands; i++) {
        // the commandText parameter queries for extra information; IE always
        // passes NULL, but we provide an implementation anyways
        if (commandText) {
            if ((commandText->cmdtextf & OLECMDTEXTF_NAME) != 0) {
                commandText->cwActual = (ULONG)wcslen(L"Share Link");
                StringCchCopy(commandText->rgwz, commandText->cwBuf, L"Share Link");
            } else if ((commandText->cmdtextf & OLECMDTEXTF_STATUS) != 0) {
                commandText->cwActual = (ULONG)wcslen(L"");
                StringCchCopy(commandText->rgwz, commandText->cwBuf, L"");
            }

            commandText = NULL; // the text result is for the first supported command
        }

        // If we are waiting for the UI to start, we display the icon insensitive
        commands[i].cmdf = OLECMDF_SUPPORTED;
        if (!shareUrl_)
            commands[i].cmdf |= OLECMDF_ENABLED;
    }

    return S_OK;
}

STDMETHODIMP
HippoToolbarAction::Exec (const GUID *commandGroup,
                          DWORD       commandId,
                          DWORD       nCommandExecOptions,
                          VARIANTARG *commandInput,
                          VARIANTARG *commandOutput)
{
    // The docs are inconsistent about what the different command IDs are,
    // and don't define the command group. Treat everything the same

    if (!browser_) {
        hippoDebugDialog(L"No browser is known");
        return E_FAIL;
    }

    HippoBSTR url;
    HippoBSTR title;

    if (!(SUCCEEDED(browser_->get_LocationURL(&url)) &&
          SUCCEEDED(browser_->get_LocationName(&title)) &&
          url && ((WCHAR *)url)[0] && title && ((WCHAR *)title)[0])) 
    {
        hippoDebugDialog(L"There isn't a current web page to share");
        return E_FAIL;
    }

    HippoPtr<IHippoUI> ui;

    if (SUCCEEDED(launcher_.getUI(&ui))) {
        doShareLink(ui, url, title);
    } else {
        // Stash away the URL and Title to use when the UI shows up
        shareUrl_ = url;
        shareTitle_ = title;

        if (SUCCEEDED(launcher_.launchUI())) {
            updateCommandEnabled();
        } else {
            shareUrl_ = NULL;
            shareTitle_ = NULL;
        }
    }

    return S_OK;

    
    HippoPtr<IUnknown> unknown;

    return S_OK;
}

/////////////////// HippoUILaunchListener implementation ///////////////////

void 
HippoToolbarAction::onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui)
{
    HippoBSTR url = shareUrl_;
    HippoBSTR title = shareTitle_;

    shareUrl_ = NULL;
    shareTitle_ = NULL;
    updateCommandEnabled();

    doShareLink(ui, url, title);
}

void 
HippoToolbarAction::onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason)
{
    shareUrl_ = NULL;
    shareTitle_ = NULL;
    updateCommandEnabled();

    hippoDebugDialog(L"%s", reason);
}

/////////////////////////////////////////////////////////////////////

void
HippoToolbarAction::clearSite()
{
    if (site_) {
        site_ = NULL;
        browser_ = NULL;
    }
}

// Tell the container in which we are embedded to recheck command
// sensitivity; the actual new value of sensitivity is conveyed by
// IOleCommandTarget::QueryStatus()
void
HippoToolbarAction::updateCommandEnabled()
{
    HippoQIPtr<IOleCommandTarget> frameTarget(site_);
    if (frameTarget) {
        frameTarget->Exec(NULL, OLECMDID_UPDATECOMMANDS, 0, NULL, NULL);
    }
}

void
HippoToolbarAction::doShareLink(IHippoUI        *ui, 
                                const HippoBSTR &url, 
                                const HippoBSTR &title) 
{
    // In theory, we should restrict setting the foreground to the HippoUI process, 
    // the chance of some other app getting in the way is small
    AllowSetForegroundWindow(ASFW_ANY);
    ui->ShareLink(url.m_str, title.m_str);
}