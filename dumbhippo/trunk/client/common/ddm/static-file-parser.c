/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <string.h>

#include "static-file-backend.h"

/*
 * This file implements loading a data model with data from an XML file on disk.
 * The file structure we are parsing is:
 *
 * <m:model m:xmlns="http://mugshot.org/p/system" m:resourceBase="http://mugshot/o/">
 *     <resource xmlns="http://mugshot.org/p/o/user" resourceId="user/l31fag3qErqwqA">
 *          <name m:type="+s">John Doe</name>
 *          <externalAccounts m:type="+r?" defaultChildren="+" resourceId="externalAccount/l31fag3qErqwqA.MYSPACE"/>
 *          <contacts m:type="r*" m:resourceId="user/51FAj31daP8d12"/>
 *          <contacters m:type="r*" m:update="clear"/>
 *     </resource>
 * </model>
 *
 * This is basically the same as the wire protocol XML format with the following modifications:
 *  - We have an enclosing m:model element
 *  - The default update is "add" not "replace"
 *  - Only updates of "add" and "clear" are allowed
 *
 * The intent here is testing; if you wanted to use a dump for caching, you'd need to
 * store more information to track how the data was fetched, not just raw results
 * (The online data engine includes an implementation of caching via SQLite.)
 */

#define SYSTEM_NAMESPACE "http://mugshot.org/p/system"

typedef enum {
    STATE_OUTSIDE,
    STATE_MODEL,
    STATE_RESOURCE,
    STATE_PROPERTY
} SFParseState;

typedef enum {
    NAME_INVALID,
    NAME_DEFAULT_CHILDREN,
    NAME_MODEL,
    NAME_RESOURCE_BASE,
    NAME_RESOURCE_ID,
    NAME_TYPE,
    NAME_UPDATE,
    LAST_NAME
} SFName;

typedef struct {
    DDMQName *name;
    GSList *namespaces;
    
    const char *resource_base;
    const char *default_namespace;

    int n_attributes;
    char **attribute_names;
    char **attribute_values;
    DDMQName **attribute_qnames;
} SFElement;

typedef struct {
    DDMDataModel *model;
    
    SFParseState state;
    GSList *elements;
    SFElement *element;
    
    DDMQName *names[LAST_NAME];

    DDMDataResource *current_resource;
    DDMQName *current_property_id;
    DDMDataUpdate current_update;
    DDMDataType current_type;
    DDMDataCardinality current_cardinality;
    gboolean current_default_include;
    char *current_default_children;

    GString *value;
} SFParseInfo;

typedef struct {
    const char *prefix;
    const char *uri;
} SFNamespace;

static void
sf_parse_info_init(SFParseInfo *info,
                   DDMDataModel *model)
{
    info->model = model;
    info->state = STATE_OUTSIDE;

    info->elements = NULL;
    info->element = NULL;

    info->names[NAME_INVALID] = NULL;
    info->names[NAME_DEFAULT_CHILDREN] = ddm_qname_get(SYSTEM_NAMESPACE, "defaultChildren");
    info->names[NAME_MODEL] = ddm_qname_get(SYSTEM_NAMESPACE, "model");
    info->names[NAME_RESOURCE_BASE] = ddm_qname_get(SYSTEM_NAMESPACE, "resourceBase");
    info->names[NAME_RESOURCE_ID] = ddm_qname_get(SYSTEM_NAMESPACE, "resourceId");
    info->names[NAME_TYPE] = ddm_qname_get(SYSTEM_NAMESPACE, "type");
    info->names[NAME_UPDATE] = ddm_qname_get(SYSTEM_NAMESPACE, "update");

    info->current_resource = NULL;
    info->current_property_id = NULL;
    info->current_default_children = NULL;
    
    info->value = NULL;
}

