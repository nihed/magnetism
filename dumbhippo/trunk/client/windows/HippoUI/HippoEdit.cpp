/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoCanvas.cpp: a text entry control
 *
 * Copyright Red Hat, Inc. 2006
 **/

#include "stdafx-hippoui.h"
#include "HippoEdit.h"

#define MIN_WIDTH 50

/* If you don't change the font on the item, it defaults to a bitmapped font and looks
 * *terrible*. We might want to pick up the font from the item instead of just hardcoding
 * here, or alternatively use the normal system dialog font and size.
 */
#define FONT_SIZE 14 // In pixels
#define FONT_NAME L"Arial"

#define VERTICAL_PADDING 1 // Vertical padding around the font

/* Change notification: If I read the MSDN docs correctly, change notification messages
 * are sent to the *parent* of the control, not the control itself. So, to get 
 * notify::text working correctly we'd need to add some facilities to HippoAbstractControl
 * to send WM_COMMAND notifications to its children. We don't need notify::text at the 
 * moment, so we ignore the issue.
 */

HippoEdit::HippoEdit()
{
    // standard Windows control
    setClassName(L"EDIT");

    // If we don't specify a ES_AUTOHSCROLL, then the user's entry is clipped to the
    // visible area; while that could be useful in some circumstances,  ES_AUTOHSCROLL 
    // gives us something more like the GTK+ behavior where we can only limit by 
    // character count.
    setWindowStyle(WS_CHILD | ES_AUTOHSCROLL);

    widthReq_ = MIN_WIDTH;
    heightReq_ = FONT_SIZE + VERTICAL_PADDING * 2;
}
    
void 
HippoEdit::setListener(HippoEditListener *listener)
{
    listener_ = listener;
}

void 
HippoEdit::setText(const HippoBSTR &text)
{
    SendMessage(window_, WM_SETTEXT, 0, (LPARAM)text.m_str);
}

HippoBSTR 
HippoEdit::getText()
{
#define MAX_LENGTH 1024

    WCHAR buffer[MAX_LENGTH];

    unsigned int length = (DWORD)SendMessage(window_, WM_GETTEXT, (WPARAM)MAX_LENGTH, (LPARAM)buffer);

    return HippoBSTR(length, buffer);
}


bool
HippoEdit::create()
{
    if (isCreated())
        return true;

    if (!HippoAbstractControl::create())
        return false;

    /* In order to intercept keystrokes before the edit control has a chance to process them.
     * we swap out it's window procedure for our own, then chain to the original procedure.
     * Suprisingly, this seems to be a fairly normal thing to do.
     *
     * DO NOT CUT AND PASTE. Refactor the facility into the base control class instead.
     */
    
    /* Disable incorrect warnings about 64-bit safety. See comments in HippoUtil.h
    */
    #pragma warning(push)
    #pragma warning(disable : 4244 4312)
    oldWindowProc_ = (WNDPROC)GetWindowLong(window_, GWL_WNDPROC);
    SetWindowLongPtrW(window_, GWL_WNDPROC, (LONG_PTR)filterWindowProc);
    #pragma warning(pop)
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

    SendMessage(window_, WM_SETFONT, (WPARAM)font_,(LPARAM)FALSE);

    return true;
}

void
HippoEdit::destroy()
{
    if (font_) {
        DeleteObject(font_);
        font_ = NULL;
    }

    HippoAbstractControl::destroy();
}

int 
HippoEdit::getWidthRequestImpl()
{
    return widthReq_;
}
 
int
HippoEdit::getHeightRequestImpl(int forWidth)
{
    return heightReq_;
}

bool 
HippoEdit::filterMessage(UINT message, WPARAM wParam, LPARAM lParam)
{
    if (!listener_)
        return false;
    
    switch (message) {
    case WM_KEYDOWN:
        {
            UINT vkey = (UINT)wParam;
            HippoKey key = HIPPO_KEY_UNKNOWN;
            
            switch (vkey) {
            case VK_RETURN:
                key = HIPPO_KEY_RETURN;
                break;
            case VK_ESCAPE:
                key = HIPPO_KEY_ESCAPE;
                break;
            }

            if (key != HIPPO_KEY_UNKNOWN) {
                return listener_->onKeyPress(key, 0);
            }
        }
        break;
    case WM_UNICHAR:
        {
            if (wParam != UNICODE_NOCHAR)
                return listener_->onKeyPress(HIPPO_KEY_UNKNOWN, (gunichar)wParam);
        }
    }
     
    return false;
}

LRESULT CALLBACK 
HippoEdit::filterWindowProc(HWND   window,
                            UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    HippoEdit *edit = hippoGetWindowData<HippoEdit>(window);

    // Paranoia
    if (!edit)
        return DefWindowProc(window, message, wParam, lParam);

    if (edit->filterMessage(message, wParam, lParam))
        return 0;

    return CallWindowProc(edit->oldWindowProc_, window, message, wParam, lParam);
}
