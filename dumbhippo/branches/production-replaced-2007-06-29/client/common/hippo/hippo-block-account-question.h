/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_ACCOUNT_QUESTION_H__
#define __HIPPO_BLOCK_ACCOUNT_QUESTION_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockAccountQuestion      HippoBlockAccountQuestion;
typedef struct _HippoBlockAccountQuestionClass HippoBlockAccountQuestionClass;

typedef struct _HippoAccountQuestionButton     HippoAccountQuestionButton;

#define HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION              (hippo_block_account_question_get_type ())
#define HIPPO_BLOCK_ACCOUNT_QUESTION(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION, HippoBlockAccountQuestion))
#define HIPPO_BLOCK_ACCOUNT_QUESTION_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION, HippoBlockAccountQuestionClass))
#define HIPPO_IS_BLOCK_ACCOUNT_QUESTION(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION))
#define HIPPO_IS_BLOCK_ACCOUNT_QUESTION_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION))
#define HIPPO_BLOCK_ACCOUNT_QUESTION_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_ACCOUNT_QUESTION, HippoBlockAccountQuestionClass))

GType            hippo_block_account_question_get_type               (void) G_GNUC_CONST;

const char *hippo_account_question_button_get_text    (HippoAccountQuestionButton *button);
const char *hippo_account_question_button_get_response(HippoAccountQuestionButton *button);

G_END_DECLS

#endif /* __HIPPO_BLOCK_ACCOUNT_QUESTION_H__ */