static gboolean
sf_parse_info_decode(SFParseInfo  *info,
                     const char   *prefixed_name,
                     const char  **uri,
                     const char  **name)
{
    GSList *l;
    
    const char *colon = strchr(prefixed_name, ':');
    if (colon == NULL) {

        for (l = info->elements; l; l = l->next) {
            SFElement *element = l->data;
            
            if (element->default_namespace != NULL) {
                *uri = element->default_namespace;
                *name = prefixed_name;
                
                return TRUE;
            }
        }

        /* In general, we'd have to resolve this issue and handle namespace
         * less elements (DDMQName can't do that), but there is no
         * legitimate use of namespace-less elements in the files we are
         * parsing.
         */
        return FALSE;

    } else {
        int prefix_len = colon - prefixed_name;
        GSList *l2;

        for (l = info->elements; l; l = l->next) {
            SFElement *element = l->data;
        
            for (l2 = element->namespaces; l2; l2 = l2->next) {
                SFNamespace *namespace = l2->data;

                if (strncmp(namespace->prefix, prefixed_name, prefix_len) == 0 &&
                    namespace->prefix[prefix_len] == '\0')
                {
                    *uri = namespace->uri;
                    *name = colon + 1;
                    
                    return TRUE;
                }
            }
        }
    }

    return FALSE;
}

static gboolean
sf_parse_info_extract_attrs (SFParseInfo  *info,
                             GError      **error,
                             ...)
{
    SFElement *element = info->element;
    
    gboolean *attr_map = g_new0 (gboolean, element->n_attributes);
    gboolean return_value = FALSE;
    va_list vap;
    int i;

    va_start (vap, error);
    while (TRUE) {
        SFName name = va_arg (vap, SFName);
        gboolean mandatory;
        const char **loc;
        gboolean found = FALSE;
        
        if (name == NAME_INVALID)
            break;
        
        mandatory = va_arg (vap, gboolean);
        loc = va_arg (vap, const char **);

        for (i = 0; element->attribute_qnames[i]; i++) {
            if (!attr_map[i] && element->attribute_qnames[i] == info->names[name]) {
                if (found) {
                    g_set_error (error,
                                 G_MARKUP_ERROR,
                                 G_MARKUP_ERROR_INVALID_CONTENT,
                                 "Duplicate attribute '%s' for element <%s/>",
                                 info->names[name]->name, element->name->name);
                    goto error;
                }
	  
                *loc = element->attribute_values[i];
                found = TRUE;
                attr_map[i] = TRUE;
            }
        }
      
        if (!found && mandatory) {
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Missing attribute '%s'for element <%s/>",
                         info->names[name]->name,
                         element->name->name);
            goto error;
        }
        else if (!found)
            *loc = NULL;
    }

    for (i = 0; i < element->n_attributes; i++)
        if (!attr_map[i] &&
            strcmp(element->attribute_names[i], "xmlns") != 0 &&
            !g_str_has_prefix(element->attribute_names[i], "xmlns:"))
        {
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_UNKNOWN_ATTRIBUTE,
                         "Unknown attribute '%s' for element <%s/>",
                         element->attribute_names[i], element->name->name);
            goto error;
        }

    return_value = TRUE;

 error:
    g_free(attr_map);
    
    return return_value;
}

static void
sf_parse_info_pop_element(SFParseInfo *info)
{
    SFElement *element = info->element;
    GSList *l;
    
    info->elements = g_slist_delete_link(info->elements, info->elements);
    if (info->elements)
        info->element = info->elements->data;
    else
        info->element = NULL;

    for (l = element->namespaces; l; l = l->next)
        g_slice_free(SFNamespace, l->data);
    
    g_slist_free(element->namespaces);

    g_strfreev(element->attribute_names);
    g_strfreev(element->attribute_values);
    g_free(element->attribute_qnames);

    g_slice_free(SFElement, element);
}

