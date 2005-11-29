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
    };
    class HippoFlickrRESTInvocation : public HippoFlickrInvocation {
    public:
        HippoFlickrRESTInvocation(HippoFlickr *flickr) : HippoFlickrInvocation(flickr) {
        }
        void handleError(HRESULT res);
        void handleComplete(void *responseData, long responseBytes);
        virtual void handleCompleteXML(IXMLDOMElement *doc) = 0;
    };
    class HippoFlickrFrobInvocation : public HippoFlickrRESTInvocation {
    public:
        HippoFlickrFrobInvocation(HippoFlickr *flickr) : HippoFlickrRESTInvocation(flickr) {
        }    
        void handleCompleteXML(IXMLDOMElement *doc);
    };

    enum {
        UNINITIALIZED,
        REQUESTING_FROB,
        REQUESTING_AUTH,
        DISPLAYING_AUTH,
        IDLE,
        UPLOADING
    } state_;

    HippoUI *ui_;

    HippoHTTP *frobRequest_;
    HippoPtr<IWebBrowser2> authBrowser_;

    HippoArray<HippoBSTR> pendingUploads_;
    HippoArray<HippoHTTP> activeUploads_;

    HippoBSTR authServiceUrl_;
    HippoBSTR baseServiceUrl_;
    HippoBSTR sharedSecret_;
    HippoBSTR apiKey_;

    HippoBSTR authFrob_;
    HippoBSTR authToken_;

    HippoHTTP *invokeMethod(HippoFlickrInvocation *invocation, WCHAR *methodName, ...);
    void appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues);
    void getFrob();
    void setFrob(BSTR frob);
    void enqueueUpload(BSTR filename);

    void uploadComplete(long reqPosition);
};
