/* HippoAbstractControl.h: a "widget" (wrapper around an HWND)
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoUIUtil.h"
#include "HippoAbstractWindow.h"
#include <hippo/hippo-basics.h>

class HippoAbstractControl : public HippoAbstractWindow {
public:
    HippoAbstractControl(); 

    int getWidthRequest();
    int getHeightRequest(int forWidth);

    void getLastRequest(int *width_p, 
                        int *height_p);

    // bool isRequestChanged() { return requestChangedSinceAllocate_; }

    void markRequestChanged();

    void ensureRequestAndAllocation();

    void setParent(HippoAbstractControl *parent);

    void setResizable(HippoOrientation orientation,
                      bool             value);

    virtual bool create();

    virtual void show(bool activate);

    bool isHResizable() { return hresizable_; }
    bool isVResizable() { return vresizable_; }

    // only parent widgets should call these, and only in onSizeAllocated() implementations.
    void sizeAllocate(const HippoRectangle *rect);
    void sizeAllocate(int x, int y, int width, int height);
    // virtual so subclasses can chose a different policy for picking X/Y when spontaneously
    // resizing. The implementation should chose a new X/Y then call the 4 argument form
    virtual void sizeAllocate(int width, int height);

    // kind of a bad hack to get request-changed emitted on the wrapping canvas item
    void setCanvasItem(GObject *item);

protected:
    virtual int getWidthRequestImpl() = 0;
    virtual int getHeightRequestImpl(int forWidth) = 0;

    // convenience hook called after size allocation
    virtual void onSizeAllocated();

    // called if we're marked as size request changed
    virtual void onRequestChanged();

    virtual void onMoveResizeMessage(const HippoRectangle *newClientArea);

    virtual void showChildren();
    virtual void createChildren();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    HippoAbstractControl *parent_;

private:
    int lastWidthRequest_;
    int lastHeightRequest_;
    unsigned int hresizable_ : 1;
    unsigned int vresizable_ : 1;
    unsigned int requestChangedSinceRequest_ : 1;
    unsigned int requestChangedSinceAllocate_ : 1;
    unsigned int insideAllocation_ : 1;
    GObject *canvasItem_;
};