static gboolean
sf_parse_info_push_element(SFParseInfo         *info,
                           const gchar         *element_name,
			   const gchar        **attribute_names,
			   const gchar        **attribute_values,
                           GError             **error)
{
    SFElement *element;
    const char *uri;
    const char *name;
    int i = 0;

    element = g_slice_new(SFElement);

    element->namespaces = NULL;
    element->resource_base = NULL;
    element->default_namespace = NULL;
    element->attribute_names = g_strdupv((char **)attribute_names);
    element->attribute_qnames = NULL;
    element->attribute_values = g_strdupv((char **)attribute_values);
    
    for (i = 0; element->attribute_names[i]; i++) {
        if (strcmp(element->attribute_names[i], "xmlns") == 0) {
            element->default_namespace = element->attribute_values[i];
        } else if (g_str_has_prefix(attribute_names[i], "xmlns:")) {
            SFNamespace *namespace = g_slice_new(SFNamespace);
            namespace->prefix = element->attribute_names[i] + 6;
            namespace->uri = element->attribute_values[i];

            element->namespaces = g_slist_prepend(element->namespaces, namespace);
        }
    }

    element->n_attributes = i;

    if (element->default_namespace == NULL && info->elements != NULL) {
        SFElement *parent = info->elements->data;

        element->default_namespace = parent->default_namespace;
    }

    info->elements = g_slist_prepend(info->elements, element);
    info->element = element;
    
    if (!sf_parse_info_decode(info, element_name, &uri, &name)) {
        g_set_error (error,
                     G_MARKUP_ERROR,
                     G_MARKUP_ERROR_INVALID_CONTENT,
                     "Can't resolve namespace for <%s/>",
                     element_name);
        
        sf_parse_info_pop_element(info);
        return FALSE;
    }

    element->name = ddm_qname_get(uri, name);

    element->attribute_qnames = g_new0(DDMQName *, element->n_attributes);

    for (i = 0; element->attribute_names[i]; i++) {
        const char *attribute_uri;
        const char *attribute_name;

        if (sf_parse_info_decode(info, attribute_names[i],
                                 &attribute_uri, &attribute_name)) {
            element->attribute_qnames[i] = ddm_qname_get(attribute_uri, attribute_name);

            if (element->attribute_qnames[i] == info->names[NAME_RESOURCE_BASE]) {
                element->resource_base = element->attribute_values[i];
            }
        }
    }
    
    if (element->resource_base == NULL && info->elements->next != NULL) {
        SFElement *parent = info->elements->next->data;

        element->resource_base = parent->resource_base;
    }

    return TRUE;
}

static void
sf_parse_info_finish(SFParseInfo *info)
{
    while (info->elements)
        sf_parse_info_pop_element(info);

    if (info->current_default_children != NULL)
        g_free(info->current_default_children);
    if (info->value != NULL)
        g_string_free(info->value, TRUE);
}

static gboolean
uri_is_absolute(const char *uri)
{
    const char *p;

    for (p = uri; *p; p++) {
        char c = *p;
        if (!((c >= 'A' && c <= 'Z') ||
              (c >= 'a' && c <= 'z') ||
              (c >= '0' && c <= '9') ||
              (c == '.') ||
              (c == '+') ||
              (c == '-')))
            break;
    }

    return *p == ':';
}

static char *
get_absolute_resource_id(SFParseInfo *info,
                         const char  *maybe_relative,
                         GError     **error)
{
    if (uri_is_absolute(maybe_relative)) {
        return g_strdup(maybe_relative);
    } else {
        if (info->element->resource_base == NULL) {
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "relative resource_id with no resource base");
            return NULL;
        }
        
        /* Really should do full relative URI resolution, but I don't
         * want to write that one-off here.
         *
         * http://bugzilla.gnome.org/show_bug.cgi?id=489862
         */
        return g_strconcat(info->element->resource_base, maybe_relative, NULL);
    }
}

