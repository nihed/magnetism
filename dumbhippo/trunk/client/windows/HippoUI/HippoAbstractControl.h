/* HippoAbstractControl.h: a "widget" (wrapper around an HWND)
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractWindow.h"

class HippoAbstractControl : public HippoAbstractWindow {
public:
    HippoAbstractControl(); 

    int getWidthRequest();
    int getHeightRequest(int forWidth);

    void getLastRequest(int *width_p, 
                        int *height_p);

    void setParent(HippoAbstractControl *parent);

    virtual void queueResize();

    virtual bool create();

    virtual void show(bool activate);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    HippoAbstractControl *parent_;
private:
    int lastWidthRequest_;
    int lastHeightRequest_;
};
