#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoHTTP.h"
#include <exdisp.h>

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
    class HippoFlickrUploadInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrUploadInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }
        void handleCompleteXML(IXMLDOMElement *doc);
        void onError();
    };

    // constants
    HippoBSTR authServiceUrl_;
    HippoBSTR baseServiceUrl_;
    HippoBSTR uploadServiceUrl_;
    HippoBSTR sharedSecret_;
    HippoBSTR apiKey_;

    enum {
        REQUIRE_FROB,
        REQUESTING_FROB,
        REQUESTING_AUTH,
        REQUIRE_TOKEN,
        REQUESTING_TOKEN,
        IDLE,
        UPLOADING
    } state_;

    HippoUI *ui_;

    HippoHTTP *frobRequest_;
    HippoHTTP *tokenRequest_;
    HippoPtr<IWebBrowser2> authBrowser_;

    HippoArray<HippoBSTR> pendingUploads_;

    HippoHTTP *activeUploadRequest_;
    void *activeUploadBuf_;
    DWORD activeUploadLen_;

    HippoArray<HippoBSTR> completedUploads_;

    HippoBSTR authFrob_;
    HippoBSTR authToken_;

    void sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                HippoArray<HippoBSTR> &sortedParamNames,
                HippoArray<HippoBSTR> &sortedParamValues);
    HippoHTTP *invokeMethod(HippoFlickrInvocation *invocation, WCHAR *methodName, ...);
    void computeAPISig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                       HippoBSTR &sigMd5);
    void appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    bool readPhoto(WCHAR *filename, void **bufRet, DWORD *lenRet);
    void getFrob();
    void getToken();
    void onUploadComplete(WCHAR *photoId);
    void setToken(WCHAR *token);
    void setFrob(WCHAR *frob);
    void enqueueUpload(BSTR filename);
    void processUploads();

    void uploadComplete(long reqPosition);
};
