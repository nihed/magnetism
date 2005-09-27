#pragma once

#include <ole2.h>
#include <assert.h>
#include <strsafe.h>
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
	if (raw_)
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
	if (unknown)
	    unknown->QueryInterface(*piid, (LPVOID *)&raw_);
    }
};

// Very simple version of CComBSTR

class HippoBSTR
{
public:
    HippoBSTR() : m_str(0) {
    }

    HippoBSTR(const OLECHAR *str) 
	: m_str(::SysAllocString(str)) {
    }
    
    ~HippoBSTR() {
	if (m_str)
	    ::SysFreeString(m_str);
    }

    HRESULT Append(const OLECHAR *str) {
	UINT oldlen = SysStringLen(m_str);
	UINT appendlen = wcslen(str);
	
	if (oldlen + appendlen >= oldlen && // check for overflow
	    ::SysReAllocStringLen(&m_str, m_str, oldlen + appendlen)) {
	    memcpy(m_str + oldlen, str, sizeof(OLECHAR) * (appendlen + 1));
	    return S_OK;
	} else {
	    return E_OUTOFMEMORY;
	}
    }

    operator BSTR () {
	return m_str;
    }

    BSTR *operator&() {
	assert(m_str == NULL);
	return &m_str;
    }

    HippoBSTR & operator=(const OLECHAR *str) {
	// On memory failure, leaves NULL in the result
	if (m_str)
	    ::SysFreeString(m_str);
	m_str = ::SysAllocString(str);

	return *this;
    }
    
    BSTR m_str;
};

inline void hippoDebug(WCHAR *format, ...)
{
    WCHAR buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), format, vap);
    va_end (vap);
    MessageBoxW(NULL, buf, L"Hippo Debug", MB_OK);
}