/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoCanvas.h: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include "HippoGSignal.h"
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-widgets.h>

typedef struct _HippoCanvasContextWin      HippoCanvasContextWin;
class HippoScrollbar;
class HippoToolTip;

class HippoCanvas : public HippoAbstractControl {
public:
    HippoCanvas(); 

    void setRoot(HippoCanvasItem *item);
    void setScrollbarPolicy(HippoOrientation     orientation,
                            HippoScrollbarPolicy policy);

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

    virtual void initializeUI();

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
    void onHover(WPARAM wParam, LPARAM lParam);
    void startTrackingHover();

    // Helper routines for size allocation
    int computeChildWidthRequest();
    int computeChildHeightRequest(int forWidth);
    void clearCachedChildHeights();
    int getChildHeightRequest(bool hscrollbar, bool vscrollbar);
    int getCanvasHeightRequest(bool hscrollbar, bool vscrollbar);
    bool tryAllocate(bool hscrollbar, bool vscrollbar);

    GConnection1<void,const HippoRectangle*> rootPaintNeeded_;
    GConnection0<void> rootRequestChanged_;
    HippoGObjectPtr<HippoCanvasItem> root_;
    HippoGObjectPtr<HippoCanvasContextWin> context_;
    HippoPtr<HippoScrollbar> hscroll_;
    HippoPtr<HippoScrollbar> vscroll_;
    HippoPtr<HippoToolTip> tooltip_;

    // We keep state across the process of getWidthRequest() =>
    // => getHeightRequest() => onSizeAllocate() to avoid asking our child
    // the same question twice; one reason for that is efficiency, but it
    // also reduces the chance we'll get confused by inconsistent answers.

    // Result of child->getWidthRequest(), set in getWidthRequest()
    int childWidthReq_;
    // Our computed minimum width, based on childWidthReq_
    int canvasWidthReq_; 
    
    // Value of 'forWidth' passed to the last call to getHeightRequest(), the variables
    // that follow are dependent on this
    int currentWidth_;
    // Child height requests depending on the scrollbar states, arranged as
    // childHeightRequest[hscrollbarVisible][vscrollbarVisible]
    int childHeightReq_[2][2];
    // Our computed minimum height
    int canvasHeightReq_;

    int canvasX_;
    int canvasY_;
    
    HippoCanvasPointer pointer_;
    unsigned int hscrollNeeded_ : 1;
    unsigned int vscrollNeeded_ : 1;
    unsigned int containsMouse_ : 1;
    HippoScrollbarPolicy hscrollbarPolicy_;
    HippoScrollbarPolicy vscrollbarPolicy_;

    int lastMoveX_;
    int lastMoveY_;
};
