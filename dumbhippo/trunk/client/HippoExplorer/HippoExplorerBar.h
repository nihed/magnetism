/* HippoExplorerBar.cpp: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 **/

#include <shlobj.h>

#include "Globals.h"

#ifndef HIPPO_EXPLORER_BAR_H
#define HIPPO_EXPLORER_BAR_H

#define CB_CLASS_NAME (TEXT("HippoExplorerBarSampleClass"))

/**************************************************************************

   CHippoExplorerBar class definition

**************************************************************************/

class CHippoExplorerBar : public IDeskBand, 
                  public IInputObject, 
                  public IObjectWithSite,
                  public IPersistStream
{
protected:
   DWORD m_ObjRefCount;

public:
   CHippoExplorerBar();
   ~CHippoExplorerBar();

   //IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, LPVOID*);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   //IOleWindow methods
   STDMETHOD (GetWindow) (HWND*);
   STDMETHOD (ContextSensitiveHelp) (BOOL);

   //IDockingWindow methods
   STDMETHOD (ShowDW) (BOOL fShow);
   STDMETHOD (CloseDW) (DWORD dwReserved);
   STDMETHOD (ResizeBorderDW) (LPCRECT prcBorder, IUnknown* punkToolbarSite, BOOL fReserved);

   //IDeskBand methods
   STDMETHOD (GetBandInfo) (DWORD, DWORD, DESKBANDINFO*);

   //IInputObject methods
   STDMETHOD (UIActivateIO) (BOOL, LPMSG);
   STDMETHOD (HasFocusIO) (void);
   STDMETHOD (TranslateAcceleratorIO) (LPMSG);

   //IObjectWithSite methods
   STDMETHOD (SetSite) (IUnknown*);
   STDMETHOD (GetSite) (REFIID, LPVOID*);

   //IPersistStream methods
   STDMETHOD (GetClassID) (LPCLSID);
   STDMETHOD (IsDirty) (void);
   STDMETHOD (Load) (LPSTREAM);
   STDMETHOD (Save) (LPSTREAM, BOOL);
   STDMETHOD (GetSizeMax) (ULARGE_INTEGER*);

private:
    BOOL m_bFocus;
    HWND m_hwndParent;
    HWND m_hWnd;
    DWORD m_dwViewMode;
    DWORD m_dwBandID;
    IInputObjectSite *m_pSite;

private:
    void FocusChange(BOOL);
    LRESULT OnKillFocus(void);
    LRESULT OnSetFocus(void);
    static LRESULT CALLBACK WndProc(HWND hWnd, UINT uMessage, WPARAM wParam, LPARAM lParam);
    LRESULT OnPaint(void);
    LRESULT OnCommand(WPARAM wParam, LPARAM lParam);
    BOOL RegisterAndCreateWindow(void);
};

#endif   // HIPPO_EXPLORER_BAR_H
