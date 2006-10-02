/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_XML_UTILS_H__
#define __HIPPO_XML_UTILS_H__

#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

#define HIPPO_XML_ERROR hippo_xml_error_quark ()

typedef enum
{
    HIPPO_XML_ERROR_NOT_FOUND,
    HIPPO_XML_ERROR_INVALID_CONTENT
} HippoXmlError;

typedef enum {                                              /* Destination type */
    HIPPO_SPLIT_NODE     = 0,  /* Structured XML content:   LmMessageNode *         */
    HIPPO_SPLIT_STRING   = 1,  /* Uninterpreted string:     const char *            */
    HIPPO_SPLIT_INT32    = 2,  /* Integer:                  int                     */
    HIPPO_SPLIT_INT64    = 3,  /* 64-bit integer:           int64                   */
    HIPPO_SPLIT_BOOLEAN  = 4,  /* true|false                gboolean                */
    HIPPO_SPLIT_TIME_MS  = 5,  /* milliseconds since epoch  int64                   */
    HIPPO_SPLIT_GUID     = 6,  /* DumbHippo GUID            const char *            */
    HIPPO_SPLIT_URI      = 7,  /* Any URI                   const char *            */

    HIPPO_SPLIT_TYPE_MASK = 0x000f,
    
    HIPPO_SPLIT_OPTIONAL  = 0x0010,  /* Value is optional */
    HIPPO_SPLIT_ELEMENT   = 0x0020   /* Child element, not attribute (forced on by HIPPO_SPLIT_NODE) */
} HippoSplitFlags;

/**
 * hippo_xml_split:
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
 * hippo_xml_split(node, NULL,
 *                 "id", HIPPO_SPLIT_GUID, &guid,
 *                 "count", HIPPO_SPLIT_INT32 | HIPPO_SPLIT_OPTIONAL, &count,
 *                 "title", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &title,
 *                 NULL);
 * 
 * Return value: %TRUE if value extraction succeeded, otherwise FALSE
 **/
gboolean hippo_xml_split(LmMessageNode *node, GError **error, ...);

GQuark hippo_xml_error_quark (void);

G_END_DECLS

#endif /* __HIPPO_XML_UTIL_H__ */
