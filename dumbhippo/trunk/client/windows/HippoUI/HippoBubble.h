/* HippoBubble.h: notification bubble
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <shlobj.h>
#include <mshtml.h>
#include <mshtmhst.h>
#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>

class HippoUI;
struct HippoLinkShare;
struct HippoLinkSwarm;

class HippoBubble :
    public IHippoBubble,
    public IDispatch,
    public IDocHostUIHandler,
    public IStorage,
    public IOleInPlaceFrame,
    public IOleClientSite,
    public IOleInPlaceSite,
    public IOleContainer
{
public:
    HippoBubble();
    ~HippoBubble();

    void setUI(HippoUI *ui);

    void setLinkNotification(HippoLinkShare &share);
    void setSwarmNotification(HippoLinkSwarm &swarm);
    void show(void);

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
    STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);           
    STDMETHODIMP GetTypeInfoCount (unsigned int *);
    STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                         VARIANT *, EXCEPINFO *, unsigned int *);

    // IStorage
    STDMETHODIMP CreateStream(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStream ** ppstm);
    STDMETHODIMP OpenStream(const WCHAR * pwcsName,void * reserved1,DWORD grfMode,DWORD reserved2,IStream ** ppstm);
    STDMETHODIMP CreateStorage(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStorage ** ppstg);
    STDMETHODIMP OpenStorage(const WCHAR * pwcsName,IStorage * pstgPriority,DWORD grfMode,SNB snbExclude,DWORD reserved,IStorage ** ppstg);
    STDMETHODIMP CopyTo(DWORD ciidExclude,IID const * rgiidExclude,SNB snbExclude,IStorage * pstgDest);
    STDMETHODIMP MoveElementTo(const OLECHAR * pwcsName,IStorage * pstgDest,const OLECHAR* pwcsNewName,DWORD grfFlags);
    STDMETHODIMP Commit(DWORD grfCommitFlags);
    STDMETHODIMP Revert(void);
    STDMETHODIMP EnumElements(DWORD reserved1,void * reserved2,DWORD reserved3,IEnumSTATSTG ** ppenum);
    STDMETHODIMP DestroyElement(const OLECHAR * pwcsName);
    STDMETHODIMP RenameElement(const WCHAR * pwcsOldName,const WCHAR * pwcsNewName);
    STDMETHODIMP SetElementTimes(const WCHAR * pwcsName,FILETIME const * pctime,FILETIME const * patime,FILETIME const * pmtime);
    STDMETHODIMP SetClass(REFCLSID clsid);
    STDMETHODIMP SetStateBits(DWORD grfStateBits,DWORD grfMask);
    STDMETHODIMP Stat(STATSTG * pstatstg,DWORD grfStatFlag);

    // IOleWindow
    STDMETHODIMP GetWindow(HWND FAR* lphwnd);
    STDMETHODIMP ContextSensitiveHelp(BOOL fEnterMode);
    // IOleInPlaceUIWindow
    STDMETHODIMP GetBorder(LPRECT lprectBorder);
    STDMETHODIMP RequestBorderSpace(LPCBORDERWIDTHS pborderwidths);
    STDMETHODIMP SetBorderSpace(LPCBORDERWIDTHS pborderwidths);
    STDMETHODIMP SetActiveObject(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName);
    // IOleInPlaceFrame
    STDMETHODIMP InsertMenus(HMENU hmenuShared,LPOLEMENUGROUPWIDTHS lpMenuWidths);
    STDMETHODIMP SetMenu(HMENU hmenuShared,HOLEMENU holemenu,HWND hwndActiveObject);
    STDMETHODIMP RemoveMenus(HMENU hmenuShared);
    STDMETHODIMP SetStatusText(LPCOLESTR pszStatusText);
    STDMETHODIMP TranslateAccelerator(  LPMSG lpmsg,WORD wID);

    // IOleClientSite
    STDMETHODIMP SaveObject();
    STDMETHODIMP GetMoniker(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker ** ppmk);
    STDMETHODIMP GetContainer(LPOLECONTAINER FAR* ppContainer);
    STDMETHODIMP ShowObject();
    STDMETHODIMP OnShowWindow(BOOL fShow);
    STDMETHODIMP RequestNewObjectLayout();

    // IOleInPlaceSite methods
    STDMETHODIMP CanInPlaceActivate();
    STDMETHODIMP OnInPlaceActivate();
    STDMETHODIMP OnUIActivate();
    STDMETHODIMP GetWindowContext(LPOLEINPLACEFRAME FAR* lplpFrame,LPOLEINPLACEUIWINDOW FAR* lplpDoc,LPRECT lprcPosRect,LPRECT lprcClipRect,LPOLEINPLACEFRAMEINFO lpFrameInfo);
    STDMETHODIMP Scroll(SIZE scrollExtent);
    STDMETHODIMP OnUIDeactivate(BOOL fUndoable);
    STDMETHODIMP OnInPlaceDeactivate();
    STDMETHODIMP DiscardUndoState();
    STDMETHODIMP DeactivateAndUndo();
    STDMETHODIMP OnPosRectChange(LPCRECT lprcPosRect);
    // IParseDisplayName
    STDMETHODIMP ParseDisplayName(IBindCtx *pbc,LPOLESTR pszDisplayName,ULONG *pchEaten,IMoniker **ppmkOut);
    // IOleContainer
    STDMETHODIMP EnumObjects(DWORD grfFlags,IEnumUnknown **ppenum);
    STDMETHODIMP LockContainer(BOOL fLock);
    
    // IHippoBubble
    STDMETHODIMP DebugLog(BSTR str);
    STDMETHODIMP DisplaySharedLink(BSTR linkId);
    STDMETHODIMP OpenExternalURL(BSTR url);
    STDMETHODIMP GetXmlHttp(IXMLHttpRequest **request);
    STDMETHODIMP Close();

    // IDocHostUIHandler
    STDMETHODIMP EnableModeless(BOOL enable);
    STDMETHODIMP FilterDataObject(IDataObject *dobj, IDataObject **dobjRet);
    STDMETHODIMP GetDropTarget(IDropTarget *dropTarget, IDropTarget **dropTargetRet);
    STDMETHODIMP GetExternal(IDispatch **dispatch);
    STDMETHODIMP GetHostInfo(DOCHOSTUIINFO *info);
    STDMETHODIMP GetOptionKeyPath(LPOLESTR *chKey, DWORD dw);
    STDMETHODIMP HideUI(VOID);
    STDMETHODIMP OnDocWindowActivate(BOOL activate);
    STDMETHODIMP OnFrameWindowActivate(BOOL activate);
    STDMETHODIMP ResizeBorder(LPCRECT border, IOleInPlaceUIWindow *uiWindow, BOOL frameWindow);
    STDMETHODIMP ShowContextMenu(DWORD id, POINT *pt, IUnknown *cmdtReserved, IDispatch *dispReserved);
    STDMETHODIMP ShowUI(DWORD id, IOleInPlaceActiveObject *activeObject, IOleCommandTarget *commandTarget, IOleInPlaceFrame *frame, IOleInPlaceUIWindow *doc);
    STDMETHODIMP TranslateAccelerator(LPMSG msg, const GUID *guidCmdGroup, DWORD cmdID);
    STDMETHODIMP TranslateUrl(DWORD translate, OLECHAR *chURLIn, OLECHAR **chURLOut);
    STDMETHODIMP UpdateUI(VOID);

private:
    HINSTANCE instance_;
    HWND window_;

    IOleObject* ie_;

    HippoUI* ui_;

    HippoBSTR currentLink_;
    HippoBSTR currentLinkId_;
    HippoBSTR currentSenderUrl_;

    bool embedIE(void);
    bool appendTransform(BSTR src, BSTR style, ...);
    bool invokeJavascript(BSTR funcName, VARIANT *invokeResult, int nargs, ...);
    bool create(void);
    bool createWindow(void);
    bool registerClass();

    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);
    DWORD refCount_;
};
