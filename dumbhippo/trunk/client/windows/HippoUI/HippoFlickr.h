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

class HippoUI;

class HippoFlickr :
    public IDispatch,
    public IHippoFlickr
{
public:
    HippoFlickr(void);
    ~HippoFlickr(void);

    void setUI(HippoUI *ui);

    void uploadPhoto(BSTR filename);

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
    STDMETHODIMP CreatePhotoset(BSTR title, BSTR descriptionHtml);

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
        virtual void onError() = 0;
    };
    class HippoFlickrCheckTokenInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrCheckTokenInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrFrobInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrFrobInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrTokenInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrTokenInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
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
        void onError();
    };
    class HippoFlickrUploadThumbnailInvocation : public HippoFlickrAbstractUploadInvocation {
    public:
        HippoFlickrUploadThumbnailInvocation(HippoFlickr *flickr, IStream *uploadStream) : HippoFlickrAbstractUploadInvocation(flickr, uploadStream) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrCreatePhotosetInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrCreatePhotosetInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrAddPhotoInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrAddPhotoInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };

    class HippoFlickrPhoto {
    public:
        HippoFlickrPhoto(HippoFlickr *flickr, WCHAR *filename);

        bool getStream(IStream **bufRet, DWORD *lenRet);
        bool getThumbnailStream(IStream **bufRet, DWORD *lenRet);
        BSTR getFilename() { return filename_; }
        BSTR getThumbnailFilename() { return thumbnailFilename_; }
        WCHAR *getThumbnailUrl() { return thumbnailUrl_; }
        void setThumbnailUrl(WCHAR *url) { thumbnailUrl_ = url; }

        WCHAR *getFlickrId() { return flickrId_; }
        void setFlickrId(WCHAR *flickrId) { flickrId_ = flickrId; }
    private:
        HippoFlickr *flickr_;
        HippoBSTR filename_;
        HippoBSTR thumbnailFilename_;
        HippoBSTR flickrId_;
        HippoBSTR thumbnailUrl_;

        void findImageEncoder(WCHAR *fmt, CLSID &clsId);
        bool genericGetStream(WCHAR *filename, IStream **bufRet, DWORD *lenRet);
    };

    // constants
    HippoBSTR authServiceUrl_;
    HippoBSTR baseServiceUrl_;
    HippoBSTR uploadServiceUrl_;
    HippoBSTR sharedSecret_;
    HippoBSTR apiKey_;

    enum {
        UNINITIALIZED,
        CHECKING_TOKEN,
        REQUESTING_FROB,
        REQUESTING_AUTH,
        REQUESTING_TOKEN,
        IDLE,
        UPLOADING,
        UPLOADING_THUMBNAIL,
        CREATING_PHOTOSET,
        POPULATING_PHOTOSET,
        PHOTOSET_POPULATED
    } state_;

    enum {
        STATUS_DISPLAY_INITIAL,
        STATUS_DISPLAY_AWAITING_DOCUMENT,
        STATUS_DISPLAY_DOCUMENT_LOADED
    } statusDisplayState_;

    HippoUI *ui_;

    class HippoFlickrIEWindowCallback : public HippoIEWindowCallback
    {
    public:
        HippoFlickrIEWindowCallback(HippoFlickr *flickr) { flickr_ = flickr; }
        virtual void onDocumentComplete();
    private:
        HippoFlickr *flickr_;
    };

    bool statusDisplayVisible_;
    HINSTANCE instance_;
    HippoIEWindow *shareWindow_;
    HippoFlickrIEWindowCallback *ieWindowCallback_;
    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoPtr<IWebBrowser2> authBrowser_;

    HippoArray<HippoFlickrPhoto *> pendingUploads_;

    HippoFlickrPhoto *activeUploadPhoto_;
    IStream *activeUploadStream_;
    DWORD activeUploadLen_;

    HippoArray<HippoFlickrPhoto *> completedUploads_;

    HippoFlickrPhoto *activePhotosetPhoto_;
    HippoArray<HippoFlickrPhoto *> photosetAddedPhotos_;

    HippoBSTR authFrob_;
    HippoBSTR authToken_;

    HippoBSTR photoSetId_;
    HippoBSTR photoSetUrl_;

    void ensureStatusWindow();
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);

    void sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                HippoArray<HippoBSTR> &sortedParamNames,
                HippoArray<HippoBSTR> &sortedParamValues);

    void invokeMethod(HippoFlickrInvocation *invocation, WCHAR *methodName, ...);
    void computeAPISig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                       HippoBSTR &sigMd5);
    void appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    bool readPhoto(WCHAR *filename);
    void checkToken();
    void getFrob();
    void getToken();
    void onUploadComplete(WCHAR *photoId);
    void onUploadThumbnailComplete(WCHAR *url);
    void setToken(WCHAR *token);
    void setFrob(WCHAR *frob);
    void onPhotosetCreated(WCHAR *photoId, WCHAR *photoUrl);
    void onPhotosetPopulated();
    void processPhotoset();

    void notifyPhotoAdded(HippoFlickrPhoto *photo);
    void notifyPhotoUploading(HippoFlickrPhoto *photo);
    void notifyPhotoThumbnailUploaded(HippoFlickrPhoto *photo);
    void notifyPhotoComplete(HippoFlickrPhoto *photo);
    void enqueueUpload(BSTR filename);
    void processUploads();

    void uploadComplete(long reqPosition);

    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;
    DWORD refCount_;
};
