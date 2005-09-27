

/* this ALWAYS GENERATED file contains the definitions for the interfaces */


 /* File created by MIDL compiler version 6.00.0361 */
/* at Mon Sep 26 20:55:27 2005
 */
/* Compiler settings for .\HippoExplorer.idl:
    Oicf, W1, Zp8, env=Win32 (32b run)
    protocol : dce , ms_ext, c_ext, robust
    error checks: allocation ref bounds_check enum stub_data 
    VC __declspec() decoration level: 
         __declspec(uuid()), __declspec(selectany), __declspec(novtable)
         DECLSPEC_UUID(), MIDL_INTERFACE()
*/
//@@MIDL_FILE_HEADING(  )

#pragma warning( disable: 4049 )  /* more than 64k source lines */


/* verify that the <rpcndr.h> version is high enough to compile this file*/
#ifndef __REQUIRED_RPCNDR_H_VERSION__
#define __REQUIRED_RPCNDR_H_VERSION__ 475
#endif

#include "rpc.h"
#include "rpcndr.h"

#ifndef __RPCNDR_H_VERSION__
#error this stub requires an updated version of <rpcndr.h>
#endif // __RPCNDR_H_VERSION__


#ifndef __HippoExplorer_h_h__
#define __HippoExplorer_h_h__

#if defined(_MSC_VER) && (_MSC_VER >= 1020)
#pragma once
#endif

/* Forward Declarations */ 

#ifndef __IHippoEmbed_FWD_DEFINED__
#define __IHippoEmbed_FWD_DEFINED__
typedef interface IHippoEmbed IHippoEmbed;
#endif 	/* __IHippoEmbed_FWD_DEFINED__ */


#ifndef __IHippoEmbedEvents_FWD_DEFINED__
#define __IHippoEmbedEvents_FWD_DEFINED__
typedef interface IHippoEmbedEvents IHippoEmbedEvents;
#endif 	/* __IHippoEmbedEvents_FWD_DEFINED__ */


#ifndef __HippoEmbed_FWD_DEFINED__
#define __HippoEmbed_FWD_DEFINED__

#ifdef __cplusplus
typedef class HippoEmbed HippoEmbed;
#else
typedef struct HippoEmbed HippoEmbed;
#endif /* __cplusplus */

#endif 	/* __HippoEmbed_FWD_DEFINED__ */


/* header files for imported files */
#include "oaidl.h"
#include "ocidl.h"

