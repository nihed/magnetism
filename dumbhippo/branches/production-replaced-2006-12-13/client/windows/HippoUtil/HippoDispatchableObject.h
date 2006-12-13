/* HippoDispatchableObject.h: Templatized base class implementing IUnknown and IDispatch
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

/**
 * HippoDispatchableObject is a base class you can derive from when you have a 
 * class implenting a single COM interface and you want it to be dynamically
 * accessible via IDispatch. The class is doubly templatized: on the 
 * interface that your are implementing and on the derived class itself. The former
 * is necessary in order that we can properly implement QueryInterface(),
 * and also prevents diamond-shaped inheritance from IDispatch if the interface
 * itself inherits from IDispatch. The latter is used to primarily to access
 * a static member function T::getTypeInfo() which returns a pointer to 
 * ITypeInfo for the implemented interface. (Note that this function should 
 * *not* call AddRef on its result.)
 *
 * So, a class using this base class looks like:
 * 
 *   class MyClass : public HippoDispatchInterface<ITheInterface, MyClass> {
 *       static ITypeInfo *getTypeInfo();
 *   }
 * 
 * You cannot use this base class if you also need to implement other interfaces,
 * such as IConnectionPointContainer, since your class then would multiply
 * inherit IUnknown. It should be possible to split out the IDispatch part of 
 * this class from the IUnknown implementation and use it in such cases, as long
 * as the derived class only has one copy of IDispatch in its inheritance 
 * hierarchy; this class is meant to make the common single-interface case easy,
 * but there could be a separate HippoDispatchable, say, that does that.
 */
template <class I, class T>
class HippoDispatchableObject : public I, virtual public IDispatch {
public:
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

protected:
    HippoDispatchableObject();
    virtual ~HippoDispatchableObject();

private:
    DWORD refCount_;
};

template <class I, class T>
STDMETHODIMP 
HippoDispatchableObject<I,T>::QueryInterface(const IID &ifaceID, 
                                             void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<I *>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, __uuidof(I)))
        *result = static_cast<I *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

template <class I, class T>
STDMETHODIMP_(DWORD)
HippoDispatchableObject<I,T>::AddRef()
{
    return ++refCount_;
}
                                    \
template <class I, class T>
STDMETHODIMP_(DWORD)
HippoDispatchableObject<I,T>::Release()
{
    refCount_--;

    if (refCount_ != 0)
        return refCount_;

    delete this;
    return 0;
}

template <class I, class T>
STDMETHODIMP
HippoDispatchableObject<I,T>::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

template <class I, class T>
STDMETHODIMP 
HippoDispatchableObject<I,T>::GetTypeInfo(UINT        iTInfo,
                                          LCID        lcid,
                                          ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ITypeInfo *typeInfo = T::getTypeInfo();
    if (!typeInfo)
        return E_OUTOFMEMORY;

    typeInfo->AddRef();
    *ppTInfo = typeInfo;

    return S_OK;
}
        
template <class I, class T>
STDMETHODIMP 
HippoDispatchableObject<I,T>::GetIDsOfNames (REFIID    riid,
                                             LPOLESTR *rgszNames,
                                             UINT      cNames,
                                             LCID      lcid,
                                             DISPID   *rgDispId)
 {
    ITypeInfo *typeInfo = T::getTypeInfo();
    if (!typeInfo)
        return E_OUTOFMEMORY;
    
    return DispGetIDsOfNames(typeInfo, rgszNames, cNames, rgDispId);
 }
        
template <class I, class T>
STDMETHODIMP
HippoDispatchableObject<I,T>::Invoke (DISPID        member,
                                      const IID    &iid,
                                      LCID          lcid,              
                                      WORD          flags,
                                      DISPPARAMS   *dispParams,
                                      VARIANT      *result,
                                      EXCEPINFO    *excepInfo,  
                                      unsigned int *argErr)
{
    ITypeInfo *typeInfo = T::getTypeInfo();
    if (!typeInfo)
        return E_OUTOFMEMORY;
    
    HippoQIPtr<I> obj(static_cast<I *>(this));
    HRESULT hr = DispInvoke(static_cast<I *>(static_cast<T *>(this)), typeInfo, member, flags, 
                            dispParams, result, excepInfo, argErr);

    return hr;
}

template <class I, class T>
HippoDispatchableObject<I,T>::HippoDispatchableObject()
{
    refCount_ = 1;
}

template <class I, class T>
HippoDispatchableObject<I,T>::~HippoDispatchableObject()
{
}
