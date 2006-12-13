/* HippoExternal.h: object made available to Javascript from HippoIE
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

class HippoExternal :
    public IHippoExternal,
    public IDispatch
{
public:
    HippoExternal();
    ~HippoExternal();

    void setApplication(IDispatch *application);

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

    // IHippoExternal
    STDMETHODIMP get_Application(IDispatch **application);
    STDMETHODIMP DebugLog(BSTR str);
    STDMETHODIMP GetXmlHttp(IXMLHttpRequest **request);

private:
    DWORD refCount_;
    HippoPtr<IDispatch> application_;

    HippoPtr<ITypeInfo> ifaceTypeInfo_;
};
