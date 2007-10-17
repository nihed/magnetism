/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_JSON_H__
#define __HIPPO_JSON_H__

G_BEGIN_DECLS

char** json_array_split  (const char  *s,
                          GError     **error);
char*  json_string_parse (const char  *s,
                          GError     **error);

G_END_DECLS

#endif /* __HIPPO_JSON_H__ */
