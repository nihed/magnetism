/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoCanvas.h: a text entry control
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include <hippo/hippo-event.h>
#include "HippoAbstractControl.h"

class HippoEditListener {
public:
    virtual void onTextChanged() = 0;
    virtual bool onKeyPress(HippoKey key, gunichar character) = 0;
};

class HippoEdit : public HippoAbstractControl
{
public:
    HippoEdit();
    
    void setText(const HippoBSTR &text);
    HippoBSTR getText();

    void setListener(HippoEditListener *listener);

    bool handleNotification(UINT notification);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);
    virtual bool create();
    virtual void destroy();

private:
    bool filterMessage(UINT message, WPARAM wParam, LPARAM lParam);

    static LRESULT CALLBACK filterWindowProc(HWND   window,
                                             UINT   message,
                                             WPARAM wParam,
                                             LPARAM lParam);

    HippoEditListener *listener_;

    int widthReq_;
    int heightReq_;

    HippoBSTR text_;
    HFONT font_;
    WNDPROC oldWindowProc_;
};

