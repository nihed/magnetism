/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 **/

#include "stdafx.h"
#include "HippoExplorerBar.h"
#include "Guid.h"
#include <strsafe.h>

CHippoExplorerBar::CHippoExplorerBar()
{
    m_pSite = NULL;
    
    m_hWnd = NULL;
    m_hwndParent = NULL;
    
    m_bFocus = FALSE;
    
    m_dwViewMode = 0;
    m_dwBandID = 0;
    
    m_ObjRefCount = 1;
    g_DllRefCount++;
}

CHippoExplorerBar::~CHippoExplorerBar()
{
    // This should have been freed in a call to SetSite(NULL), but 
    // it is defined here to be safe.
    if(m_pSite)
    {
        m_pSite->Release();
        m_pSite = NULL;
    }
    
    g_DllRefCount--;
}

/* IUnknown Implementation */

STDMETHODIMP 
CHippoExplorerBar::QueryInterface(REFIID riid, LPVOID *ppReturn)
{
    *ppReturn = NULL;
    
    //IUnknown
    if(IsEqualIID(riid, IID_IUnknown))
    {
        *ppReturn = this;
    }
    
    //IOleWindow
    else if(IsEqualIID(riid, IID_IOleWindow))
    {
        *ppReturn = (IOleWindow*)this;
    }
    
    //IDockingWindow
    else if(IsEqualIID(riid, IID_IDockingWindow))
    {
        *ppReturn = (IDockingWindow*)this;
    }   
    
    //IInputObject
    else if(IsEqualIID(riid, IID_IInputObject))
    {
        *ppReturn = (IInputObject*)this;
    }   
    
    //IObjectWithSite
    else if(IsEqualIID(riid, IID_IObjectWithSite))
    {
        *ppReturn = (IObjectWithSite*)this;
    }   
    
    //IDeskBand
    else if(IsEqualIID(riid, IID_IDeskBand))
    {
        *ppReturn = (IDeskBand*)this;
    }   
    
    //IPersist
    else if(IsEqualIID(riid, IID_IPersist))
    {
        *ppReturn = (IPersist*)this;
    }   
    
    //IPersistStream
    else if(IsEqualIID(riid, IID_IPersistStream))
    {
        *ppReturn = (IPersistStream*)this;
    }   
    
    if(*ppReturn)
    {
        (*(LPUNKNOWN*)ppReturn)->AddRef();
        return S_OK;
    }
    
    return E_NOINTERFACE;
}                                             

STDMETHODIMP_(DWORD) CHippoExplorerBar::AddRef()
{
    return ++m_ObjRefCount;
}


STDMETHODIMP_(DWORD) CHippoExplorerBar::Release()
{
    if(--m_ObjRefCount == 0)
    {
        delete this;
        return 0;
    }
   
    return m_ObjRefCount;
}

/* IOleWindow Implementation */

/**************************************************************************

   CHippoExplorerBar::GetWindow()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::GetWindow(HWND *phWnd)
{
    *phWnd = m_hWnd;
    
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::ContextSensitiveHelp()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::ContextSensitiveHelp(BOOL fEnterMode)
{
    return E_NOTIMPL;
}

///////////////////////////////////////////////////////////////////////////
//
// IDockingWindow Implementation
//

/**************************************************************************

   CHippoExplorerBar::ShowDW()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::ShowDW(BOOL fShow)
{
    if(m_hWnd)
    {
        if(fShow)
        {
            //show our window
            ShowWindow(m_hWnd, SW_SHOW);
        }
        else
        {
            //hide our window
            ShowWindow(m_hWnd, SW_HIDE);
        }
    }

    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::CloseDW()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::CloseDW(DWORD dwReserved)
{
    ShowDW(FALSE);
    
    if(IsWindow(m_hWnd))
        DestroyWindow(m_hWnd);
    
    m_hWnd = NULL;
       
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::ResizeBorderDW()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::ResizeBorderDW(LPCRECT prcBorder, 
                                       IUnknown* punkSite, 
                                       BOOL fReserved)
{
    // This method is never called for Band Objects.
    return E_NOTIMPL;
}

///////////////////////////////////////////////////////////////////////////
//
// IInputObject Implementation
//

/**************************************************************************

   CHippoExplorerBar::UIActivateIO()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::UIActivateIO(BOOL fActivate, LPMSG pMsg)
{
    if(fActivate)
        SetFocus(m_hWnd);
    
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::HasFocusIO()
   
   If this window or one of its decendants has the focus, return S_OK. Return 
   S_FALSE if neither has the focus.

**************************************************************************/

STDMETHODIMP CHippoExplorerBar::HasFocusIO(void)
{
    if(m_bFocus)
        return S_OK;
    
    return S_FALSE;
}

