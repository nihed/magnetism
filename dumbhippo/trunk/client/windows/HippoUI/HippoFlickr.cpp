#include "StdAfx.h"
#include ".\hippoflickr.h"
#include "HippoUI.h"
extern "C" {
#include <md5.h>
}

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include <wincrypt.h>

HippoFlickr::HippoFlickr(void) : baseServiceUrl_(L"http://www.flickr.com/services/rest/"), 
                                 authServiceUrl_(L"http://flickr.com/services/auth/"),
                                 sharedSecret_(L"a31c67baceb0761e"),
                                 apiKey_(L"0e96a6f88118ed4d866a0651e45383c1")
{
    state_ = UNINITIALIZED;
}

HippoFlickr::~HippoFlickr(void)
{
}

void
HippoFlickr::setUI(HippoUI *ui)
{
    ui_ = ui;
}

static void
sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                HippoArray<HippoBSTR> &sortedParamNames,
                HippoArray<HippoBSTR> &sortedParamValues)
{
    HippoArray<HippoBSTR> tempParamNames;
    tempParamNames.copyFrom(paramNames);
    HippoArray<HippoBSTR> tempParamValues;
    tempParamValues.copyFrom(paramValues);
    while (tempParamNames.length() > 0) {
        ULONG max = 0;
        for (ULONG i = 1; i < tempParamNames.length(); i++) {
            if (wcscmp(tempParamNames[max], tempParamNames[i]) > 0) {
                max = i;
            }
        }
        sortedParamNames.append(tempParamNames[max]);
        sortedParamValues.append(tempParamValues[max]);
        tempParamNames.remove(max);
        tempParamValues.remove(max);
    }
}

void
HippoFlickr::appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues)
{
    unsigned char *utf;
    HippoBSTR sig;
    HippoArray<HippoBSTR> sortedParamNames;
    HippoArray<HippoBSTR> sortedParamValues;

    sig = sharedSecret_;

    sortParamArrays(paramNames, paramValues, sortedParamNames, sortedParamValues);

    for (unsigned int i = 0; i < sortedParamNames.length(); i++) {
        sig.Append(sortedParamNames[i]);
        sig.Append(sortedParamValues[i]);
    }

    unsigned char digest[16];
    WCHAR digestStr[33];
    utf = (unsigned char *) g_utf16_to_utf8(sig, -1, NULL, NULL, NULL);
    MD5Context md5Ctx;
    MD5Init(&md5Ctx);
    MD5Update(&md5Ctx, utf, strlen((char*)utf));
    MD5Final(digest, &md5Ctx);
    g_free(utf);
    for (unsigned int i = 0; i < 16; i++) {
        WCHAR *digestPtr = digestStr;
        wsprintf(digestPtr+(2*i), L"%02X", digest[i]);
    }
    digestStr[sizeof(digestStr)/sizeof(digestStr[0]) - 1] = 0;

    paramNames.append(HippoBSTR(L"api_sig"));
    paramValues.append(digestStr);
}

