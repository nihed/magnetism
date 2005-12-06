#pragma once

#include <exdisp.h>
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoHTTP.h"
#include "HippoRemoteWindow.h"

class HippoUI;

class HippoFlickr
{
public:
    HippoFlickr(void);
    ~HippoFlickr(void);

    void setUI(HippoUI *ui);

    void uploadPhoto(BSTR filename);

private:
    class HippoFlickrInvocation : public HippoHTTPAsyncHandler
    {
    public:
        HippoFlickrInvocation(HippoFlickr *flickr) {
            this->flickr_ = flickr;
        }
    protected:
        HippoFlickr *flickr_;
        // Protected since we use "delete this" in callbacks, can't declare auto variables of this type
        ~HippoFlickrInvocation() {
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
        ~HippoFlickrCheckTokenInvocation();
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrFrobInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrFrobInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        ~HippoFlickrFrobInvocation();
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrTokenInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrTokenInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        ~HippoFlickrTokenInvocation();
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };
    class HippoFlickrUploadInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrUploadInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        ~HippoFlickrUploadInvocation();
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };

    class HippoFlickrPhoto {
    public:
        HippoFlickrPhoto(HippoFlickr *flickr, WCHAR *filename);

        bool getStream(IStream **bufRet, DWORD *lenRet);
        BSTR getFilename() { return filename_; }
        BSTR getThumbnailFilename() { return thumbnailFilename_; }

        WCHAR *getFlickrId() { return flickrId_; }
        void setFlickrId(WCHAR *flickrId) { flickrId_ = flickrId; }
    private:
        HippoFlickr *flickr_;
        HippoBSTR filename_;
        HippoBSTR thumbnailFilename_;
        HippoBSTR flickrId_;

        void findImageEncoder(WCHAR *fmt, CLSID &clsId);
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
        UPLOADING
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
    HippoRemoteWindow *shareWindow_;
    HippoFlickrIEWindowCallback *ieWindowCallback_;
    HippoIE *ie_;
    HippoPtr<IWebBrowser2> browser_;

    HippoHTTP *frobRequest_;
    HippoHTTP *tokenRequest_;
    HippoPtr<IWebBrowser2> authBrowser_;

    HippoArray<HippoFlickrPhoto *> pendingUploads_;

    HippoHTTP *activeUploadRequest_;
    HippoFlickrPhoto *activeUploadPhoto_;
    IStream *activeUploadStream_;
    DWORD activeUploadLen_;

    HippoArray<HippoFlickrPhoto *> completedUploads_;

    HippoBSTR authFrob_;
    HippoBSTR authToken_;

    void ensureStatusWindow();
    bool invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...);

    void sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                HippoArray<HippoBSTR> &sortedParamNames,
                HippoArray<HippoBSTR> &sortedParamValues);

    HippoHTTP *invokeMethod(HippoFlickrInvocation *invocation, WCHAR *methodName, ...);
    void computeAPISig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                       HippoBSTR &sigMd5);
    void appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    bool readPhoto(WCHAR *filename);
    void checkToken();
    void getFrob();
    void getToken();
    void onUploadComplete(WCHAR *photoId);
    void setToken(WCHAR *token);
    void setFrob(WCHAR *frob);

    void notifyPhotoAdded(HippoFlickrPhoto *photo);
    void notifyPhotoUploading(HippoFlickrPhoto *photo);
    void notifyPhotoComplete(HippoFlickrPhoto *photo);
    void enqueueUpload(BSTR filename);
    void processUploads();

    void uploadComplete(long reqPosition);
};
