/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_TITLE_PATTERN_H__
#define __HIPPO_TITLE_PATTERNH__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct HippoTitlePattern HippoTitlePattern;

HippoTitlePattern *hippo_title_pattern_new        (const char        *app_id,
                                                   const char        *pattern_string);
void               hippo_title_pattern_free       (HippoTitlePattern *pattern);
gboolean           hippo_title_pattern_matches    (HippoTitlePattern *pattern,
                                                   const char        *title);
const char        *hippo_title_pattern_get_app_id (HippoTitlePattern *pattern);

G_END_DECLS

#endif /* __HIPPO_TITLE_PATTERN_H__ */