/**************************************************************************

   CHippoExplorerBar::TranslateAcceleratorIO()
   
   If the accelerator is translated, return S_OK or S_FALSE otherwise.

**************************************************************************/

STDMETHODIMP CHippoExplorerBar::TranslateAcceleratorIO(LPMSG pMsg)
{
    return S_FALSE;
}

///////////////////////////////////////////////////////////////////////////
//
// IObjectWithSite implementations
//

/**************************************************************************

   CHippoExplorerBar::SetSite()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::SetSite(IUnknown* punkSite)
{
    // If a site is being held, release it.
    if(m_pSite)
    {
        m_pSite->Release();
        m_pSite = NULL;
    }
    
    // If punkSite is not NULL, a new site is being set.
    if(punkSite)
    {
        //Get the parent window.
        IOleWindow  *pOleWindow;
    
        m_hwndParent = NULL;
       
        if(SUCCEEDED(punkSite->QueryInterface(IID_IOleWindow, 
                                              (LPVOID*)&pOleWindow)))
        {
            pOleWindow->GetWindow(&m_hwndParent);
            pOleWindow->Release();
        }
    
        if(!m_hwndParent)
            return E_FAIL;
    
        if(!RegisterAndCreateWindow())
            return E_FAIL;
    
        //Get and keep the IInputObjectSite pointer.
        if(SUCCEEDED(punkSite->QueryInterface(IID_IInputObjectSite,
                                              (LPVOID*)&m_pSite)))
        {
            return S_OK;
        }
       
        return E_FAIL;
    }
    
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::GetSite()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::GetSite(REFIID riid, LPVOID *ppvReturn)
{
    *ppvReturn = NULL;
    
    if(m_pSite)
        return m_pSite->QueryInterface(riid, ppvReturn);
    
    return E_FAIL;
}

///////////////////////////////////////////////////////////////////////////
//
// IDeskBand implementation
//

/**************************************************************************

   CHippoExplorerBar::GetBandInfo()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::GetBandInfo(DWORD dwBandID, DWORD dwViewMode, DESKBANDINFO* pdbi)
{
    if(pdbi)
    {
        m_dwBandID = dwBandID;
        m_dwViewMode = dwViewMode;
    
        if(pdbi->dwMask & DBIM_MINSIZE)
        {
            pdbi->ptMinSize.x = 0;
            pdbi->ptMinSize.y = 30;
        }
    
        if(pdbi->dwMask & DBIM_MAXSIZE)
        {
            pdbi->ptMaxSize.x = -1;
            pdbi->ptMaxSize.y = 30;
        }
    
        if(pdbi->dwMask & DBIM_INTEGRAL)
        {
            pdbi->ptIntegral.x = 1;
            pdbi->ptIntegral.y = 1;
        }
    
        if(pdbi->dwMask & DBIM_ACTUAL)
        {
            pdbi->ptActual.x = 0;
            pdbi->ptActual.y = 0;
        }
    
        if(pdbi->dwMask & DBIM_TITLE)
        {
            StringCchCopyW(pdbi->wszTitle, 256, L"");
        }
    
        if(pdbi->dwMask & DBIM_MODEFLAGS)
        {
            pdbi->dwModeFlags = DBIMF_NORMAL;
        }
       
        if(pdbi->dwMask & DBIM_BKCOLOR)
        {
            //Use the default background color by removing this flag.
            pdbi->dwMask &= ~DBIM_BKCOLOR;
        }
    
        return S_OK;
    }
    
    return E_INVALIDARG;
}

///////////////////////////////////////////////////////////////////////////
//
// IPersistStream implementations
// 
// This is only supported to allow the desk band to be dropped on the 
// desktop and to prevent multiple instances of the desk band from showing 
// up in the shortcut menu. This desk band doesn't actually persist any data.
//

/**************************************************************************

   CHippoExplorerBar::GetClassID()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::GetClassID(LPCLSID pClassID)
{
    *pClassID = CLSID_HippoExplorerBar;
    
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::IsDirty()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::IsDirty(void)
{
    return S_FALSE;
}

/**************************************************************************

   CHippoExplorerBar::Load()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::Load(LPSTREAM pStream)
{
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::Save()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::Save(LPSTREAM pStream, BOOL fClearDirty)
{
    return S_OK;
}

/**************************************************************************

   CHippoExplorerBar::GetSizeMax()
   
**************************************************************************/

STDMETHODIMP CHippoExplorerBar::GetSizeMax(ULARGE_INTEGER *pul)
{
    return E_NOTIMPL;
}

///////////////////////////////////////////////////////////////////////////
//
// private method implementations
//

/**************************************************************************

   CHippoExplorerBar::WndProc()
   
**************************************************************************/

