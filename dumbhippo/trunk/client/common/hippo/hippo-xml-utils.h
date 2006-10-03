/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_XML_UTILS_H__
#define __HIPPO_XML_UTILS_H__

#include <hippo/hippo-basics.h>
#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

#define HIPPO_XML_ERROR hippo_xml_error_quark ()

typedef enum
{
    HIPPO_XML_ERROR_NOT_FOUND,
    HIPPO_XML_ERROR_INVALID_CONTENT
} HippoXmlError;

typedef enum {                                              /* Destination type */
    HIPPO_SPLIT_SET      = 0,  /* Value present at all      gboolean                */
    HIPPO_SPLIT_NODE     = 1,  /* Structured XML content:   LmMessageNode *         */
    HIPPO_SPLIT_STRING   = 2,  /* Uninterpreted string:     const char *            */
    HIPPO_SPLIT_INT32    = 3,  /* Integer:                  int                     */
    HIPPO_SPLIT_INT64    = 4,  /* 64-bit integer:           int64                   */
    HIPPO_SPLIT_BOOLEAN  = 5,  /* true|false                gboolean                */
    HIPPO_SPLIT_TIME_MS  = 6,  /* milliseconds since epoch  int64                   */
    HIPPO_SPLIT_GUID     = 7,  /* DumbHippo GUID            const char *            */
    HIPPO_SPLIT_URI      = 8,  /* Any URI                   const char *            */
    HIPPO_SPLIT_ENTITY   = 9,  /* DumbHippo entity          HippoEntity *           */
    HIPPO_SPLIT_PERSON   = 10, /* DumbHippo user            HippoPerson *           */
    HIPPO_SPLIT_POST     = 11, /* Post                      HippoPost *             */

    HIPPO_SPLIT_TYPE_MASK = 0x001f,
    
    HIPPO_SPLIT_OPTIONAL  = 0x0020,  /* Value is optional (forced on by HIPPO_SPLIT_SET)  */
    HIPPO_SPLIT_ELEMENT   = 0x0040   /* Child element, not attribute (forced on by HIPPO_SPLIT_NODE) */
} HippoSplitFlags;

/**
 * hippo_xml_split:
 * @cache: Data cache; used to resolve values of type HIPPO_SPLIT_ENTITY, etc. Can be NULL if none such
 * @node: Node to extract values from
 * @error: GEror to store any resulting error message in or NULL to send errors to g_warning()
 * 
 * Utility function to extract values from an LmMessageNode. The values can either be
 * provided as attributes or as child elements (you have to specify which: its not automatic)
 *
 * Call like:
 *
 * const char *guid;
 * int count = 42;
 * const const *title;
 *
 * hippo_xml_split(cache, node, NULL,
 *                 "id", HIPPO_SPLIT_GUID, &guid,
 *                 "count", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_OPTIONAL, &count,
 *                 "title", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &title,
 *                 NULL);
 * 
 * Return value: %TRUE if value extraction succeeded, otherwise FALSE
 **/
gboolean hippo_xml_split(HippoDataCache *cache,
                         LmMessageNode  *node,
                         GError        **error, ...);

GQuark hippo_xml_error_quark (void);

G_END_DECLS

#endif /* __HIPPO_XML_UTIL_H__ */
