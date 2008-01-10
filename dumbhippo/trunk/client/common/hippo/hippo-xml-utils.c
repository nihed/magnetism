/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdarg.h>
#include <string.h>
#include "hippo-basics.h"
#include "hippo-data-cache.h"
#include "hippo-group.h"
#include "hippo-xml-utils.h"

GQuark
hippo_xml_error_quark (void)
{
    static GQuark q = 0;
    if (q == 0)
        q = g_quark_from_static_string ("hippo-xml-error-quark");

    return q;
}

static gboolean
parse_boolean(const char *s,
              gboolean   *result)
{
    if (strcmp(s, "true") == 0) {
        *result = TRUE;
        return TRUE;
    } else if (strcmp(s, "false") == 0) {
        *result = FALSE;
        return TRUE;
    }

    return FALSE;
}

static gboolean
validate_absolute_uri(const char *s)
{
    /* We trust the server, so this is mostly a question of
     * robustness.
     */
    if (*s == '/') {
        g_warning("URI '%s' is supposed to be absolute", s);
    }
    
    return TRUE;
}

static gboolean
validate_relative_uri(const char *s)
{
    /* We trust the server, so this is mostly a question of
     * robustness.
     */
    if (*s != '/') {
        g_warning("URI '%s' is supposed to be relative", s);
    }
    
    return TRUE;
}

static gboolean
validate_either_uri(const char *s)
{
    /* FIXME do something ? */
    
    return TRUE;
}


typedef struct {
    const char *attribute_name;
    HippoSplitFlags flags;
    void *location;
    gboolean found;
} HippoSplitInfo;


/* This keeps people from setting a default value until after the xml split, but it
 * also catches an easy/common bug
 */
#define CHECK_NULL_IF_OPTIONAL(info, type) do {                         \
    if (info->flags & HIPPO_SPLIT_OPTIONAL) {                           \
        if (*((type *) (info->location)) != NULL)                       \
            g_warning("HIPPO_SPLIT_OPTIONAL given but type %s for attr '%s' not initialized to NULL", \
                      #type, info->attribute_name);                     \
    }                                                                   \
} while(0)

static gboolean
hippo_xml_split_process_value(HippoDataCache  *cache,
                              const char      *node_name,
                              HippoSplitInfo  *info,
                              const char      *value,
                              GError         **error)
{
    HippoEntity *entity;
    
    switch (info->flags & HIPPO_SPLIT_TYPE_MASK) {
    case HIPPO_SPLIT_NODE:
        g_assert_not_reached();
        break;
    case HIPPO_SPLIT_STRING:
        CHECK_NULL_IF_OPTIONAL(info, char*);
        *(const char **)info->location = value;
        break;
    case HIPPO_SPLIT_INT32:
        if (!hippo_parse_int32(value, (int *)info->location)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a valid 32-bit value",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        break;
    case HIPPO_SPLIT_INT64:
        if (!hippo_parse_int64(value, (gint64 *)info->location)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a valid 64-bit value",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        break;
    case HIPPO_SPLIT_BOOLEAN:
        if (!parse_boolean(value, (gboolean *)info->location)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,

                        "Value '%s' for attribute '%s' of node <%s/> is not a valid boolean value",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        break;
    case HIPPO_SPLIT_TIME_MS:
        if (!hippo_parse_int64(value, (gint64 *)info->location)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a valid millisecond timestamp",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        break;
    case HIPPO_SPLIT_GUID:
        CHECK_NULL_IF_OPTIONAL(info, char*);

        if (!hippo_verify_guid(value)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a GUID",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        *(const char **)info->location = value;
        break;
    case HIPPO_SPLIT_ENTITY:
    case HIPPO_SPLIT_GROUP:
    case HIPPO_SPLIT_PERSON:
        if (!cache)
            g_error("HIPPO_SPLIT_ENTITY used without passing in a HippoDataCache");

        CHECK_NULL_IF_OPTIONAL(info, HippoEntity*);
        
        if (!hippo_verify_guid(value)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a GUID",
                        value, info->attribute_name, node_name);
            return FALSE;
        }

        entity = NULL; /* hippo_data_cache_lookup_entity(cache, value); */
        if (!entity) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a entity we know about",
                        value, info->attribute_name, node_name);
            return FALSE;
        }

        if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_PERSON) {
            if (!HIPPO_IS_PERSON(entity)) {
                g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                            "Value '%s' for attribute '%s' of node <%s/> doesn't point to a user",
                            value, info->attribute_name, node_name);
                return FALSE;
            }
        } else if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_GROUP) {
            if (!HIPPO_IS_GROUP(entity)) {
                g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                            "Value '%s' for attribute '%s' of node <%s/> doesn't point to a group",
                            value, info->attribute_name, node_name);
                return FALSE;
            }
        }
        
        *(HippoEntity **)info->location = entity;
        break;
    case HIPPO_SPLIT_URI_ABSOLUTE:
        CHECK_NULL_IF_OPTIONAL(info, char*);
        
        if (!validate_absolute_uri(value)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not an absolute URI",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        *(const char **)info->location = value;
        break;
    case HIPPO_SPLIT_URI_RELATIVE:
        CHECK_NULL_IF_OPTIONAL(info, char*);
        
        if (!validate_relative_uri(value)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a relative URI",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        *(const char **)info->location = value;
        break;
    case HIPPO_SPLIT_URI_EITHER:
        CHECK_NULL_IF_OPTIONAL(info, char*);
        
        if (!validate_either_uri(value)) {
            g_set_error(error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                        "Value '%s' for attribute '%s' of node <%s/> is not a URI",
                        value, info->attribute_name, node_name);
            return FALSE;
        }
        *(const char **)info->location = value;
        break;        
    case HIPPO_SPLIT_SET:
        *(gboolean *)info->location = TRUE;
        break;
    default:
        g_error("Unknown split type %d\n", info->flags & HIPPO_SPLIT_TYPE_MASK);
    }

    info->found = TRUE;

    return TRUE;
}

