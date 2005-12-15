/* HippoFlickr.cpp: Integration with Flickr services
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <exdisp.h>
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoHTTP.h"
#include "HippoIEWindow.h"
#include "HippoExternalBrowser.h"

class HippoUI;

class HippoFlickr :
    public IDispatch,
    public IHippoFlickr
{
public:
    HippoFlickr();
    ~HippoFlickr();

    void setUI(HippoUI *ui);

    void uploadPhoto(BSTR filename);

    bool isCommitted() { return state_ > PROCESSING_UPLOADING; }

    //IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    //IDispatch methods
    STDMETHODIMP GetTypeInfoCount(UINT *);
    STDMETHODIMP GetTypeInfo(UINT, LCID, ITypeInfo **);
    STDMETHODIMP GetIDsOfNames(REFIID, LPOLESTR *, UINT, LCID, DISPID *);
    STDMETHODIMP Invoke(DISPID, REFIID, LCID, WORD, DISPPARAMS *, VARIANT *, EXCEPINFO *, UINT *);

    // IHippoFlickr methods
    STDMETHODIMP CreatePhotoset(BSTR title);

private:
    class HippoFlickrInvocation : public HippoHTTPAsyncHandler
    {
    public:
        HippoFlickrInvocation(HippoFlickr *flickr) {
            this->flickr_ = flickr;
            this->request_ = NULL;
        }
        void setRequest(HippoHTTP *http) {
            request_ = http;
        }
    protected:
        HippoFlickr *flickr_;
        HippoHTTP *request_;
        // Protected since we use "delete this" in callbacks, can't declare auto variables of this type
        ~HippoFlickrInvocation() {
            assert(request_ != NULL);
            delete request_;
        }
    };
    class HippoFlickrRESTInvocation : public HippoFlickrInvocation {
    public:
        HippoFlickrRESTInvocation(HippoFlickr *flickr) : HippoFlickrInvocation(flickr) {
        }
        void handleError(HRESULT res);
        void handleComplete(void *responseData, long responseBytes);
        void handleError(WCHAR *text);
        bool findFirstNamedChild(IXMLDOMElement *top, WCHAR *expectedName, HippoPtr<IXMLDOMElement> &eltRet);
        bool findFirstNamedChildTextValue(IXMLDOMElement *top, WCHAR *expectedName, HippoBSTR &ret);

        virtual void handleCompleteXML(IXMLDOMElement *doc) = 0;
        virtual void onError() { flickr_->state_ = HippoFlickr::State::FATAL_ERROR; delete this; }
    };
    class HippoFlickrAbstractAuthInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrAbstractAuthInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        virtual void handleAuth(WCHAR *token, WCHAR *userId) = 0;
    };
    class HippoFlickrCheckTokenInvocation : public HippoFlickrAbstractAuthInvocation {
    public:
        HippoFlickrCheckTokenInvocation(HippoFlickr *flickr) : HippoFlickrAbstractAuthInvocation(flickr) {
        }
        void handleAuth(WCHAR *token, WCHAR *userId);
        void onError();
    };
    class HippoFlickrFrobInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrFrobInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
    };
    class HippoFlickrTokenInvocation : public HippoFlickrAbstractAuthInvocation {
    public:
        HippoFlickrTokenInvocation(HippoFlickr *flickr) : HippoFlickrAbstractAuthInvocation(flickr) {
        }
        void handleAuth(WCHAR *token, WCHAR *userId);
    };
    class HippoFlickrAbstractUploadInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrAbstractUploadInvocation(HippoFlickr *flickr, IStream *uploadStream) : HippoFlickrRESTInvocation(flickr) {
            uploadStream_ = uploadStream;
        }
        ~HippoFlickrAbstractUploadInvocation();
    protected:
        IStream *uploadStream_;
    };
    class HippoFlickrUploadInvocation : public HippoFlickrAbstractUploadInvocation {
    public:
        HippoFlickrUploadInvocation(HippoFlickr *flickr, IStream *uploadStream) : HippoFlickrAbstractUploadInvocation(flickr, uploadStream) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
    };
    class HippoFlickrUploadThumbnailInvocation : public HippoFlickrAbstractUploadInvocation {
    public:
        HippoFlickrUploadThumbnailInvocation(HippoFlickr *flickr, IStream *uploadStream) : HippoFlickrAbstractUploadInvocation(flickr, uploadStream) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
    };
    class HippoFlickrTagPhotoInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrTagPhotoInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
    };
    class HippoFlickrGetInfoInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrGetInfoInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
    };

    class HippoFlickrPhoto {
    public:
        typedef enum {
            INITIAL,
            UPLOADING,
            UPLOADING_THUMBNAIL,
            REQUESTING_INFO,
            PRESHARE,
            ADDING_TAGS,
            COMPLETE
        } PhotoState;
       
        HippoFlickrPhoto(HippoFlickr *flickr, WCHAR *filename);

        PhotoState getState() { return state_; }
        void setState(PhotoState newState) { state_ = newState; }

        bool getStream(IStream **bufRet, DWORD *lenRet);
        bool getThumbnailStream(IStream **bufRet, DWORD *lenRet);
        BSTR getFilename() { return filename_; }
        BSTR getThumbnailFilename() { return thumbnailFilename_; }
        WCHAR *getThumbnailUrl() { return thumbnailUrl_; }
        void setThumbnailUrl(WCHAR *url) { thumbnailUrl_ = url; }

        BSTR getInfoXml() { return infoXml_; }
        void setInfoXml(WCHAR *xml) { infoXml_ = xml; }

        WCHAR *getFlickrId() { return flickrId_; }
        void setFlickrId(WCHAR *flickrId) { flickrId_ = flickrId; }
    private:
        PhotoState state_;
        HippoFlickr *flickr_;
        HippoBSTR filename_;
        HippoBSTR thumbnailFilename_;
        HippoBSTR flickrId_;
        HippoBSTR thumbnailUrl_;

        HippoBSTR infoXml_;

        void findImageEncoder(WCHAR *fmt, CLSID &clsId);
        bool genericGetStream(WCHAR *filename, IStream **bufRet, DWORD *lenRet);
    };

    // constants
    HippoBSTR authServiceUrl_;
    HippoBSTR baseServiceUrl_;
    HippoBSTR uploadServiceUrl_;
    HippoBSTR sharedSecret_;
    HippoBSTR apiKey_;

    typedef enum {
        UNINITIALIZED = 0,
        LOADING_STATUSDISPLAY,
        PREPARING_THUMBNAILS,
        CHECKING_TOKEN,
        REQUESTING_FROB,
        REQUESTING_AUTH, //5 
        REQUESTING_TOKEN,
        IDLE_AWAITING_SHARE,
        UPLOADING_AWAITING_SHARE,
        PROCESSING_UPLOADING,
        PROCESSING_FINALIZING, //10
        COMPLETE,
        FATAL_ERROR
    } State;
    
    State state_;

    void setState(State newState);

    HippoUI *ui_;

    class HippoFlickrIEWindowCallback : public HippoIEWindowCallback
    {
    public:
        HippoFlickrIEWindowCallback(HippoFlickr *flickr) { flickr_ = flickr; }
        virtual void onDocumentComplete();
        virtual bool onClose(HWND window);
    private:
        HippoFlickr *flickr_;
    };

    class HippoFlickrAuthBrowserCallback : public HippoExternalBrowserEvents
    {
    public:
        HippoFlickrAuthBrowserCallback(HippoFlickr *flickr) { flickr_ = flickr; }
        virtual void onDocumentComplete(HippoExternalBrowser *browser);
        virtual void onQuit(HippoExternalBrowser *browser);
    private:
        HippoFlickr *flickr_;
    };

    bool statusDisplayVisible_;
    HINSTANCE instance_;
    HippoIEWindow *shareWindow_;
    HippoFlickrIEWindowCallback *ieWindowCallback_;
    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoFlickr::HippoFlickrAuthBrowserCallback *authCb_;
    HippoPtr<HippoExternalBrowser> authBrowser_;

    HippoArray<HippoBSTR> pendingDisplay_;

    HippoArray<HippoFlickrPhoto *> pendingUploads_;

    HippoFlickrPhoto *activeUploadPhoto_;
    IStream *activeUploadStream_;
    DWORD activeUploadLen_;

    HippoArray<HippoFlickrPhoto *> completedUploads_;

    HippoFlickrPhoto *activeTaggingPhoto_;

    HippoArray<HippoFlickrPhoto *> processed_;

    HippoBSTR authFrob_;
    HippoBSTR authToken_;
    HippoBSTR userId_;

    HippoBSTR tagTitle_;

    void ensureStatusWindow();
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);

    void sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                HippoArray<HippoBSTR> &sortedParamNames,
                HippoArray<HippoBSTR> &sortedParamValues);

    void invokeMethod(HippoFlickrInvocation *invocation, WCHAR *methodName, ...);
    void computeAPISig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                       HippoBSTR &sigMd5);
    void appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    void prepareUpload(BSTR filename);
    bool readPhoto(WCHAR *filename);
    void checkToken();
    void getFrob();
    void getToken();
    void onUploadComplete(WCHAR *photoId);
    void onUploadThumbnailComplete(WCHAR *url);
    void onGetInfoComplete(IXMLDOMElement *photo);
    void setToken(WCHAR *token, WCHAR *userId);
    void setFrob(WCHAR *frob);
    void processPhotoset();

    void notifyUserId();
    void notifyPhotoAdded(HippoFlickrPhoto *photo);
    void notifyPhotoThumbnailUploaded(HippoFlickrPhoto *photo);
    void notifyPhotoUploaded(HippoFlickrPhoto *photo);
    void notifyPhotoComplete(HippoFlickrPhoto *photo);
    void enqueueUpload(BSTR filename);
    void processUploads();

    void uploadComplete(long reqPosition);

    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;
    DWORD refCount_;

	// private so they aren't used
	HippoFlickr(const HippoFlickr &other);
	HippoFlickr& operator=(const HippoFlickr &other);
};