static void
encodeQueryString(HippoBSTR &url, HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues)
{
    url = L"?";
    bool first = TRUE;

    for (unsigned int i = 0; i < paramNames.length(); i++) {
        if (i > 0)
            url.Append(L"&");
        url.Append(paramNames[i]);
        url.Append(L"=");

        WCHAR encoded[1024] = {0}; 
        DWORD len = sizeof(encoded)/sizeof(encoded[0]);

        if (!SUCCEEDED (UrlEscape(paramValues[i], encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
            return;
        url.Append(encoded);
    }
}

HippoHTTP *
HippoFlickr::invokeMethod(HippoFlickr::HippoFlickrInvocation *invocation, WCHAR *methodName, ...)
{
    va_list args;
    HippoArray<HippoBSTR> paramNames;
    HippoArray<HippoBSTR> paramValues;
    HippoBSTR query;
    HippoBSTR url;
    WCHAR *argName;

    paramNames.append(HippoBSTR(L"method"));
    paramValues.append(HippoBSTR(methodName));
    paramNames.append(HippoBSTR(L"api_key"));
    paramValues.append(HippoBSTR(apiKey_));

    va_start(args, methodName);

    while ((argName = va_arg (args, WCHAR *)) != NULL) {
        WCHAR *argValue = va_arg (args, WCHAR *);
        paramNames.append(argName);
        paramValues.append(argValue);
    }

    va_end(args);

    appendApiSig(paramNames, paramValues);

    encodeQueryString(query, paramNames, paramValues);
    url = baseServiceUrl_;
    url.Append(query);

    HippoHTTP *http = new HippoHTTP();
    http->doGet(url, invocation);
    return http;
}

void 
HippoFlickr::HippoFlickrRESTInvocation::handleError(HRESULT result)
{
    flickr_->ui_->logError(L"failed REST invocation", result);
}

void 
HippoFlickr::HippoFlickrRESTInvocation::handleComplete(void *responseData, long responseBytes) {
    HippoPtr<IXMLDOMDocument> doc;
    HRESULT hr;
    VARIANT_BOOL successful;

    hr = CoCreateInstance(CLSID_DOMDocument, NULL, CLSCTX_INPROC,
        IID_IXMLDOMDocument, (void**) &doc);
    if (FAILED(hr)) {
        this->handleError(hr);
        return;
    }
    HippoBSTR xmlStr(L"");
    xmlStr.setUTF8((char*) responseData, responseBytes);
    hr = doc->loadXML(xmlStr, &successful);
    if (FAILED(hr)) {
        this->handleError(hr);
        return;
    }
    IXMLDOMElement *top;
    hr = doc->get_documentElement(&top);
    if (FAILED(hr)) {
        this->handleError(hr);
        return;
    }
    _variant_t resp(L"");
    hr = top->getAttribute(_bstr_t(L"stat"), &resp);
    if (FAILED(hr)) {
        this->handleError(hr);
        return;
    }
    if (resp.vt == VT_NULL) {
        this->handleError(ERROR_SUCCESS);
        return;
    }
    assert(resp.vt == VT_BSTR);
    if (wcscmp (resp.bstrVal, L"ok")) {
        this->handleError(ERROR_SUCCESS);
        return;
    }

    handleCompleteXML(top);
}

void 
HippoFlickr::HippoFlickrFrobInvocation::handleCompleteXML(IXMLDOMElement *top) {
    HRESULT hr;
    IXMLDOMNodeList *children;
    long nChildren;

    hr = top->get_childNodes(&children);
    if (FAILED(hr))
        goto lose;
    children->get_length(&nChildren);
    for (long i = 0; i < nChildren; i++) {
        IXMLDOMNode *node;
        DOMNodeType nodeType;
        children->get_item(i, &node);
        node->get_nodeType(&nodeType);
        if (nodeType == NODE_ELEMENT) { // <frob>
            HippoQIPtr<IXMLDOMElement> elt(node);
            elt->normalize();
            IXMLDOMNode *text;
            hr = elt->get_firstChild(&text);
            if (FAILED(hr))
                goto lose;
            DOMNodeType textType;
            hr = text->get_nodeType(&textType);
            if (FAILED(hr))
                goto lose;
            if (textType != NODE_TEXT)
                goto lose;
            variant_t textValue;
            text->get_nodeValue(&textValue);
            assert (textValue.vt = VT_BSTR);
            this->flickr_->setFrob(textValue.bstrVal);
        }
    }

    return;
lose:
    this->handleError(hr);
}

void
HippoFlickr::setFrob(BSTR frob)
{
    HippoBSTR authURL;
    HippoBSTR authQuery;
    HippoArray<HippoBSTR> paramNames;
    HippoArray<HippoBSTR> paramValues;

    state_ = REQUESTING_AUTH;
    authFrob_ = frob;

    authURL = authServiceUrl_;
    paramNames.append(HippoBSTR(L"api_key"));
    paramValues.append(HippoBSTR(apiKey_));
    paramNames.append(HippoBSTR(L"perms"));
    paramValues.append(HippoBSTR(L"write"));
    paramNames.append(HippoBSTR(L"frob"));
    paramValues.append(HippoBSTR(authFrob_));

    appendApiSig(paramNames, paramValues);

    encodeQueryString(authQuery, paramNames, paramValues);
    authURL.Append(authQuery);

    ui_->launchBrowser(authURL, authBrowser_);
}

void
HippoFlickr::getFrob()
{
    HippoFlickrFrobInvocation *frobInvocation = new HippoFlickrFrobInvocation(this);
    frobRequest_ = invokeMethod(frobInvocation, L"flickr.auth.getFrob", NULL);
    state_ = REQUESTING_FROB;
}

void
HippoFlickr::enqueueUpload(BSTR filename)
{
    pendingUploads_.append(HippoBSTR(filename));
}

void 
HippoFlickr::uploadPhoto(BSTR filename)
{
    if (state_ == UNINITIALIZED) {
        getFrob();
    }
    if (state_ != IDLE) {
        enqueueUpload(filename);
        return;
    }
}