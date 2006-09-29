/* HippoCanvas.h: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include "HippoGSignal.h"
#include <hippo/hippo-canvas-item.h>

typedef struct _HippoCanvasContextWin      HippoCanvasContextWin;
class HippoScrollbar;

class HippoCanvas : public HippoAbstractControl {
public:
    HippoCanvas(); 

    void setRoot(HippoCanvasItem *item);
    void setScrollable(HippoOrientation orientation,
                       bool value);

    void getCanvasOrigin(int *x_p, int *y_p);
    void getViewport(RECT *rect_p);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    virtual void createChildren();
    virtual void showChildren();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onSizeAllocated();

private:
    void updateScrollbars();
    void scrollTo(int newX, int newY);
    bool getMouseCoords(LPARAM lParam, int *x_p, int *y_p);
    void updatePointer(int rootItemX, int rootItemY);
    void onPaint(WPARAM wParam, LPARAM lParam);
    void onMouseDown(int button, WPARAM wParam, LPARAM lParam);
    void onMouseUp(int button, WPARAM wParam, LPARAM lParam);
    void onMouseMove(WPARAM wParam, LPARAM lParam);
    void onMouseLeave(WPARAM wParam, LPARAM lParam);
    void onRootRequestChanged();
    void onRootPaintNeeded(const HippoRectangle *damage_box);

    GConnection1<void,const HippoRectangle*> rootPaintNeeded_;
    GConnection0<void> rootRequestChanged_;
    HippoGObjectPtr<HippoCanvasItem> root_;
    HippoGObjectPtr<HippoCanvasContextWin> context_;
    HippoPtr<HippoScrollbar> hscroll_;
    HippoPtr<HippoScrollbar> vscroll_;
    int canvasWidthReq_;
    int canvasHeightReq_;
    int canvasX_;
    int canvasY_;
    HippoCanvasPointer pointer_;
    unsigned int hscrollNeeded_ : 1;
    unsigned int vscrollNeeded_ : 1;
    unsigned int hscrollable_ : 1;
    unsigned int vscrollable_ : 1;
    unsigned int containsMouse_ : 1;
};
