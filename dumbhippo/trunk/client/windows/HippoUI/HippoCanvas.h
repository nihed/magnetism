/* HippoCanvas.h: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include <hippo/hippo-canvas-item.h>

typedef struct _HippoCanvasContextWin      HippoCanvasContextWin;
class HippoScrollbar;

class HippoCanvas : public HippoAbstractControl {
public:
    HippoCanvas(); 

    virtual bool create();
    virtual void show(bool activate);

    void setRoot(HippoCanvasItem *item);
    void setScrollable(HippoOrientation orientation,
                       bool value);

    void getCanvasOrigin(int *x_p, int *y_p);
    void getViewport(RECT *rect_p);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onSizeChanged();

private:
    void updateScrollbars();
    void scrollTo(int newX, int newY);

    HippoGObjectPtr<HippoCanvasItem> root_;
    HippoGObjectPtr<HippoCanvasContextWin> context_;
    HippoPtr<HippoScrollbar> hscroll_;
    HippoPtr<HippoScrollbar> vscroll_;
    int canvasWidthReq_;
    int canvasHeightReq_;
    int canvasX_;
    int canvasY_;
    unsigned int hscrollNeeded_ : 1;
    unsigned int vscrollNeeded_ : 1;
    unsigned int hscrollable_ : 1;
    unsigned int vscrollable_ : 1;
};
