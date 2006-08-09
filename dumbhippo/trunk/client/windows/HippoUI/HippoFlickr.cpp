/* HippoFlickr.cpp: Integration with Flickr services
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"
#include "HippoFlickr.h"
#include "HippoUI.h"
#include <HippoRegKey.h>
#include "HippoIEWindow.h"
#include "HippoUIUtil.h"
#include "Guid.h"
extern "C" {
#include <md5.h>
}
#include "cleangdiplus.h"

#import <msxml3.dll>  named_guids
#include <mshtml.h>

#include <wincrypt.h>

static const WCHAR *HIPPO_SUBKEY_FLICKR = HIPPO_REGISTRY_KEY L"\\Flickr";

HippoFlickr::HippoFlickr(HippoUI *ui) : baseServiceUrl_(L"http://www.flickr.com/services/rest/"), 
                                        authServiceUrl_(L"http://flickr.com/services/auth/"),
                                        signupUrl_(L"http://www.flickr.com/signup/"),
                                        uploadServiceUrl_(L"http://www.flickr.com/services/upload/"),
                                        sharedSecret_(L"a31c67baceb0761e"),
                                        apiKey_(L"0e96a6f88118ed4d866a0651e45383c1")
{
    state_ = UNINITIALIZED;
    activeUploadPhoto_ = NULL;
    activeTaggingPhoto_ = NULL;
    shareWindow_ = NULL;
    ieWindowCallback_ = NULL;
    ui_ = ui;
    authBrowser_ = NULL;

    hippoLoadTypeInfo((WCHAR *)0,
                      &IID_IHippoFlickr, &ifaceTypeInfo_,
                      NULL);

    HippoRegKey hippoFlickrReg(HKEY_CURRENT_USER, 
                               HIPPO_SUBKEY_FLICKR,
                               false);
    HippoBSTR userId;
    if (hippoFlickrReg.loadString(L"userId", &userId)) {
        haveAccount_ = TRUE;
        showShareWindow();
    } else {
        ui_->debugLogW(L"creating Flickr first time window");
        haveAccount_ = FALSE;
        setState(LOADING_FIRSTTIME);
        showIEWindow(L"Sharing Photos Setup", L"sharephotoset-first", new HippoFlickrFirstTimeWindowCallback(this));
    }
}

HippoFlickr::~HippoFlickr(void)
{
    for (UINT i = 0; i < pendingUploads_.length(); i++)
        delete pendingUploads_[i];
    if (activeUploadPhoto_)
        delete activeUploadPhoto_;
    for (UINT i = 0; i < completedUploads_.length(); i++)
        delete completedUploads_[i];
    if (ieWindowCallback_)
        delete ieWindowCallback_;
    if (shareWindow_)
        delete shareWindow_;
    if (authBrowser_)
        delete authBrowser_;
}

void 
HippoFlickr::setState(State newState)
{
    // Allow entry into these two states from any state
    if (newState != FATAL_ERROR && newState != CANCELLED) {
    switch (state_) {
        case UNINITIALIZED:
            assert(newState == LOADING_STATUSDISPLAY || newState == LOADING_FIRSTTIME);
            break;
        case LOADING_FIRSTTIME:
            assert(newState == DISPLAYING_FIRSTTIME);
            break;
        case DISPLAYING_FIRSTTIME:
            assert(newState == CREATING_ACCOUNT || newState == LOADING_STATUSDISPLAY);
            break;
        case CREATING_ACCOUNT:
            assert(newState == LOADING_STATUSDISPLAY);
            break;
        case LOADING_STATUSDISPLAY:
            assert(newState == PREPARING_THUMBNAILS);
            break;
        case PREPARING_THUMBNAILS:
            assert(newState == CHECKING_TOKEN || newState == REQUESTING_FROB);
            break;
        case CHECKING_TOKEN:
            assert(newState == IDLE_AWAITING_SHARE || newState == REQUESTING_FROB);
            break;
        case REQUESTING_FROB:
            assert(newState == REQUESTING_AUTH);
            break;
        case REQUESTING_AUTH:
            assert(newState == REQUESTING_TOKEN);
            break;
        case REQUESTING_TOKEN:
            assert(newState == IDLE_AWAITING_SHARE);  
            break;
        case IDLE_AWAITING_SHARE:
            assert(newState == IDLE_AWAITING_SHARE || newState == UPLOADING_AWAITING_SHARE || newState == PROCESSING_FINALIZING); 
            break;
        case UPLOADING_AWAITING_SHARE:
            assert(newState == IDLE_AWAITING_SHARE || newState == UPLOADING_AWAITING_SHARE || newState == PROCESSING_UPLOADING); 
            break;
        case PROCESSING_UPLOADING:
            assert(newState == PROCESSING_UPLOADING || newState == PROCESSING_FINALIZING); 
            break;
        case PROCESSING_FINALIZING:
            assert(newState == COMPLETE); 
            break;
        case COMPLETE:
            assert(newState == COMPLETE);
            break;
        case FATAL_ERROR:
            assert(FALSE);
            break;
        case CANCELLED:
            assert(FALSE);
            break;
        default:
            assert(FALSE);
    }
    }
    ui_->debugLogW(L"HippoFlickr transitioning from state %d to %d", state_, newState);
    state_ = newState;
}

HippoInvocation
HippoFlickr::createInvocation(const HippoBSTR &functionName)
{
    ui_->debugLogW(L"invoking javascript method %s", functionName.m_str);
    return ie_->createInvocation(functionName);
}

void
HippoFlickr::HippoFlickrFirstTimeWindowCallback::onDocumentComplete() 
{
    if (flickr_->state_ != HippoFlickr::State::DISPLAYING_FIRSTTIME)
        flickr_->setState(HippoFlickr::State::DISPLAYING_FIRSTTIME);
}

bool
HippoFlickr::HippoFlickrFirstTimeWindowCallback::onClose()
{
    flickr_->setState(HippoFlickr::State::CANCELLED);
    return TRUE;
}

void
HippoFlickr::HippoFlickrStatusWindowCallback::onDocumentComplete() 
{
    // We need to wait until the link share display is fully loaded before doing
    // much
    if (flickr_->state_ == HippoFlickr::LOADING_STATUSDISPLAY) {
        flickr_->ui_->debugLogW(L"got flickr document complete");
        flickr_->setState(HippoFlickr::PREPARING_THUMBNAILS);
        for (UINT i = 0; i < flickr_->pendingDisplay_.length(); i++) {
            flickr_->prepareUpload(flickr_->pendingDisplay_[i]);
        }
        flickr_->checkToken();
    }
}

bool
HippoFlickr::HippoFlickrStatusWindowCallback::onClose()
{
    flickr_->setState(HippoFlickr::State::CANCELLED);
    return TRUE;
}

void 
HippoFlickr::showIEWindow(WCHAR *title, WCHAR *relUrl, HippoIEWindowCallback *cb)
{
    if (shareWindow_ != NULL)
        delete shareWindow_;
    if (ieWindowCallback_ != NULL)
        delete ieWindowCallback_;

    HippoBSTR url;
    ui_->getRemoteURL(HippoBSTR(relUrl), &url);
    ieWindowCallback_ = cb;
    shareWindow_ = new HippoIEWindow(url, ieWindowCallback_);
    shareWindow_->setUI(ui_);
    shareWindow_->setApplication(this);
    shareWindow_->setTitle(title);
    shareWindow_->create();
    shareWindow_->moveResize(CW_DEFAULT, CW_DEFAULT, 650, 600);
    shareWindow_->show();
    ie_ = shareWindow_->getIE();
}

void
HippoFlickr::showShareWindow(void)
{
    ui_->debugLogW(L"creating Flickr share window");

    setState(LOADING_STATUSDISPLAY);

    HippoArray<HippoBSTR> queryParamNames;
    HippoArray<HippoBSTR> queryParamValues;
    queryParamNames.append(HippoBSTR(L"next"));
    queryParamValues.append(HippoBSTR(L"close"));
    HippoBSTR queryString;
    HippoUIUtil::encodeQueryString(queryString, queryParamNames, queryParamValues);
    HippoBSTR url(L"sharephotoset");
    url.Append(queryString);

    showIEWindow(L"Share Photos", url, new HippoFlickrStatusWindowCallback(this));
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
    HippoArray<HippoBSTR> sortedParamNames;
    HippoArray<HippoBSTR> sortedParamValues;

    sortParamArrays(paramNames, paramValues, sortedParamNames, sortedParamValues);

    for (unsigned int i = 0; i < sortedParamNames.length(); i++) {
        sig.Append(sortedParamNames[i]);
        sig.Append(sortedParamValues[i]);
    }

    unsigned char digest[16];
    WCHAR digestStr[33];
    HippoUStr utf(sig);
    MD5Context md5Ctx;
    MD5Init(&md5Ctx);
    MD5Update(&md5Ctx, (unsigned char *)utf.c_str(), static_cast<unsigned int>(strlen((char *)utf.c_str())));
    MD5Final(digest, &md5Ctx);
    for (unsigned int i = 0; i < 16; i++) {
        WCHAR *digestPtr = digestStr;
        StringCchPrintfW(digestPtr+(2*i), sizeof(digestStr) - 2*i, L"%02X", digest[i]);
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
        paramNames.append(HippoBSTR(argName));
        paramValues.append(HippoBSTR(argValue));
    }

    appendApiSig(paramNames, paramValues);

    HippoUIUtil::encodeQueryString(query, paramNames, paramValues);
    url = baseServiceUrl_;
    url.Append(query);

    HippoHTTP *http = new HippoHTTP();
    http->doGet(url, false, invocation);
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
HippoFlickr::HippoFlickrRESTInvocation::handleError(const HippoBSTR &text)
{
    flickr_->ui_->debugLogW(L"HippoFlickr failure: %s", text);
    flickr_->createInvocation(L"dhFlickrError").add(text).run();
    if (flickr_->state_ == HippoFlickr::State::CANCELLED)
        return;
    this->onError();
}

void 
HippoFlickr::HippoFlickrRESTInvocation::handleComplete(void *responseData, long responseBytes) {
    HippoPtr<IXMLDOMDocument> doc;
    HRESULT hr;
    VARIANT_BOOL successful;

    flickr_->ui_->debugLogW(L"got REST method response");
    if (flickr_->state_ == HippoFlickr::State::CANCELLED)
        return;
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
        HippoBSTR msg(L"Flickr error");

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
HippoFlickr::HippoFlickrCheckTokenInvocation::handleAuth(WCHAR *token, WCHAR *userId)
{
    this->flickr_->authToken_ = token;
    this->flickr_->userId_ = userId;
    this->flickr_->notifyUserId();
    this->flickr_->processUploads();
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
                               HIPPO_SUBKEY_FLICKR,
                               false);
    HippoBSTR token;
    if (hippoFlickrReg.loadString(L"token", &token)) {
        HippoFlickr::HippoFlickrCheckTokenInvocation *invocation = new HippoFlickr::HippoFlickrCheckTokenInvocation(this);
        invokeMethod(invocation, L"flickr.auth.checkToken", L"auth_token", token.m_str, NULL);
        setState(CHECKING_TOKEN);
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
HippoFlickr::HippoFlickrAuthBrowserCallback::onNavigate(HippoExternalBrowser *browser, BSTR url)
{
    flickr_->ui_->debugLogW(L"got navigate for auth browser, url=%s", url);
    if (flickr_->state_ == HippoFlickr::State::CREATING_ACCOUNT &&
        (wcscmp(L"http://www.flickr.com/", url) == 0
         || wcscmp(L"http://www.flickr.com/welcome/hello/", url) == 0))
    {
        flickr_->showShareWindow();
    }
}

void 
HippoFlickr::HippoFlickrAuthBrowserCallback::onDocumentComplete(HippoExternalBrowser *browser)
{
    flickr_->ui_->debugLogW(L"got document complete for auth browser");
}

void 
HippoFlickr::HippoFlickrAuthBrowserCallback::onQuit(HippoExternalBrowser *browser)
{
    flickr_->ui_->debugLogW(L"got onQuit for auth browser");
    if (flickr_->state_ == HippoFlickr::State::REQUESTING_AUTH) {
        flickr_->createInvocation(L"dhFlickrAuthComplete").run();
        flickr_->getToken();
    } else if (flickr_->state_ == HippoFlickr::State::CREATING_ACCOUNT) {
        flickr_->showShareWindow();
    } else {
        // Shouldn't get here
        return;
    }
    delete flickr_->authCb_;
    flickr_->authCb_ = NULL;
    flickr_->authBrowser_->Release();
    flickr_->authBrowser_ = NULL;
}

void
HippoFlickr::getAuthUrl(HippoBSTR &authUrl)
{
    HippoArray<HippoBSTR> paramNames;
    HippoArray<HippoBSTR> paramValues;
    authUrl = authServiceUrl_;

    paramNames.append(HippoBSTR(L"api_key"));
    paramValues.append(HippoBSTR(apiKey_));
    paramNames.append(HippoBSTR(L"perms"));
    paramValues.append(HippoBSTR(L"write"));
    paramNames.append(HippoBSTR(L"frob"));
    paramValues.append(HippoBSTR(authFrob_));

    appendApiSig(paramNames, paramValues);

    HippoBSTR authQuery;
    HippoUIUtil::encodeQueryString(authQuery, paramNames, paramValues);
    authUrl.Append(authQuery);
}

void
HippoFlickr::setFrob(WCHAR *frob)
{
    HippoBSTR authURL;
    HippoBSTR partialAuthQuery;

    ui_->debugLogW(L"got Flickr auth frob %s", frob);

    setState(REQUESTING_AUTH);
    authFrob_ = frob;

    getAuthUrl(authURL);

    createInvocation(L"dhFlickrAwaitingAuth").run();
    if (authBrowser_ != NULL) {
        // We already have a browser open for the account creation, reuse it
        authBrowser_->navigate(authURL);
    } else {
        authCb_ = new HippoFlickrAuthBrowserCallback(this);
        authBrowser_ = new HippoExternalBrowser(authURL, TRUE, authCb_);
    }
}

void
HippoFlickr::getFrob()
{
    HippoFlickrFrobInvocation *frobInvocation = new HippoFlickrFrobInvocation(this);
    invokeMethod(frobInvocation, L"flickr.auth.getFrob", NULL);
    setState(REQUESTING_FROB);
}

void
HippoFlickr::HippoFlickrAbstractAuthInvocation::handleCompleteXML(IXMLDOMElement *doc)
{
    HippoBSTR token;
    HippoPtr<IXMLDOMElement> authNode;
    HippoPtr<IXMLDOMElement> userNode;
    HRESULT hr;

    if (!findFirstNamedChild(doc, L"auth", authNode))
        return;
    if (!findFirstNamedChildTextValue(authNode, L"token", token))
        return;
    if (!findFirstNamedChild(authNode, L"user", userNode))
        return;
    _variant_t vUserId(L"");
    hr = userNode->getAttribute(_bstr_t(L"nsid"), &vUserId);
    if (FAILED(hr)) {
        this->handleError(hr);
        return;
    }
    if (vUserId.vt != VT_BSTR) {
        this->handleError(L"couldn't find nsid");
        return;
    }

    this->handleAuth(token, vUserId.bstrVal);
    delete this;
}

void
HippoFlickr::HippoFlickrTokenInvocation::handleAuth(WCHAR *token, WCHAR *nsid)
{
    flickr_->authToken_= token;
    flickr_->userId_ = nsid;
    this->flickr_->notifyUserId();
    flickr_->ui_->debugLogW(L"got Flickr auth token %s, userid %s", flickr_->authToken_.m_str, flickr_->userId_.m_str);
    HippoRegKey hippoFlickrReg(HKEY_CURRENT_USER, 
                               HIPPO_SUBKEY_FLICKR,
                               true);
    hippoFlickrReg.saveString(L"token", flickr_->authToken_);
    hippoFlickrReg.saveString(L"userId", flickr_->userId_);

    flickr_->processUploads();
}

void
HippoFlickr::getToken()
{
    HippoFlickr::HippoFlickrTokenInvocation *invocation = new HippoFlickr::HippoFlickrTokenInvocation(this);
    invokeMethod(invocation, L"flickr.auth.getToken", L"frob", authFrob_.m_str, NULL);
    setState(REQUESTING_TOKEN);
}

void
HippoFlickr::prepareUpload(BSTR filename)
{
    assert(state_ > LOADING_STATUSDISPLAY);
    HippoFlickrPhoto *photo = new HippoFlickrPhoto(this, filename);
    pendingUploads_.append(photo);
    createInvocation(L"dhFlickrAddPhoto")
        .add(photo->getFilename())
        .add(photo->getThumbnailFilename())
        .run();
    if (state_ == IDLE_AWAITING_SHARE)
        processUploads();
}

void
HippoFlickr::enqueueUpload(BSTR filename)
{
    if (state_ <= LOADING_STATUSDISPLAY) {
        pendingDisplay_.append(filename);
    } else {
        prepareUpload(filename);
    }
}

void
HippoFlickr::onUploadComplete(WCHAR *photoId)
{
    ui_->debugLogW(L"got upload complete for photo %s", photoId);
    activeUploadPhoto_->setFlickrId(photoId);
    createInvocation(L"dhFlickrPhotoUploadComplete")
        .add(activeUploadPhoto_->getFilename())
        .add(activeUploadPhoto_->getFlickrId())
        .run();
    activeUploadPhoto_->setState(HippoFlickr::HippoFlickrPhoto::PhotoState::UPLOADING_THUMBNAIL);

    HGLOBAL hg = NULL;
    void *buf;
    DWORD len;
    IStream *uploadStream;
    if (!activeUploadPhoto_->getThumbnailStream(&uploadStream, &len))
        return;

    GetHGlobalFromStream(uploadStream, &hg);
    buf = GlobalLock(hg);

    HippoFlickr::HippoFlickrUploadThumbnailInvocation *invocation 
        = new HippoFlickr::HippoFlickrUploadThumbnailInvocation(this, uploadStream);

    HippoBSTR postThumbnailUrl;
    ui_->getRemoteURL(L"upload/postinfo", &postThumbnailUrl);

    HippoHTTP *request = new HippoHTTP();
    invocation->setRequest(request); // takes ownership
    ui_->debugLogW(L"doing POST to %s", postThumbnailUrl.m_str);
    request->doMultipartFormPost(postThumbnailUrl, invocation, 
        L"thumbnail", TRUE, buf, len, L"image/png", activeUploadPhoto_->getFilename(), NULL);
}

void
HippoFlickr::onUploadThumbnailComplete(WCHAR *url)
{
    activeUploadPhoto_->setThumbnailUrl(url);
    activeUploadPhoto_->setState(HippoFlickr::HippoFlickrPhoto::PhotoState::REQUESTING_INFO);
    createInvocation(L"dhFlickrPhotoThumbnailUploadComplete")
        .add(activeUploadPhoto_->getFilename())
        .add(activeUploadPhoto_->getThumbnailUrl())
        .run();
    HippoFlickr::HippoFlickrGetInfoInvocation *invocation = new HippoFlickr::HippoFlickrGetInfoInvocation(this);
    invokeMethod(invocation, L"flickr.photos.getInfo", L"auth_token", authToken_.m_str, L"photo_id", activeUploadPhoto_->getFlickrId(), NULL);
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
HippoFlickr::HippoFlickrUploadThumbnailInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    HippoBSTR url;
    if (!findFirstNamedChildTextValue(top, L"url", url))
        return;
    this->flickr_->onUploadThumbnailComplete(url);
    delete this;
}

void
HippoFlickr::HippoFlickrGetInfoInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    HippoPtr<IXMLDOMElement> photoNode;

    if (!findFirstNamedChild(top, L"photo", photoNode))
        return;
    this->flickr_->onGetInfoComplete(photoNode);
    delete this;
}

void
HippoFlickr::onGetInfoComplete(IXMLDOMElement *photo)
{
    HippoBSTR xml;
    HRESULT res;
    res = photo->get_xml(&xml);
    if (!SUCCEEDED(res)) {
        ui_->debugLogW(L"failed to get XML property from photo");
        setState(HippoFlickr::State::FATAL_ERROR);
        return;
    }
    activeUploadPhoto_->setInfoXml(xml);
    createInvocation(L"dhFlickrPhotoSetInfoXml")
        .add(activeUploadPhoto_->getFilename())
        .add(activeUploadPhoto_->getInfoXml())
        .run();
    activeUploadPhoto_->setState(HippoFlickr::HippoFlickrPhoto::PhotoState::PRESHARE);
    completedUploads_.append(activeUploadPhoto_);
    activeUploadPhoto_ = NULL;
    processUploads();
}

HippoFlickr::HippoFlickrPhoto::HippoFlickrPhoto(HippoFlickr *flickr, WCHAR *filename)
{
    flickr_ = flickr;
    filename_ = filename;
    state_ = HippoFlickr::HippoFlickrPhoto::PhotoState::INITIAL;

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
        flickr_->ui_->debugLogW(L"failed to read photo, error code %d", st);
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
        flickr_->ui_->logLastHresult(L"Couldn't open photo");
        return FALSE;
    }
    DWORD size = GetFileSize(fd, NULL);
    if (size == INVALID_FILE_SIZE) {
        flickr_->ui_->logLastHresult(L"failed to get photo size");
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
            flickr_->ui_->logLastHresult(L"failed to read from photo");
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
HippoFlickr::notifyUserId() 
{
    createInvocation(L"dhFlickrSetUserId").add(userId_).run();
}

// IHippoFlickr implementation

STDMETHODIMP
HippoFlickr::HaveFlickrAccount(BOOL haveAccount) 
{
    if (state_ != HippoFlickr::State::DISPLAYING_FIRSTTIME) {
        ui_->debugLogW(L"got HaveFlickrAccount in invalid state");
        return E_FAIL;
    }
    ui_->debugLogW(L"have flickr account: %s", haveAccount ? L"TRUE" : L"FALSE");
    haveAccount_ = !!haveAccount;
    if (haveAccount) {
        delete shareWindow_;
        shareWindow_ = NULL;
        showShareWindow();
    } else {
        setState(HippoFlickr::State::CREATING_ACCOUNT);
        authCb_ = new HippoFlickrAuthBrowserCallback(this);
        authBrowser_ = new HippoExternalBrowser(signupUrl_, TRUE, authCb_);
        createInvocation(L"dhFlickrAwaitingAccount").run();
    }
    return S_OK;
}

STDMETHODIMP
HippoFlickr::CreatePhotoset(BSTR title) 
{
    UINT len = ::SysStringLen(title);

    if (len == 0)
        return E_FAIL;


    tagTitle_ = L"";
    for (UINT i = 0; i < len; i++) {
        if (iswalnum(title[i])) {
            WCHAR substr[2];
            substr[0] = title[i];
            substr[1] = 0;
            tagTitle_.Append(substr);
        }
    }

    createInvocation(L"dhFlickrSetTagName").add(tagTitle_).run();
    switch (state_) {
        case IDLE_AWAITING_SHARE:
            setState(PROCESSING_FINALIZING);
            processPhotoset();
            break;
        case UPLOADING_AWAITING_SHARE:
            setState(PROCESSING_UPLOADING);
            break;
        default:
            ui_->debugLogW(L"got CreatePhotoset in unexpected state %d", state_);
            return E_FAIL;
    }
    return S_OK;
}

void
HippoFlickr::HippoFlickrTagPhotoInvocation::handleCompleteXML(IXMLDOMElement *top)
{
    flickr_->activeTaggingPhoto_->setState(HippoFlickr::HippoFlickrPhoto::PhotoState::COMPLETE);
    flickr_->createInvocation(L"dhFlickrPhotoComplete").add(flickr_->activeTaggingPhoto_->getFilename()).run();
    flickr_->processed_.append(flickr_->activeTaggingPhoto_);
    flickr_->activeTaggingPhoto_ = NULL;
    flickr_->processPhotoset();
    delete this;
}

void
HippoFlickr::processPhotoset()
{
    assert(state_ == HippoFlickr::State::PROCESSING_FINALIZING); 
    if (completedUploads_.length() == 0) {
        setState(HippoFlickr::State::COMPLETE);
        return;
    }

    assert(activeTaggingPhoto_ == NULL);
    activeTaggingPhoto_ = completedUploads_[0];
    completedUploads_.remove(0);
    HippoFlickr::HippoFlickrTagPhotoInvocation *invocation = new HippoFlickr::HippoFlickrTagPhotoInvocation(this);
    invokeMethod(invocation, L"flickr.photos.addTags",  
                 L"auth_token", authToken_.m_str, 
                 L"photo_id", activeTaggingPhoto_->getFlickrId(),
                 L"tags", tagTitle_.m_str,
                 NULL);
}

void 
HippoFlickr::processUploads()
{
    bool haveMore = pendingUploads_.length() > 0;
    if (!haveMore) {
        switch (state_) {
        case CHECKING_TOKEN:
        case REQUESTING_TOKEN:
        case UPLOADING_AWAITING_SHARE:
            setState(IDLE_AWAITING_SHARE);
            return;
        case PROCESSING_UPLOADING:
            setState(PROCESSING_FINALIZING);
            processPhotoset();
            return;
        default:
            assert(FALSE);
        }
    } else {
        switch (state_) {
        case CHECKING_TOKEN:
        case REQUESTING_TOKEN:
            setState(HippoFlickr::State::IDLE_AWAITING_SHARE);
            break;
        default:
            break;
        }
    }

    assert(activeUploadPhoto_ == NULL);
    assert(pendingUploads_.length() > 0);                  

    activeUploadPhoto_ = pendingUploads_[0];
    assert(activeUploadPhoto_->getState() == HippoFlickrPhoto::INITIAL);
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
        ui_->logHresult(L"couldn't determine mime type for photo", res);
        GlobalUnlock(hg);
        uploadStream->Release();
        return;
    }
    GlobalUnlock(hg);

    setState(HippoFlickr::State::UPLOADING_AWAITING_SHARE);
    activeUploadPhoto_->setState(HippoFlickr::HippoFlickrPhoto::PhotoState::UPLOADING);
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

    createInvocation(L"dhFlickrPhotoUploadStarted").add(activeUploadPhoto_->getFilename()).run();
    ui_->debugLogW(L"doing POST to %s", uploadServiceUrl_.m_str);
    request->doMultipartFormPost(uploadServiceUrl_, invocation, 
        L"api_key", FALSE, apiKey_.m_str,
        L"auth_token", FALSE, authToken_.m_str,
        L"api_sig", FALSE, apiSig.m_str,
        L"photo", TRUE, buf, len, mimeType, activeUploadPhoto_->getFilename(), NULL);
}

void 
HippoFlickr::uploadPhoto(BSTR filename)
{
    enqueueUpload(filename);
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
