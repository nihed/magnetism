/* HippoToolTip.h: a standard Windows ToolTip window
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractWindow.h"
#include "HippoUIUtil.h"
#include <hippo/hippo-graphics.h>
#include <commctrl.h>

class HippoToolTip : public HippoAbstractWindow {
public:
    HippoToolTip();

    virtual bool create();

    void setForWindow(HWND forWindow);

    bool areaEqual(const HippoRectangle *forArea) {
        return hippo_rectangle_equal(forArea, &forArea_) != FALSE;
    }

    void update(int                   mouse_x,
                int                   mouse_y,
                const HippoRectangle *forArea,
                const char           *text);
    void activate();
    void deactivate();

    virtual void show(bool activate);
    virtual void hide();

protected:

private:

    void getTrackingToolInfo(TOOLINFO *ti);

    HWND forWindow_;
    HippoRectangle forArea_;
    int mouseX_, mouseY_;
    HippoBSTR text_;
};
