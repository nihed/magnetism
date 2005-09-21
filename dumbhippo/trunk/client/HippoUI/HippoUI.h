/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "resource.h"
#include <HippoUtil.h>

class CHippoUI 
    : public IHippoUI 
{
public:
    CHippoUI();
    ~CHippoUI();

    bool registerActive();
    void revokeActive();

    //IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    //IDispatch methods
    STDMETHODIMP GetTypeInfoCount(UINT *);
    STDMETHODIMP GetTypeInfo(UINT, LCID, ITypeInfo **);
    STDMETHODIMP GetIDsOfNames(REFIID, LPOLESTR *, UINT, LCID, DISPID *);
    STDMETHODIMP Invoke(DISPID, REFIID, LCID, WORD, DISPPARAMS *, VARIANT *, EXCEPINFO *, UINT *);

    //IHippoUI methods
    STDMETHODIMP Log(BSTR message);

protected:
    DWORD objRefCount_;

private:    
    ITypeInfo *uiTypeInfo_;     // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;      // Handle from RegisterActiveObject
};