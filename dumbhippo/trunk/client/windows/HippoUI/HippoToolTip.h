/* HippoToolTip.h: a standard Windows ToolTip window
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractWindow.h"
#include "HippoUIUtil.h"
#include <hippo/hippo-basics.h>
#include <commctrl.h>

class HippoToolTip : public HippoAbstractWindow {
public:
    HippoToolTip();

    virtual bool create();

    void setForWindow(HWND forWindow);

    void activate(const HippoRectangle *forArea,
                  const char           *text);
    void deactivate();

    virtual void show(bool activate);
    virtual void hide();

protected:

private:

    void getTrackingToolInfo(TOOLINFO *ti);

    HWND forWindow_;
    HippoRectangle forArea_;
    HippoBSTR text_;
};
