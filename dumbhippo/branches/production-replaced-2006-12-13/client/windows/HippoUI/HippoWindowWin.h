#ifndef __HIPPO_WINDOW_WIN_H__
#define __HIPPO_WINDOW_WIN_H__

/* Implementation of HippoWindow for Windows */
class HippoUI;

#include <hippo/hippo-window.h>
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoWindowWin      HippoWindowWin;
typedef struct _HippoWindowWinClass HippoWindowWinClass;

#define HIPPO_TYPE_WINDOW_WIN              (hippo_window_win_get_type ())
#define HIPPO_WINDOW_WIN(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_WINDOW_WIN, HippoWindowWin))
#define HIPPO_WINDOW_WIN_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_WINDOW_WIN, HippoWindowWinClass))
#define HIPPO_IS_WINDOW_WIN(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_WINDOW_WIN))
#define HIPPO_IS_WINDOW_WIN_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_WINDOW_WIN))
#define HIPPO_WINDOW_WIN_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_WINDOW_WIN, HippoWindowWinClass))

GType            hippo_window_win_get_type               (void) G_GNUC_CONST;

HippoWindow*     hippo_window_win_new (HippoUI *ui);


G_END_DECLS

#endif /* __HIPPO_WINDOW_WIN_H__ */
