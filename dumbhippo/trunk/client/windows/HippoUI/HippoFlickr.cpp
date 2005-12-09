/* HippoFlickr.cpp: Integration with Flickr services
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "StdAfx.h"
#include "HippoFlickr.h"
#include "HippoUI.h"
#include <HippoRegKey.h>
#include "HippoIEWindow.h"
#include "HippoUIUtil.h"
#include "Guid.h"
extern "C" {
#include <md5.h>
}
#include <gdiplus.h>

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include <wincrypt.h>

static const WCHAR *DUMBHIPPO_SUBKEY_FLICKR = L"Software\\DumbHippo\\Flickr";

HippoFlickr::HippoFlickr(void) : baseServiceUrl_(L"http://www.flickr.com/services/rest/"), 
                                 authServiceUrl_(L"http://flickr.com/services/auth/"),
                                 uploadServiceUrl_(L"http://www.flickr.com/services/upload/"),
                                 sharedSecret_(L"a31c67baceb0761e"),
                                 apiKey_(L"0e96a6f88118ed4d866a0651e45383c1")
{
    state_ = UNINITIALIZED;
    statusDisplayState_ = STATUS_DISPLAY_INITIAL;
    statusDisplayVisible_ = false;
    activeUploadPhoto_ = NULL;

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoUtil, 
                                0, 1, /* Version */
                                0,    /* LCID */
                                &typeLib);
    if (SUCCEEDED (hr)) {
        typeLib->GetTypeInfoOfGuid(IID_IHippoFlickr, &ifaceTypeInfo_);
        typeLib->GetTypeInfoOfGuid(CLSID_HippoFlickr, &classTypeInfo_);
    } else
        hippoDebug(L"Failed to load type lib: %x\n", hr);
}

HippoFlickr::~HippoFlickr(void)
{
}

void
HippoFlickr::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoFlickr::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list args;
    va_start (args, nargs);
    ui_->debugLogW(L"invoking javascript method %s", funcName);
    HRESULT result = ie_->invokeJavascript(funcName, invokeResult, nargs, args);
    bool ret = SUCCEEDED(result);
    if (!ret)
        ui_->logError(L"failed to invoke javascript", result);
    va_end (args);
    return ret;
    return true;
}

void
HippoFlickr::HippoFlickrIEWindowCallback::onDocumentComplete() 
{
    // We need to wait until the link share display is fully loaded before invoking
    // javascript
    if (flickr_->statusDisplayState_ == HippoFlickr::STATUS_DISPLAY_AWAITING_DOCUMENT) {
        flickr_->ui_->debugLogW(L"got flickr document complete");
        flickr_->statusDisplayState_ = HippoFlickr::STATUS_DISPLAY_DOCUMENT_LOADED;
        for (UINT i = 0; i < flickr_->pendingUploads_.length(); i++) {
            flickr_->notifyPhotoAdded(flickr_->pendingUploads_[i]);
        }
        if (flickr_->activeUploadPhoto_ != NULL) {
            flickr_->notifyPhotoAdded(flickr_->activeUploadPhoto_);
            flickr_->notifyPhotoUploading(flickr_->activeUploadPhoto_);
            WCHAR *thumbnailUrl = flickr_->activeUploadPhoto_->getThumbnailUrl();
            if (thumbnailUrl)
                flickr_->notifyPhotoThumbnailUploaded(flickr_->activeUploadPhoto_);
        }
        for (UINT i = 0; i < flickr_->completedUploads_.length(); i++) {
            HippoFlickrPhoto *photo = flickr_->completedUploads_[i];
            flickr_->notifyPhotoAdded(photo);
            flickr_->notifyPhotoUploading(photo);
            flickr_->notifyPhotoComplete(photo);
            flickr_->notifyPhotoThumbnailUploaded(photo);
        }
    }
}