#define MAX_INFO 32

gboolean
hippo_xml_split(HippoDataCache *cache,
                LmMessageNode  *node,
                GError        **error, ...)
{
    va_list vap;
    int count;
    int i;
    HippoSplitInfo infos[MAX_INFO];
    gboolean need_element_scan = FALSE;
    GError *internal_error = NULL;

    /*
     * Scan the passed in arguments and store into a local array
     */
    va_start(vap, error);

    count = 0;
    while (TRUE) {
        HippoSplitInfo *info = &infos[count];
        
        info->attribute_name = va_arg(vap, const char *);
        if (!info->attribute_name)
            break;
        info->flags = va_arg(vap, HippoSplitFlags);
        info->location = va_arg(vap, void *);
        info->found = FALSE;

        /* HIPPO_SPLIT_NODE means XML content, so must be a child element, not an attribute */
        if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_NODE)
            info->flags |= HIPPO_SPLIT_ELEMENT;
        
        /* HIPPO_SPLIT_SET is pointless without HIPPO_SPLIT_OPTIONAL */
        if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_SET)
            info->flags |= HIPPO_SPLIT_OPTIONAL;

        if (info->flags & HIPPO_SPLIT_ELEMENT)
            need_element_scan = TRUE;

        count++;
        if (count == MAX_INFO) {
            /* If you hit this, either up the MAX_INFO count, or do a prescan
             * to count and dynamically allocate.
             */
            g_error("hippo_xml_split called with too many parameters");
        }
    }

    va_end(vap);

    /*
     * Look for attributes. The O(N*M) here is necessitated only by the fact that there
     *  is no way to iterate over the attributes of a LmMessageNode.
     */
    for (i = 0; i < count; i++) {
        HippoSplitInfo *info = &infos[i];
        
        if ((info->flags & HIPPO_SPLIT_ELEMENT) == 0) {
            const char *value = lm_message_node_get_attribute(node, info->attribute_name);
            if (!value)
                continue;

            if (!hippo_xml_split_process_value(cache, node->name, info, value, &internal_error))
                goto out;
        }
    }

    /*
     * Look for child elements
     */
    if (need_element_scan) {
        LmMessageNode *child;

        for (child = node->children; child; child = child->next) {
            for (i = 0; i < count; i++) {
                HippoSplitInfo *info = &infos[i];
                if ((info->flags & HIPPO_SPLIT_ELEMENT) != 0 &&
                    !info->found &&
                    strcmp(info->attribute_name, child->name) == 0)
                {
                    if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_NODE) {
                        *((LmMessageNode **)info->location) = child;
                        info->found = TRUE;
                    } else {
                        if (child->children) {
                            g_set_error(&internal_error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_INVALID_CONTENT,
                                        "Child element <%s/> of node <%s/> has unexpected child elements",
                                        child->name, node->name);
                            goto out;
                        }
                                        
                        if (!hippo_xml_split_process_value(cache, node->name, info,
                                                           /* loudmouth has value==NULL when the node has empty content ("<foo/>") */
                                                           child->value ? child->value : "",
                                                           &internal_error))
                            goto out;
                    }
                }
            }
        }
    }

    /*
     * See if there were any mandatory values that were not passed in.
     */
    for (i = 0; i < count; i++) {
        HippoSplitInfo *info = &infos[i];
        if (!info->found) {            
            if (!(info->flags & HIPPO_SPLIT_OPTIONAL)) {
                g_set_error(&internal_error, HIPPO_XML_ERROR, HIPPO_XML_ERROR_NOT_FOUND,
                            "%s '%s' not found for node <%s/>",
                            ((info->flags & HIPPO_SPLIT_ELEMENT) != 0) ? "Child element" : "Attribute",
                            info->attribute_name,
                            node->name);
                goto out;
            } else if ((info->flags & HIPPO_SPLIT_TYPE_MASK) == HIPPO_SPLIT_SET) {
                *(gboolean *)info->location = FALSE;
            }
        }
    }

 out:
    if (internal_error) {
        if (error) {
            g_propagate_error(error, internal_error);
        } else {
            g_warning("%s", internal_error->message);
            g_error_free(internal_error);
        }

        return FALSE;
    } else {
        return TRUE;
    }
}
