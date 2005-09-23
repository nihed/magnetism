/* HippoExplorerBar.h: Horizontal explorer bar
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <shlobj.h>
#include <HippoUtil.h>

class HippoExplorerBar : public IDeskBand, 
                         public IInputObject, 
                         public IObjectWithSite,
                         public IPersistStream
{
public:
   HippoExplorerBar();
   ~HippoExplorerBar();

   //IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, void **);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   //IOleWindow methods
   STDMETHODIMP GetWindow(HWND *);
   STDMETHODIMP ContextSensitiveHelp(BOOL);

   //IDockingWindow methods
   STDMETHODIMP ShowDW(BOOL);
   STDMETHODIMP CloseDW(DWORD);
   STDMETHODIMP ResizeBorderDW(const RECT *, IUnknown *, BOOL);

   //IDeskBand methods
   STDMETHODIMP GetBandInfo(DWORD, DWORD, DESKBANDINFO*);

   //IInputObject methods
   STDMETHODIMP UIActivateIO(BOOL, MSG *);
   STDMETHODIMP HasFocusIO();
   STDMETHODIMP TranslateAcceleratorIO(MSG *);

   //IObjectWithSite methods
   STDMETHODIMP SetSite(IUnknown *);
   STDMETHODIMP GetSite(const IID &, void **);

   //IPersistStream methods
   STDMETHODIMP GetClassID(CLSID *);
   STDMETHODIMP IsDirty();
   STDMETHODIMP Load(IStream *);
   STDMETHODIMP Save(IStream *, BOOL);
   STDMETHODIMP GetSizeMax(ULARGE_INTEGER *);

protected:
    DWORD refCount_;

private:
    HippoPtr<IInputObjectSite> site_;
    HWND window_;
    bool hasFocus_;

private:
    bool createWindow(HWND parentWindow);
    bool registerWindowClass();
    bool processMessage(UINT   message, 
	                WPARAM wParam,
		        LPARAM lParam);
    void setHasFocus(bool hasFocus);
    void onPaint();
    static LRESULT CALLBACK windowProc(HWND   window,
		  	               UINT   message,
			               WPARAM wParam,
			               LPARAM lParam);
};
