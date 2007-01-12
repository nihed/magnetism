/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoButton.cpp: a pushbutton control
 *
 * Copyright Red Hat, Inc. 2006
 **/

#include "stdafx-hippoui.h"
#include "HippoButton.h"

#define MIN_WIDTH 50

/* If you don't change the font on the item, it defaults to a bitmapped font and looks
 * *terrible*. We might want to pick up the font from the item instead of just hardcoding
 * here, or alternatively use the normal system dialog font and size.
 */
#define FONT_SIZE 13 // In pixels
#define FONT_NAME L"Arial"

#define HORIZONTAL_PADDING 6 // Amount to add to the string size horizontally on top and bottom
#define VERTICAL_PADDING   2 // Amount to add to the string size vertically on each side

/* Change notification: If I read the MSDN docs correctly, change notification messages
 * are sent to the *parent* of the control, not the control itself. So, to get 
 * notify::text working correctly we'd need to add some facilities to HippoAbstractControl
 * to send WM_COMMAND notifications to its children. We don't need notify::text at the 
 * moment, so we ignore the issue.
 */

HippoButton::HippoButton()
{
    // standard Windows control
    setClassName(L"BUTTON");

    setWindowStyle(WS_CHILD | BS_PUSHBUTTON);
}
    
void 
HippoButton::setListener(HippoButtonListener *listener)
{
    listener_ = listener;
}

bool 
HippoButton::handleNotification(UINT notification)
{
    switch (notification) {
        case BN_CLICKED:
            listener_->onClicked();
            return true;
        default:
            return false;
    }
}

void 
HippoButton::setText(const HippoBSTR &text)
{
    text_ = text;
    if (window_)
        SendMessage(window_, WM_SETTEXT, 0, (LPARAM)text.m_str);
}

HippoBSTR
HippoButton::getText()
{
#define MAX_LENGTH 1024

    if (window_) {
        WCHAR buffer[MAX_LENGTH];

        unsigned int length = (DWORD)SendMessage(window_, WM_GETTEXT, (WPARAM)MAX_LENGTH, (LPARAM)buffer);

        return HippoBSTR(length, buffer);
    } else {
        return text_;
    }
}

void
HippoButton::ensureFont()
{
    if (isDestroyed())
        return;

    LOGFONT lf;

    lf.lfHeight = - FONT_SIZE;
    lf.lfWidth = 0;
    lf.lfEscapement = 0;
    lf.lfOrientation = 0;
    lf.lfWeight = FW_DONTCARE;
    lf.lfItalic = FALSE;
    lf.lfUnderline = FALSE;
    lf.lfStrikeOut = FALSE;
    lf.lfCharSet = DEFAULT_CHARSET;
    lf.lfOutPrecision = OUT_DEFAULT_PRECIS;
    lf.lfClipPrecision = CLIP_DEFAULT_PRECIS;
    lf.lfQuality = DEFAULT_QUALITY;
    lf.lfPitchAndFamily = DEFAULT_PITCH | FF_DONTCARE;
    
    wcsncpy(lf.lfFaceName, FONT_NAME, G_N_ELEMENTS(lf.lfFaceName)- 1);
    lf.lfFaceName[G_N_ELEMENTS(lf.lfFaceName)- 1] = 0;

    font_ = CreateFontIndirect(&lf);
}

bool
HippoButton::create()
{
    if (isCreated())
        return true;

    if (!HippoAbstractControl::create())
        return false;

    ensureFont();
    SendMessage(window_, WM_SETFONT, (WPARAM)font_,(LPARAM)FALSE);

    if (text_)
        SendMessage(window_, WM_SETTEXT, 0, (LPARAM)text_.m_str);

    return true;
}

void
HippoButton::destroy()
{
    if (font_) {
        DeleteObject(font_);
        font_ = NULL;
    }

    HippoAbstractControl::destroy();
}

void
HippoButton::computeSize()
{
    HDC dc;
    SIZE size = { 0, 0 };

    ensureFont();

    if (window_)
        dc = GetDC(window_);
    else
        dc = CreateDC(L"DISPLAY", NULL, NULL, NULL);

    if (dc && text_) {
        SelectObject(dc, font_);
        GetTextExtentPoint32(dc, text_.m_str, text_.Length(), &size);
    }

    widthReq_ = size.cx + HORIZONTAL_PADDING * 2;
    heightReq_ = FONT_SIZE + VERTICAL_PADDING * 2;

    if (window_)
        ReleaseDC(window_, dc);
    else
        DeleteDC(dc);
}

int 
HippoButton::getWidthRequestImpl()
{
    if (widthReq_ < 0)
        computeSize();

    return widthReq_;
}
 
int
HippoButton::getHeightRequestImpl(int forWidth)
{
    if (heightReq_ < 0)
        computeSize();

    return heightReq_;
}
