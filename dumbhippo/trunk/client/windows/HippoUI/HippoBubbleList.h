/* HippoBubbleList.h: Window with a list of notification bubbles
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoDataCache.h"
#include "HippoAbstractWindow.h"
#include "HippoIE.h"
#include "HippoMySpace.h"

struct HippoPost;

class HippoBubbleList :
    public IHippoBubbleList,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoBubbleList();
    ~HippoBubbleList();

    void addLinkShare(const HippoPost &share);
    void addMySpaceCommentNotification(long myId, long blogId, const HippoMySpaceBlogComment &comment);
    void clear();

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

    // IHippoBubbleList
    STDMETHODIMP DisplaySharedLink(BSTR linkId, BSTR url);
    STDMETHODIMP GetServerBaseUrl(BSTR *ret);
    STDMETHODIMP Resize(int width, int height);

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();
    virtual void initializeBrowser();

    virtual void onClose(bool fromScript);

private:
    void addEntity(const HippoBSTR &id);
    void addEntity(const HippoEntity &entity);

    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    DWORD refCount_;
    int desiredWidth_;
    int desiredHeight_;

    void moveResizeWindow();
};
