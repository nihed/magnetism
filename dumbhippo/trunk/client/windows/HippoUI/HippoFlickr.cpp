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
                                 uploadServiceUrl_(L"http://www.flickr.com/services/upload/"),
                                 sharedSecret_(L"a31c67baceb0761e"),
                                 apiKey_(L"0e96a6f88118ed4d866a0651e45383c1")
{
    state_ = REQUIRE_FROB;
}

HippoFlickr::~HippoFlickr(void)
{
}

void
HippoFlickr::setUI(HippoUI *ui)
{
    ui_ = ui;
}

void
HippoFlickr::sortParamArrays(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
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
HippoFlickr::computeAPISig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues,
                           HippoBSTR &sigMd5)
{
    HippoBSTR sig(sharedSecret_);
    unsigned char *utf;
    HippoArray<HippoBSTR> sortedParamNames;
    HippoArray<HippoBSTR> sortedParamValues;

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

    sigMd5 = digestStr;
}

void
HippoFlickr::appendApiSig(HippoArray<HippoBSTR> &paramNames, HippoArray<HippoBSTR> &paramValues)
{
    HippoBSTR sig;
    computeAPISig(paramNames, paramValues, sig);
    paramNames.append(HippoBSTR(L"api_sig"));
    paramValues.append(sig);
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

    appendApiSig(paramNames, paramValues);

    encodeQueryString(query, paramNames, paramValues);
    url = baseServiceUrl_;
    url.Append(query);

    HippoHTTP *http = new HippoHTTP();
    http->doGet(url, invocation);
    va_end(args);
    return http;
}

void 
HippoFlickr::HippoFlickrRESTInvocation::handleError(HRESULT result)
{
    HippoBSTR str;
    hippoHresultToString(result, str);
    handleError(str);
}

void 
HippoFlickr::HippoFlickrRESTInvocation::handleError(WCHAR *text)
{
    flickr_->ui_->debugLogW(L"HippoFlickr failure: %s", text);
    this->onError();
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
        this->handleError(L"no stat attribute in REST response");
        return;
    }
    assert(resp.vt == VT_BSTR);
    if (wcscmp (resp.bstrVal, L"ok")) {
        HippoBSTR msg(L"got error code in REST response");

        IXMLDOMNodeList *children;
        long nChildren;

        hr = top->get_childNodes(&children);
        if (SUCCEEDED(hr)) {
            children->get_length(&nChildren);
            for (long i = 0; i < nChildren; i++) {
                IXMLDOMNode *node;
                DOMNodeType nodeType;
                children->get_item(i, &node);
                node->get_nodeType(&nodeType);
                if (nodeType == NODE_ELEMENT) {
                    HippoQIPtr<IXMLDOMElement> elt(node);
                    variant_t errMsg;
                    elt->getAttribute(bstr_t(L"msg"), &errMsg);
                    if (errMsg.vt == VT_BSTR) {
                        msg.Append(L": ");
                        msg.Append(errMsg.bstrVal);
                        break;
                    }
                }
            }
        }
        this->handleError(msg);
        return;
    }

    handleCompleteXML(top);
}

bool
HippoFlickr::HippoFlickrRESTInvocation::findFirstNamedChild(IXMLDOMElement *top, WCHAR *expectedName, HippoPtr<IXMLDOMElement> &eltRet)
{
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
            BSTR name;
            elt->get_nodeName(&name);
            if (wcscmp(name, expectedName))
                continue;
            eltRet = elt;
            return TRUE;
        }
    }
    this->handleError(L"failed to find element in response");
    return FALSE;
lose:
    this->handleError(hr);
    return FALSE;
}

bool
HippoFlickr::HippoFlickrRESTInvocation::findFirstNamedChildTextValue(IXMLDOMElement *top, WCHAR *expectedName, HippoBSTR &ret)
{
    HRESULT hr;
    IXMLDOMNodeList *children;
    long nChildren;
    HippoPtr<IXMLDOMElement> child;

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
            BSTR name;
            elt->get_nodeName(&name);
            if (wcscmp(name, expectedName))
                continue;
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
            ret = textValue.bstrVal;
            return TRUE;
        }
    }
    this->handleError(L"failed to find element in response");
    return FALSE;
lose:
    this->handleError(hr);
    return FALSE;
}

void
HippoFlickr::HippoFlickrFrobInvocation::handleCompleteXML(IXMLDOMElement *doc)
{
    HippoBSTR frob;

    if (!findFirstNamedChildTextValue(doc, L"frob", frob))
        return;
    this->flickr_->setFrob(frob);
    delete this;
}

void
HippoFlickr::HippoFlickrFrobInvocation::onError()
{
    this->flickr_->state_ = REQUIRE_FROB;
    delete this;
}

void
HippoFlickr::setFrob(WCHAR *frob)
{
    HippoBSTR authURL;
    HippoBSTR authQuery;
    HippoArray<HippoBSTR> paramNames;
    HippoArray<HippoBSTR> paramValues;

    ui_->debugLogW(L"got Flickr auth frob %s", frob);

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

    delete frobRequest_;
    frobRequest_ = NULL;
}

void
HippoFlickr::getFrob()
{
    HippoFlickrFrobInvocation *frobInvocation = new HippoFlickrFrobInvocation(this);
    frobRequest_ = invokeMethod(frobInvocation, L"flickr.auth.getFrob", NULL);
    state_ = REQUESTING_FROB;
}

