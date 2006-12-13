/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_MYSPACE_PERSON_H__
#define __HIPPO_CANVAS_BLOCK_MYSPACE_PERSON_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockMySpacePerson      HippoCanvasBlockMySpacePerson;
typedef struct _HippoCanvasBlockMySpacePersonClass HippoCanvasBlockMySpacePersonClass;

#define HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON              (hippo_canvas_block_myspace_person_get_type ())
#define HIPPO_CANVAS_BLOCK_MYSPACE_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON, HippoCanvasBlockMySpacePerson))
#define HIPPO_CANVAS_BLOCK_MYSPACE_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON, HippoCanvasBlockMySpacePersonClass))
#define HIPPO_IS_CANVAS_BLOCK_MYSPACE_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON))
#define HIPPO_IS_CANVAS_BLOCK_MYSPACE_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON))
#define HIPPO_CANVAS_BLOCK_MYSPACE_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_MYSPACE_PERSON, HippoCanvasBlockMySpacePersonClass))

GType            hippo_canvas_block_myspace_person_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_MYSPACE_PERSON_H__ */
