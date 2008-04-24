/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoButton.h: a pushbutton control
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include <hippo/hippo-event.h>
#include "HippoAbstractControl.h"

class HippoButtonListener {
public:
    virtual void onClicked() = 0;
};

class HippoButton : public HippoAbstractControl
{
public:
    HippoButton();
    
    void setText(const HippoBSTR &text);
    HippoBSTR getText();

    void setListener(HippoButtonListener *listener);

    virtual bool handleNotification(UINT notification);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);
    virtual bool create();
    virtual void destroy();

private:
    void ensureFont();
    void computeSize();

    HippoButtonListener *listener_;

    int widthReq_;
    int heightReq_;

    HippoBSTR text_;
    HFONT font_;
    WNDPROC oldWindowProc_;
};