void
HippoFlickr::HippoFlickrTokenInvocation::handleCompleteXML(IXMLDOMElement *doc)
{
    HippoBSTR token;
    HippoPtr<IXMLDOMElement> authNode;

    if (!findFirstNamedChild(doc, L"auth", authNode))
        return;
    if (!findFirstNamedChildTextValue(authNode, L"token", token))
        return;

    this->flickr_->setToken(token);
    delete this;
}

void
HippoFlickr::HippoFlickrTokenInvocation::onError() {
    this->flickr_->state_ = REQUIRE_TOKEN;
    delete this;
}

void
HippoFlickr::setToken(WCHAR *token)
{
    delete tokenRequest_;
    tokenRequest_ = NULL;
    state_ = IDLE;
    authToken_= token;
    ui_->debugLogW(L"got Flickr auth token %s", authToken_.m_str);
    processUploads();
}

void
HippoFlickr::getToken()
{
    HippoFlickr::HippoFlickrTokenInvocation *invocation = new HippoFlickr::HippoFlickrTokenInvocation(this);
    tokenRequest_ = invokeMethod(invocation, L"flickr.auth.getToken", L"frob", authFrob_.m_str, NULL);
    state_ = REQUESTING_TOKEN;
}

void
HippoFlickr::enqueueUpload(BSTR filename)
{
    pendingUploads_.append(HippoBSTR(filename));
}

void
HippoFlickr::HippoFlickrUploadInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    HippoBSTR photoId;

    if (!findFirstNamedChildTextValue(top, L"photoid", photoId))
        return;
    
    this->flickr_->onUploadComplete(photoId);
    delete this;
}

void
HippoFlickr::onUploadComplete(WCHAR *photoId)
{
    assert(state_ == UPLOADING);
    completedUploads_.append(photoId);
    delete activeUploadRequest_;
    free(activeUploadBuf_);
    activeUploadRequest_ = NULL;

    state_ = IDLE;
    processUploads();
}

void
HippoFlickr::HippoFlickrUploadInvocation::onError() {
    this->flickr_->state_ = IDLE;
    delete this;
}

bool
HippoFlickr::readPhoto(WCHAR *filename, void **bufRet, DWORD *lenRet)
{
    HANDLE fd = CreateFile(filename, FILE_READ_DATA, 0, NULL, OPEN_EXISTING, FILE_FLAG_SEQUENTIAL_SCAN, NULL);
    if (fd == INVALID_HANDLE_VALUE) {
        ui_->logLastError(L"Couldn't open photo");
        return FALSE;
    }
    DWORD size = GetFileSize(fd, NULL);
    if (size == INVALID_FILE_SIZE) {
        ui_->logLastError(L"failed to get photo size");
        CloseHandle(fd);
        return FALSE;
    }
    void *buf = malloc(size);
    DWORD totalBytesRead = 0;
    DWORD bytesRead = 0;
    BOOL ret;
    while (totalBytesRead < size && (ret = ReadFile(fd, ((char*)buf) + totalBytesRead, size - totalBytesRead, &bytesRead, NULL))) {
        if (!ret) {
            ui_->logLastError(L"failed to read from photo");
            free(buf);
            CloseHandle(fd);
            return FALSE;
        }
        totalBytesRead += bytesRead;
        if (bytesRead == 0)
            break;
    }
    if (totalBytesRead != size) {
        ui_->debugLogW(L"short read on photo");
    }
    CloseHandle(fd);
    *bufRet = buf;
    *lenRet = totalBytesRead;
    return TRUE;
}

void 
HippoFlickr::processUploads()
{
    if (state_ == IDLE && pendingUploads_.length() > 0) {
        HippoBSTR filename = pendingUploads_[0];
        pendingUploads_.remove(0);
        void *buf;
        DWORD len;
        WCHAR *mimeType;
        HRESULT res;

        if (!readPhoto(filename, &buf, &len))
            return;
        res = FindMimeFromData(NULL, NULL, buf, len, NULL, 0, &mimeType, 0);
        if (FAILED(res)) {
            ui_->logError(L"couldn't determine mime type for photo", res);
            free(buf);
            return;
        }
        
        state_ = UPLOADING;
        HippoFlickr::HippoFlickrUploadInvocation *invocation = new HippoFlickr::HippoFlickrUploadInvocation(this);

        activeUploadRequest_ = new HippoHTTP();
        HippoBSTR apiSig;
        HippoArray<HippoBSTR> paramNames;
        HippoArray<HippoBSTR> paramValues;

        paramNames.append(HippoBSTR(L"api_key"));
        paramValues.append(HippoBSTR(apiKey_));
        paramNames.append(HippoBSTR(L"auth_token"));
        paramValues.append(HippoBSTR(authToken_));
        computeAPISig(paramNames, paramValues, apiSig);
        
        activeUploadRequest_->doMultipartFormPost(uploadServiceUrl_, invocation, 
                                                  L"api_key", FALSE, apiKey_.m_str,
                                                  L"auth_token", FALSE, authToken_.m_str,
                                                  L"api_sig", FALSE, apiSig.m_str,
                                                  L"photo", TRUE, buf, len, mimeType, filename, NULL);                        
    }
}

void 
HippoFlickr::uploadPhoto(BSTR filename)
{
    // temporary hack until we watch for when browser closes
    if (state_ == REQUESTING_AUTH)
        state_ = REQUIRE_TOKEN;

    if (state_ == REQUIRE_FROB) {
        getFrob();
    } else if (state_ == REQUIRE_TOKEN) {
        getToken();
    }
    enqueueUpload(filename);
    processUploads();
}