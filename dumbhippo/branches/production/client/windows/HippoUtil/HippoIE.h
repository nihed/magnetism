/* HippoIE.h: Embed an instance of the IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <mshtml.h>
#include <HippoUtil.h>
#include <HippoUtilExport.h>

class HippoInvocation;

class DLLEXPORT HippoIECallback
{
public:
    // Callback when the document completely finishes loading. (Similar
    // to a <body onload=''> handler.
    virtual void onDocumentComplete() = 0;

    // Callback when window.close is called
    virtual void onClose() = 0;

    // Open a browser window for an 'external' URL. A normal 
    // <a href=''> in the document content will be handled this way
    virtual void launchBrowser(const HippoBSTR &url) = 0;

    // Check if 'host' is the hostname of a trusted server. This is
    // used to determine whether we should allow navigation to URLs 
    // within our (less-secured) container. In general, this is a 
    // second layer of safety where the first layer of safety comes
    // from not allowing untrusted HTML in the container.
    virtual bool isOurServer(const HippoBSTR &host) = 0;

    // Implementation of QueryService(SID_SToplevelBrowser, ...);
    virtual HRESULT getToplevelBrowser(const IID  &ifaceID, void **toplevelBrowser) = 0;
};

/*
 DANGER DANGER DANGER
 This class displays content in the Local Machine IE security zone; this
 means the content can do almost anything, like reading local files
 and instantiating random COM components.  AT NO POINT SHOULD IT READ UNTRUSTED
 CONTENT.  This means for example that not only can you not point it at
 http://randomsite.com, you can't do http://randomsite.com in a frame either.
 External images should be fine though. (Frames on external sites are blocked and
 links to external sites redirected to IE by the handling of BeforeNavigate2,
 so shouldn't generally be an issue. Note however that javascript: URLs are
 *not* blocked, and must be trapped by HTML sanitization.)

 Currently using this on remote trusted sites is a lot like downloading a .exe
 from that site and executing it on the fly.  This means it's vulnerable to 
 MITM attacks without some external integrity mechanism such as SSL or 
 SHA1 sum checking.
 DANGER DANGER DANGER
*/
class DLLEXPORT HippoIE :
    public IUnknown
{
public:
    // Only sets up object. This is a static method rather than a constructor because
    // we derive the actual implementation privately to avoid exposing implementation
    // details in the public header.
    static HippoIE *create(HWND window, WCHAR *src, HippoIECallback *cb, IDispatch *application);

    // Optional, apply an XSLT stylesheet to source
    virtual void setXsltTransform(WCHAR *styleSrc, ...) = 0;

    // Set whether the IE window should have a 3D border (must be called before create)
    // defaults to true
    virtual void setThreeDBorder(bool threeDBorder) = 0;

    // Actually instantiate
    virtual void embedBrowser() = 0;

    // Explicit shutdown, necessary since there are refcount cycles
    virtual void shutdown() = 0;

    // Navigate to a new location programmatically
    virtual void setLocation(const HippoBSTR &location) = 0;

    // Return IWebBrowser2 interface, not reffed
    virtual IWebBrowser2 *getBrowser() = 0;

    virtual void resize(RECT *rect) = 0;
    virtual HippoInvocation createInvocation(const HippoBSTR &functionName) = 0;
};
