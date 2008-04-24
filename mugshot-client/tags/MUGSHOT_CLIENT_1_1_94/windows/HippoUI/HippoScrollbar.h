/* HippoScrollbar.h: a control that contains a standard Windows scrollbar
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"

class HippoScrollbar : public HippoAbstractControl {
public:
    HippoScrollbar();

    virtual bool create();

    void setOrientation(HippoOrientation orientation);
    void setBounds(int minPos, int maxPos, int pageSize);

    int handleScrollMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onSizeChanged();

private:
    HippoOrientation orientation_;
    int minPos_;
    int maxPos_;
    int pageSize_;

    int widthReq_;
    int heightReq_;

    void syncBounds();

    int getScrollIncrement(int increment, double count, int currentPos);
    void clampPosition(int *posPtr);

    int handleDragMessage  (UINT   message,
                            WPARAM wParam,
                            LPARAM lParam);
    int handleWheelMessage (UINT   message,
                            WPARAM wParam,
                            LPARAM lParam);
};