static void
static_file_start_element (GMarkupParseContext *context,
			   const gchar         *element_name,
			   const gchar        **attribute_names,
			   const gchar        **attribute_values,
			   gpointer             user_data,
			   GError             **error)
{
    SFParseInfo *info = user_data;
    SFElement *element;

    if (!sf_parse_info_push_element(info, element_name,
                                    attribute_names, attribute_values,
                                    error))
        return;

    element = info->element;
    
    switch (info->state) {
    case STATE_OUTSIDE:
        if (element->name != info->names[NAME_MODEL]) {
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Unexpected element <%s/> as root element",
                         element_name);
            return;
        }
        info->state = STATE_MODEL;
        break;
    case STATE_MODEL:
        {
            const char *resource_id;
            char *absolute_resource_id;

            if (strcmp(element->name->name, "resource") != 0) {
                g_set_error (error,
                             G_MARKUP_ERROR,
                             G_MARKUP_ERROR_INVALID_CONTENT,
                             "Unexpected element <%s/> inside <model/>",
                             element_name);
            }
            
            if (!sf_parse_info_extract_attrs(info, error,
                                             NAME_RESOURCE_ID, TRUE, &resource_id,
                                             NAME_INVALID))
                return;

            absolute_resource_id = get_absolute_resource_id(info, resource_id, error);
            if (!absolute_resource_id)
                return;
                
            info->current_resource = ddm_data_model_ensure_resource(info->model, absolute_resource_id,
                                                                    element->name->uri);

            g_free(absolute_resource_id);
            
            info->state = STATE_RESOURCE;
            break;
        }
    case STATE_RESOURCE:
        {
            const char *default_children = NULL;
            const char *resource_id = NULL;
            const char *type = NULL;
            const char *update = NULL;

            info->current_property_id = element->name;

            if (!sf_parse_info_extract_attrs(info, error,
                                             NAME_TYPE,             TRUE,  &type,
                                             NAME_UPDATE,           FALSE, &update,
                                             NAME_DEFAULT_CHILDREN, FALSE, &default_children,
                                             NAME_RESOURCE_ID,      FALSE, &resource_id,
                                             NAME_INVALID))
                return;

            if (!ddm_data_parse_type(type,
                                     &info->current_type,
                                     &info->current_cardinality,
                                     &info->current_default_include)) {
                g_set_error (error,
                             G_MARKUP_ERROR,
                             G_MARKUP_ERROR_INVALID_CONTENT,
                             "Bad type string '%s'",
                             type);
                return;
            }

            if (update == NULL ||
                strcmp(update, "add") == 0)
                info->current_update = DDM_DATA_UPDATE_ADD;
            else if (strcmp(update, "clear") == 0)
                info->current_update = DDM_DATA_UPDATE_CLEAR;
            else {
                g_set_error (error,
                             G_MARKUP_ERROR,
                             G_MARKUP_ERROR_INVALID_CONTENT,
                             "Unknown or unsupported update type '%s'",
                             type);
                return;
            }

            if (info->current_type == DDM_DATA_RESOURCE) {
                if (info->current_update == DDM_DATA_UPDATE_CLEAR) {
                    if (resource_id != NULL) {
                        g_set_error (error,
                                     G_MARKUP_ERROR,
                                     G_MARKUP_ERROR_INVALID_CONTENT,
                                     "resource_id attribute not allowed for update of type 'clear'");
                        return;
                    }
                } else {
                    char *absolute_resource_id;
                    
                    if (resource_id == NULL) {
                        g_set_error (error,
                                     G_MARKUP_ERROR,
                                     G_MARKUP_ERROR_INVALID_CONTENT,
                                     "resource_id required for resource-typed properties");
                        return;
                    }

                    absolute_resource_id = get_absolute_resource_id(info, resource_id, error);
                    if (!absolute_resource_id)
                        return;

                    
                    info->value = g_string_new(absolute_resource_id);
                    g_free(absolute_resource_id);
                }
            } else {
                if (resource_id != NULL) {
                    g_set_error (error,
                                 G_MARKUP_ERROR,
                                 G_MARKUP_ERROR_INVALID_CONTENT,
                                 "resource_id attribute only allowed for resource-typed properties");
                    return;
                }

                info->value = g_string_new(NULL);
            }

            info->state = STATE_PROPERTY;
            break;
        }
    case STATE_PROPERTY:
        {
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Unexpected element <%s/> inside property element",
                         element_name);
            break;
        }
    }
}