LRESULT CALLBACK CHippoExplorerBar::WndProc(HWND hWnd, 
                                    UINT uMessage, 
                                    WPARAM wParam, 
                                    LPARAM lParam)
{
    CHippoExplorerBar *pThis = (CHippoExplorerBar*)GetWindowLong(hWnd, GWL_USERDATA);
    
    switch(uMessage)
    {
        case WM_NCCREATE:
        {
            LPCREATESTRUCT lpcs = (LPCREATESTRUCT)lParam;
            pThis = (CHippoExplorerBar*)(lpcs->lpCreateParams);
            SetWindowLong(hWnd, GWL_USERDATA, (LONG)pThis);
    
            //set the window handle
            pThis->m_hWnd = hWnd;
        }
        break;
       
        case WM_PAINT:
            return pThis->OnPaint();
       
        case WM_COMMAND:
            return pThis->OnCommand(wParam, lParam);
       
        case WM_SETFOCUS:
            return pThis->OnSetFocus();
    
        case WM_KILLFOCUS:
            return pThis->OnKillFocus();
    }
    
    return DefWindowProc(hWnd, uMessage, wParam, lParam);
}

/**************************************************************************

   CHippoExplorerBar::OnPaint()
   
**************************************************************************/

LRESULT CHippoExplorerBar::OnPaint(void)
{
    PAINTSTRUCT ps;
    RECT        rc;
    
    BeginPaint(m_hWnd, &ps);
    
    GetClientRect(m_hWnd, &rc);
    SetTextColor(ps.hdc, RGB(0, 0, 0));
    SetBkMode(ps.hdc, TRANSPARENT);
    DrawText(ps.hdc, 
             TEXT("Hippo Explorer Band"), 
             -1, 
             &rc, 
             DT_SINGLELINE | DT_CENTER | DT_VCENTER);
    
    EndPaint(m_hWnd, &ps);
    
    return 0;
}

/**************************************************************************

   CHippoExplorerBar::OnCommand()
   
**************************************************************************/

LRESULT CHippoExplorerBar::OnCommand(WPARAM wParam, LPARAM lParam)
{
    return 0;
}

/**************************************************************************

   CHippoExplorerBar::FocusChange()
   
**************************************************************************/

void CHippoExplorerBar::FocusChange(BOOL bFocus)
{
    m_bFocus = bFocus;
    
    //inform the input object site that the focus has changed
    if(m_pSite)
    {
        m_pSite->OnFocusChangeIS((IDockingWindow*)this, bFocus);
    }
}

/**************************************************************************

   CHippoExplorerBar::OnSetFocus()
   
**************************************************************************/

LRESULT CHippoExplorerBar::OnSetFocus(void)
{
    FocusChange(TRUE);
    
    return 0;
}

/**************************************************************************

   CHippoExplorerBar::OnKillFocus()
   
**************************************************************************/

LRESULT CHippoExplorerBar::OnKillFocus(void)
{
    FocusChange(FALSE);
    
    return 0;
}

/**************************************************************************

   CHippoExplorerBar::RegisterAndCreateWindow()
   
**************************************************************************/

BOOL CHippoExplorerBar::RegisterAndCreateWindow(void)
{
    //If the window doesn't exist yet, create it now.
    if(!m_hWnd)
    {
        //Can't create a child window without a parent.
        if(!m_hwndParent)
        {
            return FALSE;
        }
    
        //If the window class has not been registered, then do so.
        WNDCLASS wc;
        if(!GetClassInfo(g_hInst, CB_CLASS_NAME, &wc))
        {
            ZeroMemory(&wc, sizeof(wc));
            wc.style          = CS_HREDRAW | CS_VREDRAW | CS_GLOBALCLASS;
            wc.lpfnWndProc    = (WNDPROC)WndProc;
            wc.cbClsExtra     = 0;
            wc.cbWndExtra     = 0;
            wc.hInstance      = g_hInst;
            wc.hIcon          = NULL;
            wc.hCursor        = LoadCursor(NULL, IDC_ARROW);
            wc.hbrBackground  = (HBRUSH)CreateSolidBrush(RGB(0, 192, 0));
            wc.lpszMenuName   = NULL;
            wc.lpszClassName  = CB_CLASS_NAME;
          
            if(!RegisterClass(&wc))
            {
                //If RegisterClass fails, CreateWindow below will fail.
            }
        }
    
        RECT  rc;
    
        GetClientRect(m_hwndParent, &rc);
    
        //Create the window. The WndProc will set m_hWnd.
        CreateWindowEx(0,
                       CB_CLASS_NAME,
                       NULL,
                       WS_CHILD | WS_CLIPSIBLINGS | WS_BORDER,
                       rc.left,
                       rc.top,
                       rc.right - rc.left,
                       rc.bottom - rc.top,
                       m_hwndParent,
                       NULL,
                       g_hInst,
                       (LPVOID)this);
          
    }
    
    return (NULL != m_hWnd);
}
