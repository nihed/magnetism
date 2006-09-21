/* HippoCanvas.h: a control that contains a canvas item
 *
 * Copyright Red Hat, Inc. 2006
 **/

#pragma once

#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include <hippo/hippo-canvas-item.h>

typedef struct _HippoCanvasContextWin      HippoCanvasContextWin;

class HippoCanvas : public HippoAbstractControl {
public:
    HippoCanvas(); 

    virtual bool create();

    void setRoot(HippoCanvasItem *item);

protected:
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

private:
    HippoGObjectPtr<HippoCanvasItem> root_;
    HippoGObjectPtr<HippoCanvasContextWin> context_;
};
