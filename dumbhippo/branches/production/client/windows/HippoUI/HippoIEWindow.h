/* HippoIEWindow.h: Toplevel window with an embedded IE web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

class HippoIEWindowCallback
{
public:
    virtual void onDocumentComplete() = 0;
    // Return TRUE to allow close, FALSE to disallow
    virtual bool onClose() {
        return TRUE;
    }
};

class HippoIEWindow : public HippoAbstractWindow
{
public:
    HippoIEWindow(WCHAR *src, HippoIEWindowCallback *cb);
    ~HippoIEWindow(void);

protected:
    virtual void onDocumentComplete();
    virtual void onClose(bool fromScript);

private:
    HippoIEWindowCallback *cb_;
    bool created_;
};