static void
static_file_end_element (GMarkupParseContext *context,
			 const gchar         *element_name,
			 gpointer             user_data,
			 GError             **error)
{
    SFParseInfo *info = user_data;

    switch (info->state) {
    case STATE_OUTSIDE:
        g_assert_not_reached();
        break;
    case STATE_MODEL:
        info->state = STATE_OUTSIDE;
        break;
    case STATE_RESOURCE:
        info->current_resource = NULL;
        info->state = STATE_MODEL;
        break;
    case STATE_PROPERTY:
        {
            DDMDataValue value;

            if (info->current_update == DDM_DATA_UPDATE_ADD) {
                if (info->current_type == DDM_DATA_RESOURCE) {
                    value.type = DDM_DATA_RESOURCE;
                    value.u.resource = ddm_data_model_lookup_resource(info->model, info->value->str);
                    
                    if (value.u.resource == NULL) {
                        g_set_error (error,
                                     G_MARKUP_ERROR,
                                     G_MARKUP_ERROR_INVALID_CONTENT,
                                 "Reference to a resource %s that we don't know about",
                                     info->value->str);
                    return;
                    }
                } else {
                    if (!ddm_data_value_from_string(&value, info->current_type, info->value->str,
                                                    NULL, error))
                        return;
                }
            } else {
                value.type = DDM_DATA_NONE;
            }

            
            ddm_data_resource_update_property(info->current_resource,
                                              info->current_property_id,
                                              info->current_update,
                                              info->current_cardinality,
                                              info->current_default_include,
                                              info->current_default_children,
                                              &value);

            ddm_data_value_clear(&value);
            
            info->current_property_id = NULL;
            g_free(info->current_default_children);
            info->current_default_children = NULL;

            if (info->value != NULL) {
                g_string_free(info->value, TRUE);
                info->value = NULL;
            }
            info->state = STATE_RESOURCE;
            break;
        }
    }
    
    sf_parse_info_pop_element(info);
}

static void
static_file_text (GMarkupParseContext *context,
		  const gchar         *text,
		  gsize                text_len,  
		  gpointer             user_data,
		  GError             **error)
{
    SFParseInfo *info = user_data;
    SFElement *element = info->element;
    gboolean nonblank = FALSE;
    const char *p;

    for (p = text; (gsize)(p - text) < text_len; p++) {
        if (!g_ascii_isspace(*p))
            nonblank = TRUE;
    }

    if (info->state != STATE_PROPERTY) {
        if (nonblank)
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Unexpected text inside <%s/>",
                         element->name->name);
        return;
    }

    if (info->current_type == DDM_DATA_RESOURCE) {
        if (nonblank)
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Unexpected text inside resource-typed property");
        return;
    }
        
    if (info->current_update == DDM_DATA_UPDATE_CLEAR) {
        if (nonblank)
            g_set_error (error,
                         G_MARKUP_ERROR,
                         G_MARKUP_ERROR_INVALID_CONTENT,
                         "Unexpected text inside property with update='clear'");
        return;
    }

    g_string_append_len(info->value, text, text_len);
}

static void
static_file_passthrough (GMarkupParseContext *context,
			 const gchar         *passthrough_text,
			 gsize                text_len,  
			 gpointer             user_data,
			 GError             **error)
{
}

static void
static_file_error (GMarkupParseContext *context,
		   GError              *error,
		   gpointer             user_data)
{
}

static const GMarkupParser static_file_parser = {
    static_file_start_element,
    static_file_end_element,
    static_file_text,
    static_file_passthrough,
    static_file_error
};

gboolean
ddm_static_file_parse(const char   *filename,
                      DDMDataModel *model,
                      GError      **error)
{
    SFParseInfo info;
    GMarkupParseContext *context;
    gboolean result;
    char *text;
    gsize len;
    
    g_return_val_if_fail(filename != NULL, FALSE);
    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), FALSE);
    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    if (!g_file_get_contents(filename, &text, &len, error))
        return FALSE;
    
    sf_parse_info_init(&info, model);
    
    info.model = model;
    info.state = STATE_OUTSIDE;

    context = g_markup_parse_context_new(&static_file_parser,
                                         G_MARKUP_TREAT_CDATA_AS_TEXT,
                                         &info,
                                         NULL);
    
    result = g_markup_parse_context_parse(context, text, len, error);

    g_markup_parse_context_free(context);

    sf_parse_info_finish(&info);

    return result;
}