void
HippoFlickr::ensureStatusWindow()
{
    if (statusDisplayState_ > STATUS_DISPLAY_INITIAL)
        return;
    statusDisplayState_ = STATUS_DISPLAY_AWAITING_DOCUMENT;
    ui_->debugLogW(L"creating Flickr share window");
    ieWindowCallback_ = new HippoFlickrIEWindowCallback(this);

    HippoBSTR shareURL;
    if (!SUCCEEDED (ui_->getRemoteURL(HippoBSTR(L"sharephotoset"), &shareURL))) {
        ui_->debugLogW(L"out of memory");
        return;
    }
    shareWindow_ = new HippoIEWindow(ui_, L"Share Photos", 600, 600, shareURL, this, ieWindowCallback_);
    shareWindow_->show();
    ie_ = shareWindow_->getIE();
    statusDisplayVisible_ = true;
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

void
HippoFlickr::invokeMethod(HippoFlickr::HippoFlickrInvocation *invocation, WCHAR *methodName, ...)
{
    va_list args;
    HippoArray<HippoBSTR> paramNames;
    HippoArray<HippoBSTR> paramValues;
    HippoBSTR query;
    HippoBSTR url;
    WCHAR *argName;

    ui_->debugLogW(L"async invoking Flickr method %s", methodName);

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

    HippoUIUtil::encodeQueryString(query, paramNames, paramValues);
    url = baseServiceUrl_;
    url.Append(query);

    HippoHTTP *http = new HippoHTTP();
    http->doGet(url, invocation);
    va_end(args);
    invocation->setRequest(http);
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
    if (flickr_->statusDisplayState_ == HippoFlickr::STATUS_DISPLAY_DOCUMENT_LOADED) {
        variant_t vResult;
        HippoBSTR textStr(text);
        variant_t vText(textStr.m_str);
        flickr_->invokeJavascript(L"dhFlickrError", &vResult, 1, &vText);
    }
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
    if (top == NULL) {
        this->flickr_->ui_->debugLogW(L"xml: %s", xmlStr.m_str);
        this->handleError(L"couldn't find document element");
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
HippoFlickr::HippoFlickrCheckTokenInvocation::handleCompleteXML(IXMLDOMElement *doc)
{
    HippoBSTR token;
    HippoPtr<IXMLDOMElement> authNode;

    if (!findFirstNamedChild(doc, L"auth", authNode))
        return;
    if (!findFirstNamedChildTextValue(authNode, L"token", token))
        return;

    this->flickr_->state_ = IDLE;
    this->flickr_->authToken_ = token;
    this->flickr_->processUploads();
    delete this;
}

void
HippoFlickr::HippoFlickrCheckTokenInvocation::onError()
{
    this->flickr_->getFrob();
    delete this;
}

void 
HippoFlickr::checkToken()
{
    HippoRegKey hippoFlickrReg(HKEY_CURRENT_USER, 
                               DUMBHIPPO_SUBKEY_FLICKR,
                               false);
    HippoBSTR token;
    if (hippoFlickrReg.loadString(L"token", &token)) {
        HippoFlickr::HippoFlickrCheckTokenInvocation *invocation = new HippoFlickr::HippoFlickrCheckTokenInvocation(this);
        invokeMethod(invocation, L"flickr.auth.checkToken", L"auth_token", token.m_str, NULL);
        state_ = CHECKING_TOKEN;
    } else {
        getFrob();
    }
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
    this->flickr_->state_ = UNINITIALIZED;
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

    HippoUIUtil::encodeQueryString(authQuery, paramNames, paramValues);
    authURL.Append(authQuery);

    ui_->launchBrowser(authURL, authBrowser_);
}

void
HippoFlickr::getFrob()
{
    HippoFlickrFrobInvocation *frobInvocation = new HippoFlickrFrobInvocation(this);
    invokeMethod(frobInvocation, L"flickr.auth.getFrob", NULL);
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
    this->flickr_->state_ = UNINITIALIZED;
    delete this;
}

void
HippoFlickr::setToken(WCHAR *token)
{
    state_ = IDLE;
    authToken_= token;
    ui_->debugLogW(L"got Flickr auth token %s", authToken_.m_str);
    HippoRegKey hippoFlickrReg(HKEY_CURRENT_USER, 
                               DUMBHIPPO_SUBKEY_FLICKR,
                               true);
    hippoFlickrReg.saveString(L"token", authToken_);

    processUploads();
}

void
HippoFlickr::getToken()
{
    HippoFlickr::HippoFlickrTokenInvocation *invocation = new HippoFlickr::HippoFlickrTokenInvocation(this);
    invokeMethod(invocation, L"flickr.auth.getToken", L"frob", authFrob_.m_str, NULL);
    state_ = REQUESTING_TOKEN;
}

void
HippoFlickr::notifyPhotoAdded(HippoFlickrPhoto *photo)
{
    if (statusDisplayState_ < STATUS_DISPLAY_DOCUMENT_LOADED)
        return;
    VARIANT vResult;
    variant_t vFilename(photo->getFilename());
    variant_t vThumbnailFilename(photo->getThumbnailFilename());
    invokeJavascript(L"dhFlickrAddPhoto", &vResult, 2, &vFilename, &vThumbnailFilename);
}

void
HippoFlickr::enqueueUpload(BSTR filename)
{
    HippoFlickrPhoto *photo = new HippoFlickrPhoto(this, filename);
    pendingUploads_.append(photo);
    if (statusDisplayState_ == STATUS_DISPLAY_DOCUMENT_LOADED) {
        notifyPhotoAdded(photo);
    }
}

void
HippoFlickr::notifyPhotoComplete(HippoFlickrPhoto *photo)
{
    if (statusDisplayState_ < STATUS_DISPLAY_DOCUMENT_LOADED)
        return;
    variant_t result;
    variant_t vFilename(photo->getFilename());
    variant_t vPhotoId(HippoBSTR(photo->getFlickrId()));
    invokeJavascript(L"dhFlickrPhotoUploadComplete", &result, 2, &vFilename, &vPhotoId);
}

void
HippoFlickr::onUploadComplete(WCHAR *photoId)
{
    assert(state_ == UPLOADING);
    activeUploadPhoto_->setFlickrId(photoId);
    notifyPhotoComplete(activeUploadPhoto_);

    HGLOBAL hg = NULL;
    void *buf;
    DWORD len;
    IStream *uploadStream;
    if (!activeUploadPhoto_->getThumbnailStream(&uploadStream, &len))
        return;

    GetHGlobalFromStream(uploadStream, &hg);
    buf = GlobalLock(hg);
    state_ = UPLOADING_THUMBNAIL;
    HippoFlickr::HippoFlickrUploadThumbnailInvocation *invocation 
        = new HippoFlickr::HippoFlickrUploadThumbnailInvocation(this, uploadStream);

    HippoBSTR postThumbnailUrl;
    ui_->getRemoteURL(L"upload/postinfo", &postThumbnailUrl);

    HippoHTTP *request = new HippoHTTP();
    invocation->setRequest(request); // takes ownership
    request->doMultipartFormPost(postThumbnailUrl, invocation, 
        L"thumbnail", TRUE, buf, len, L"image/png", activeUploadPhoto_->getFilename(), NULL);
}

void
HippoFlickr::notifyPhotoThumbnailUploaded(HippoFlickrPhoto *photo)
{
    variant_t vResult;
    variant_t vFilename(photo->getFilename());
    variant_t vThumbnailUrl(photo->getThumbnailUrl());
    invokeJavascript(L"dhFlickrPhotoThumbnailUploadComplete", &vResult, 2, &vFilename, &vThumbnailUrl);
}

void
HippoFlickr::onUploadThumbnailComplete(WCHAR *url)
{
    activeUploadPhoto_->setThumbnailUrl(url);
    if (statusDisplayState_ == STATUS_DISPLAY_DOCUMENT_LOADED)
        notifyPhotoThumbnailUploaded(activeUploadPhoto_);
    completedUploads_.append(activeUploadPhoto_);
    state_ = IDLE;
    activeUploadPhoto_ = NULL;
    processUploads();
}

HippoFlickr::HippoFlickrAbstractUploadInvocation::~HippoFlickrAbstractUploadInvocation()
{
    HGLOBAL hg = NULL;
    GetHGlobalFromStream(uploadStream_, &hg);
    GlobalUnlock(hg); // locked for upload
    uploadStream_->Release();
    uploadStream_ = NULL;
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
HippoFlickr::HippoFlickrUploadInvocation::onError() {
    this->flickr_->state_ = IDLE;
    delete this;
}

void
HippoFlickr::HippoFlickrUploadThumbnailInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    HippoBSTR url;
    if (!findFirstNamedChildTextValue(top, L"url", url))
        return;
    this->flickr_->onUploadThumbnailComplete(url);
    delete this;
}

void
HippoFlickr::HippoFlickrUploadThumbnailInvocation::onError() {
    this->flickr_->state_ = IDLE;
    delete this;
}

HippoFlickr::HippoFlickrPhoto::HippoFlickrPhoto(HippoFlickr *flickr, WCHAR *filename)
{
    flickr_ = flickr;
    filename_ = filename;

    Gdiplus::Image img(filename);
    Gdiplus::Status st = img.GetLastStatus();
    if (st == Gdiplus::Ok) {
        Gdiplus::Image* thumbnail = img.GetThumbnailImage(100, 100, NULL, NULL);
        CLSID pngClsid;
        findImageEncoder(L"image/png", pngClsid);
        WCHAR tempPath[MAX_PATH];
        WCHAR tempFilenameBuf[MAX_PATH];
        GetTempPath(sizeof(tempPath)/sizeof(tempPath[0]), tempPath);
        GetTempFileName(tempPath, L"dhThumbnail", 0, tempFilenameBuf);
        HippoBSTR tempFilename(tempFilenameBuf);
        tempFilename.Append(L".png");
        thumbnail->Save(tempFilename.m_str, &pngClsid);
        if ((st = thumbnail->GetLastStatus()) != Gdiplus::Ok) {
            flickr_->ui_->debugLogW(L"failed to save png thumbnail to %s", tempFilename.m_str);
            delete thumbnail;
            return;
        }
        delete thumbnail;
        thumbnailFilename_ = tempFilename;
    } else {
        flickr_->ui_->debugLogW(L"failed to read photo thumbnail");
    }
}

void
HippoFlickr::HippoFlickrPhoto::findImageEncoder(WCHAR *fmt, CLSID &clsId)
{
    UINT nEncoders;
    UINT nEncodersBytes;
    Gdiplus::ImageCodecInfo *info;

    Gdiplus::GetImageEncodersSize(&nEncoders, &nEncodersBytes);
    info = (Gdiplus::ImageCodecInfo*) malloc(nEncodersBytes);
    Gdiplus::GetImageEncoders(nEncoders, nEncodersBytes, info);
    for (UINT i = 0; i < nEncoders; i++) {
        if(wcscmp(info[i].MimeType, fmt) == 0){ 
         clsId = info[i].Clsid;
         break;
      }
    }
    free(info);
}

bool
HippoFlickr::HippoFlickrPhoto::genericGetStream(WCHAR *filename, IStream **bufRet, ULONG *lenRet)
{
    HANDLE fd = CreateFile(filename, FILE_READ_DATA, 0, NULL, OPEN_EXISTING, FILE_FLAG_SEQUENTIAL_SCAN, NULL);
    if (fd == INVALID_HANDLE_VALUE) {
        flickr_->ui_->logLastError(L"Couldn't open photo");
        return FALSE;
    }
    DWORD size = GetFileSize(fd, NULL);
    if (size == INVALID_FILE_SIZE) {
        flickr_->ui_->logLastError(L"failed to get photo size");
        CloseHandle(fd);
        return FALSE;
    }

    CreateStreamOnHGlobal(NULL, TRUE, bufRet);
    IStream *retStream = *bufRet;
    char buf[4096];
    DWORD totalBytesRead = 0;
    DWORD bytesRead = 0;
    BOOL ret;
    while (totalBytesRead < size && (ret = ReadFile(fd, buf, sizeof(buf), &bytesRead, NULL))) {
        if (!ret) {
            flickr_->ui_->logLastError(L"failed to read from photo");
            retStream->Release();
            CloseHandle(fd);
            return FALSE;
        }
        totalBytesRead += bytesRead;
        if (bytesRead == 0)
            break;
        retStream->Write(buf, bytesRead, NULL);
    }
    if (totalBytesRead != size) {
        flickr_->ui_->debugLogW(L"short read on photo");
    }
    CloseHandle(fd);
    *lenRet = totalBytesRead;
    return TRUE;
}

bool
HippoFlickr::HippoFlickrPhoto::getStream(IStream **bufRet, ULONG *lenRet)
{
    return genericGetStream(filename_, bufRet, lenRet);
}

bool
HippoFlickr::HippoFlickrPhoto::getThumbnailStream(IStream **bufRet, ULONG *lenRet)
{
    return genericGetStream(thumbnailFilename_, bufRet, lenRet);
}

void
HippoFlickr::notifyPhotoUploading(HippoFlickrPhoto *photo)
{
    if (statusDisplayState_ < STATUS_DISPLAY_DOCUMENT_LOADED)
        return;
    variant_t vResult;
    variant_t vFilename(photo->getFilename());
    invokeJavascript(L"dhFlickrPhotoUploadStarted", &vResult, 1, &vFilename);
}

void
HippoFlickr::HippoFlickrCreatePhotosetInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    HippoBSTR photoId;
    HippoPtr<IXMLDOMElement> photosetNode;
    variant_t vPhotosetId;
    variant_t vPhotosetUrl;
    HRESULT result;

    if (!findFirstNamedChild(top, L"photoset", photosetNode))
        return;

    result = photosetNode->getAttribute(_bstr_t(L"id"), &vPhotosetId);
    if (FAILED(result))
        return;
    result = photosetNode->getAttribute(_bstr_t(L"url"), &vPhotosetUrl);
    if (FAILED(result))
        return;
    
    this->flickr_->onPhotosetCreated(vPhotosetId.bstrVal, vPhotosetUrl.bstrVal);
    delete this;
}

void
HippoFlickr::HippoFlickrCreatePhotosetInvocation::onError()
{
    delete this;
}

void 
HippoFlickr::onPhotosetCreated(WCHAR *photoId, WCHAR *photoUrl)
{
    assert(state_ == CREATING_PHOTOSET);
    photoSetId_ = photoId;
    photoSetUrl_ = photoUrl;
    this->state_ = POPULATING_PHOTOSET;

    variant_t vResult;
    variant_t vPhotosetId(photoSetId_);
    variant_t vPhotosetUrl(photoSetUrl_);
    invokeJavascript(L"dhFlickrPhotosetCreated", &vResult, 2, &vPhotosetId, &vPhotosetUrl);

    processPhotoset();
}

STDMETHODIMP
HippoFlickr::CreatePhotoset(BSTR title, BSTR descriptionHtml) 
{
    if (state_ != IDLE) {
        ui_->debugLogW(L"got createPhotoset in unexpected state");
        return E_FAIL;
    }

    HippoFlickrPhoto *primaryPhoto = completedUploads_[0];
    completedUploads_.remove(0);
    photosetAddedPhotos_.append(primaryPhoto);

    state_ = CREATING_PHOTOSET;
    HippoFlickr::HippoFlickrCreatePhotosetInvocation *invocation = new HippoFlickr::HippoFlickrCreatePhotosetInvocation(this);
    invokeMethod(invocation, L"flickr.photosets.create",  
                                             L"auth_token", authToken_.m_str, 
                                             L"title", title,
                                             L"description", descriptionHtml,
                                             L"primary_photo_id", primaryPhoto->getFlickrId(),
                                             NULL);
    return S_OK;
}

void
HippoFlickr::HippoFlickrAddPhotoInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    flickr_->photosetAddedPhotos_.append(flickr_->activePhotosetPhoto_);
    flickr_->activePhotosetPhoto_ = NULL;
    delete this;
}

void
HippoFlickr::HippoFlickrAddPhotoInvocation::onError()
{
    delete this;
}

void
HippoFlickr::onPhotosetPopulated()
{
    assert(state_ == PHOTOSET_POPULATED);
    VARIANT vResult;
    invokeJavascript(L"dhFlickrPhotosetPopulated", &vResult, 0);    
}

void
HippoFlickr::processPhotoset()
{
    assert (state_ == POPULATING_PHOTOSET);
    if (completedUploads_.length() > 0) {
        activePhotosetPhoto_ = completedUploads_[0];
        completedUploads_.remove(0);
        HippoFlickr::HippoFlickrAddPhotoInvocation *invocation = new HippoFlickr::HippoFlickrAddPhotoInvocation(this);
        invokeMethod(invocation, L"flickr.photosets.addPhoto",  
                                             L"auth_token", authToken_.m_str, 
                                             L"photoset_id", photoSetId_.m_str,
                                             L"photo_id", activePhotosetPhoto_->getFlickrId,
                                             NULL);
    } else {
        state_ = PHOTOSET_POPULATED;
        onPhotosetPopulated();
    }
}

void 
HippoFlickr::processUploads()
{
    if (state_ == IDLE && pendingUploads_.length() > 0) {
        activeUploadPhoto_ = pendingUploads_[0];
        pendingUploads_.remove(0);

        WCHAR *mimeType;
        HRESULT res;
        HGLOBAL hg = NULL;
        void *buf;
        DWORD len;
        IStream *uploadStream;
        if (!activeUploadPhoto_->getStream(&uploadStream, &len))
            return;

        GetHGlobalFromStream(uploadStream, &hg);
        buf = GlobalLock(hg);
        res = FindMimeFromData(NULL, NULL, buf, len, NULL, 0, &mimeType, 0);
        if (FAILED(res)) {
            ui_->logError(L"couldn't determine mime type for photo", res);
            GlobalUnlock(hg);
            uploadStream->Release();
            return;
        }
        GlobalUnlock(hg);

        state_ = UPLOADING;
        HippoFlickr::HippoFlickrUploadInvocation *invocation = new HippoFlickr::HippoFlickrUploadInvocation(this, uploadStream);

        HippoHTTP *request = new HippoHTTP();
        invocation->setRequest(request); // takes ownership
        HippoBSTR apiSig;
        HippoArray<HippoBSTR> paramNames;
        HippoArray<HippoBSTR> paramValues;

        paramNames.append(HippoBSTR(L"api_key"));
        paramValues.append(HippoBSTR(apiKey_));
        paramNames.append(HippoBSTR(L"auth_token"));
        paramValues.append(HippoBSTR(authToken_));
        computeAPISig(paramNames, paramValues, apiSig);
        
        buf = GlobalLock(hg);
    
        notifyPhotoUploading(activeUploadPhoto_);
        request->doMultipartFormPost(uploadServiceUrl_, invocation, 
                                                  L"api_key", FALSE, apiKey_.m_str,
                                                  L"auth_token", FALSE, authToken_.m_str,
                                                  L"api_sig", FALSE, apiSig.m_str,
                                                  L"photo", TRUE, buf, len, mimeType, activeUploadPhoto_->getFilename(), NULL);                        
    }
}

void 
HippoFlickr::uploadPhoto(BSTR filename)
{
    ensureStatusWindow();

    if (state_ == UNINITIALIZED) {
        checkToken();
    } else if (state_ == REQUESTING_AUTH) {
        // temporary hack until we watch for when browser closes
        getToken();
    }
    enqueueUpload(filename);
    processUploads();
}


/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoFlickr::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoFlickr*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoFlickr)) 
        *result = static_cast<IHippoFlickr *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoFlickr)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoFlickr::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoFlickr::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoFlickr::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}
        
STDMETHODIMP
HippoFlickr::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    HippoQIPtr<IHippoFlickr> hippoFlickr(static_cast<IHippoFlickr *>(this));
    HRESULT hr = DispInvoke(hippoFlickr, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
