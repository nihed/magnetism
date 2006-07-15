#ifndef __HIPPO_PERSON_RENDERER_H__
#define __HIPPO_PERSON_RENDERER_H__

#include <hippo/hippo-common.h>
#include <gtk/gtkcellrenderer.h>

G_BEGIN_DECLS

typedef struct _HippoPersonRenderer      HippoPersonRenderer;
typedef struct _HippoPersonRendererClass HippoPersonRendererClass;

#define HIPPO_TYPE_PERSON_RENDERER              (hippo_person_renderer_get_type ())
#define HIPPO_PERSON_RENDERER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PERSON_RENDERER, HippoPersonRenderer))
#define HIPPO_PERSON_RENDERER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PERSON_RENDERER, HippoPersonRendererClass))
#define HIPPO_IS_PERSON_RENDERER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PERSON_RENDERER))
#define HIPPO_IS_PERSON_RENDERER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PERSON_RENDERER))
#define HIPPO_PERSON_RENDERER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_PERSON_RENDERER, HippoPersonRendererClass))

GType          	     hippo_person_renderer_get_type               (void) G_GNUC_CONST;

GtkCellRenderer*     hippo_person_renderer_new                (void);

G_END_DECLS

#endif /* __HIPPO_PERSON_RENDERER_H__ */
