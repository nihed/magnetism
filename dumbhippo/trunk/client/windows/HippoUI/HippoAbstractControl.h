/* HippoAbstractControl.h: a "widget" (wrapper around an HWND)
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractWindow.h"
#include <hippo/hippo-basics.h>

class HippoAbstractControl : public HippoAbstractWindow {
public:
    HippoAbstractControl(); 

    int getWidthRequest();
    int getHeightRequest(int forWidth);

    void getLastRequest(int *width_p, 
                        int *height_p);

    void setParent(HippoAbstractControl *parent);

    void setResizable(HippoOrientation orientation,
                      bool             value);

    virtual void queueResize();

    virtual bool create();

    virtual void show(bool activate);

    bool isHResizable() { return hresizable_; }
    bool isVResizable() { return vresizable_; }

protected:
    virtual int getWidthRequestImpl() = 0;
    virtual int getHeightRequestImpl(int forWidth) = 0;

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    HippoAbstractControl *parent_;
private:
    int lastWidthRequest_;
    int lastHeightRequest_;
    unsigned int hresizable_ : 1;
    unsigned int vresizable_ : 1;
};
