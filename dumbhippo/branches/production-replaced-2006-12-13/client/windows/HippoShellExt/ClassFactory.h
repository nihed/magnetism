/* ClassFactory.h: Standard implementation of IClassFactory
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include "Globals.h"
#include <shlobj.h>

class ClassFactory 
    : public IClassFactory
{
public:
   ClassFactory(const CLSID &classID);
   ~ClassFactory();

   // IUnknown methods
   STDMETHODIMP QueryInterface(const IID &, void **);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   // IClassFactory methods
   STDMETHODIMP CreateInstance(IUnknown *, const IID &, void **);
   STDMETHODIMP LockServer(BOOL);

protected:
   DWORD refCount_;

private:
   CLSID classID_;
};