#ifdef __cplusplus
extern "C"{
#endif 

void * __RPC_USER MIDL_user_allocate(size_t);
void __RPC_USER MIDL_user_free( void * ); 


#ifndef __HippoExplorer_LIBRARY_DEFINED__
#define __HippoExplorer_LIBRARY_DEFINED__

/* library HippoExplorer */
/* [version][lcid][helpstring][uuid] */ 


EXTERN_C const IID LIBID_HippoExplorer;

#ifndef __IHippoEmbed_INTERFACE_DEFINED__
#define __IHippoEmbed_INTERFACE_DEFINED__

/* interface IHippoEmbed */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IHippoEmbed;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("82344E95-33D8-44e3-A2BF-2E4A0CCC6C95")
    IHippoEmbed : public IDispatch
    {
    public:
        virtual /* [helpstring] */ HRESULT STDMETHODCALLTYPE DisplayMessage( 
            /* [in] */ BSTR message) = 0;
        
        virtual /* [helpstring] */ HRESULT STDMETHODCALLTYPE DebugDump( 
            /* [in] */ IDispatch *element) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IHippoEmbedVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IHippoEmbed * This,
            /* [in] */ REFIID riid,
            /* [iid_is][out] */ void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IHippoEmbed * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IHippoEmbed * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IHippoEmbed * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IHippoEmbed * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IHippoEmbed * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IHippoEmbed * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring] */ HRESULT ( STDMETHODCALLTYPE *DisplayMessage )( 
            IHippoEmbed * This,
            /* [in] */ BSTR message);
        
        /* [helpstring] */ HRESULT ( STDMETHODCALLTYPE *DebugDump )( 
            IHippoEmbed * This,
            /* [in] */ IDispatch *element);
        
        END_INTERFACE
    } IHippoEmbedVtbl;

    interface IHippoEmbed
    {
        CONST_VTBL struct IHippoEmbedVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IHippoEmbed_QueryInterface(This,riid,ppvObject)	\
    (This)->lpVtbl -> QueryInterface(This,riid,ppvObject)

#define IHippoEmbed_AddRef(This)	\
    (This)->lpVtbl -> AddRef(This)

#define IHippoEmbed_Release(This)	\
    (This)->lpVtbl -> Release(This)


#define IHippoEmbed_GetTypeInfoCount(This,pctinfo)	\
    (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo)

#define IHippoEmbed_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo)

#define IHippoEmbed_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)

#define IHippoEmbed_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)


#define IHippoEmbed_DisplayMessage(This,message)	\
    (This)->lpVtbl -> DisplayMessage(This,message)

#define IHippoEmbed_DebugDump(This,element)	\
    (This)->lpVtbl -> DebugDump(This,element)

#endif /* COBJMACROS */


#endif 	/* C style interface */



/* [helpstring] */ HRESULT STDMETHODCALLTYPE IHippoEmbed_DisplayMessage_Proxy( 
    IHippoEmbed * This,
    /* [in] */ BSTR message);


void __RPC_STUB IHippoEmbed_DisplayMessage_Stub(
    IRpcStubBuffer *This,
    IRpcChannelBuffer *_pRpcChannelBuffer,
    PRPC_MESSAGE _pRpcMessage,
    DWORD *_pdwStubPhase);


/* [helpstring] */ HRESULT STDMETHODCALLTYPE IHippoEmbed_DebugDump_Proxy( 
    IHippoEmbed * This,
    /* [in] */ IDispatch *element);


void __RPC_STUB IHippoEmbed_DebugDump_Stub(
    IRpcStubBuffer *This,
    IRpcChannelBuffer *_pRpcChannelBuffer,
    PRPC_MESSAGE _pRpcMessage,
    DWORD *_pdwStubPhase);



#endif 	/* __IHippoEmbed_INTERFACE_DEFINED__ */


#ifndef __IHippoEmbedEvents_INTERFACE_DEFINED__
#define __IHippoEmbedEvents_INTERFACE_DEFINED__

/* interface IHippoEmbedEvents */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IHippoEmbedEvents;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("48584F5A-B7EB-4eda-B058-42DEDB94A89E")
    IHippoEmbedEvents : public IDispatch
    {
    public:
        virtual /* [id][helpstring] */ HRESULT STDMETHODCALLTYPE LocationChanged( 
            BSTR locationURL) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IHippoEmbedEventsVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IHippoEmbedEvents * This,
            /* [in] */ REFIID riid,
            /* [iid_is][out] */ void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IHippoEmbedEvents * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IHippoEmbedEvents * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IHippoEmbedEvents * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IHippoEmbedEvents * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IHippoEmbedEvents * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IHippoEmbedEvents * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [id][helpstring] */ HRESULT ( STDMETHODCALLTYPE *LocationChanged )( 
            IHippoEmbedEvents * This,
            BSTR locationURL);
        
        END_INTERFACE
    } IHippoEmbedEventsVtbl;

    interface IHippoEmbedEvents
    {
        CONST_VTBL struct IHippoEmbedEventsVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IHippoEmbedEvents_QueryInterface(This,riid,ppvObject)	\
    (This)->lpVtbl -> QueryInterface(This,riid,ppvObject)

#define IHippoEmbedEvents_AddRef(This)	\
    (This)->lpVtbl -> AddRef(This)

#define IHippoEmbedEvents_Release(This)	\
    (This)->lpVtbl -> Release(This)


#define IHippoEmbedEvents_GetTypeInfoCount(This,pctinfo)	\
    (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo)

#define IHippoEmbedEvents_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo)

#define IHippoEmbedEvents_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)

#define IHippoEmbedEvents_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)


#define IHippoEmbedEvents_LocationChanged(This,locationURL)	\
    (This)->lpVtbl -> LocationChanged(This,locationURL)

#endif /* COBJMACROS */


#endif 	/* C style interface */



/* [id][helpstring] */ HRESULT STDMETHODCALLTYPE IHippoEmbedEvents_LocationChanged_Proxy( 
    IHippoEmbedEvents * This,
    BSTR locationURL);


void __RPC_STUB IHippoEmbedEvents_LocationChanged_Stub(
    IRpcStubBuffer *This,
    IRpcChannelBuffer *_pRpcChannelBuffer,
    PRPC_MESSAGE _pRpcMessage,
    DWORD *_pdwStubPhase);



#endif 	/* __IHippoEmbedEvents_INTERFACE_DEFINED__ */


EXTERN_C const CLSID CLSID_HippoEmbed;

#ifdef __cplusplus

class DECLSPEC_UUID("5A96BF90-0D8A-4200-A23B-1C8DABC0CC04")
HippoEmbed;
#endif
#endif /* __HippoExplorer_LIBRARY_DEFINED__ */

/* Additional Prototypes for ALL interfaces */

/* end of Additional Prototypes */

#ifdef __cplusplus
}
#endif

#endif


