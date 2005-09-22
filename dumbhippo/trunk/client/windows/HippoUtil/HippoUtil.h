#pragma once

#include <ole2.h>
#include <assert.h>
#include "HippoUtil_h.h"

#define HIPPO_DEFINE_REFCOUNTING(C) \
   STDMETHODIMP_(DWORD)             \
   C::AddRef()                      \
   {                                \
       return ++refCount_;          \
   }                                \
                                    \
   STDMETHODIMP_(DWORD)             \
   C::Release()                     \
   {                                \
       refCount_--;                 \
                                    \
        if (refCount_ != 0)         \
	    return refCount_;       \
                                    \
        delete this;                \
        return 0;                   \
    }

/*
 * Define a very simple set of smart pointers modelled after the ATL CComPtr, CComQIPtr
 *
 * We do this to avoid dragging in ATL; if we later decide that we actually wanted
 * to use ATL, a simple search-and-replace should fix things up.
 */
template<class T>
class HippoPtr
{
public:
    HippoPtr() : raw_(0) {
    }
    HippoPtr(T *t) : raw_(t) {
	raw_->AddRef();
    }
    ~HippoPtr() {
	if (raw_) {
	    raw_->Release();
	}
    }
    operator T *(){
	return raw_;
    }
    T* operator->() {
	return raw_;
    }
    T **operator&() {
	assert(raw_ == NULL);
	return &raw_;
    }
    void operator=(T *t) {
	if (raw_)
	    raw_->Release();
	raw_ = t;
	if (raw_)
	    raw_->AddRef();
    }

protected:
    T *raw_;
};

template<class T, const IID *piid = &__uuidof(T)>
class HippoQIPtr : public HippoPtr<T> 
{
public:
    HippoQIPtr(IUnknown *unknown) : HippoPtr<T>() {
	unknown->QueryInterface(*piid, (LPVOID *)&raw_);
    }
};
