/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#define DDM_I_KNOW_THIS_IS_UNSTABLE 1
#include <ddm/ddm.h>

static DDMDataModel *ddm_model;

static void
print_resource_properties(DDMDataResource *resource,
                          GSList          *properties)
{
    GSList *tmp;
    
    for (tmp = properties; tmp != NULL; tmp = tmp->next) {
        DDMDataProperty *prop = tmp->data;
        DDMQName *prop_qname;
        char *s;
        DDMDataValue value;

        prop_qname = ddm_data_property_get_qname(prop);
        ddm_data_property_get_value(prop, &value);
        s = ddm_data_value_to_string(&value);
        
        g_print("   %s#%s = %s\n",
                prop_qname->uri, prop_qname->name, s);
        
        g_free(s);
    }
}

static void
on_resource_changed(DDMDataResource *resource,
                    GSList          *changed_qnames,
                    gpointer         user_data)
{
    GSList *tmp;
    GSList *changed_properties;    

    changed_properties = NULL;
    for (tmp = changed_qnames; tmp != NULL; tmp = tmp->next) {
        DDMQName *prop_qname = tmp->data;
        DDMDataProperty *prop;
        
        prop = ddm_data_resource_get_property_by_qname(resource, prop_qname);
        if (prop == NULL)
            g_print("   property %s#%s unset\n", prop_qname->uri, prop_qname->name);
        else
            changed_properties = g_slist_prepend(changed_properties, prop);
    }

    g_print("Resource '%s' changed %d properties\n",
            ddm_data_resource_get_resource_id(resource),
            g_slist_length(changed_properties));
    
    print_resource_properties(resource, changed_properties);
}

static void
on_query_response(GSList            *resources,
                  gpointer           user_data)
{
    GSList *l;

    for (l = resources; l != NULL; l = l->next) {
        DDMDataResource *resource = l->data;
        GSList *properties;    
    
        g_print("Resource '%s' received in reply to query\n",
                ddm_data_resource_get_resource_id(resource));
        
        properties = ddm_data_resource_get_properties(resource);
        
        print_resource_properties(resource, properties);

        g_slist_free(properties);
        
        ddm_data_resource_connect(resource,
                                  NULL, /* NULL = all properties */
                                  on_resource_changed,
                                  NULL);
    }
}

static void
on_query_error(DDMDataError     error,
               const char      *message,
               gpointer         user_data)
{
    g_printerr("Failed to get query reply: '%s'\n", message);
}

int
main(int argc, char **argv)
{
    DDMDataQuery *global_resource_query;
    GMainLoop *loop;
    
    g_type_init();
    
    ddm_model = ddm_data_model_get_default();
    
    global_resource_query = ddm_data_model_query_resource(ddm_model,
                                                          "online-desktop:/o/global", "self [ photoUrl ]");
    if (global_resource_query == NULL) {
        g_printerr("Failed to query global resource\n");
        return 1;
    }
    
    ddm_data_query_set_multi_handler(global_resource_query,
                                     on_query_response, NULL);    
    
    ddm_data_query_set_error_handler(global_resource_query,
                                     on_query_error, NULL);
    
    loop = g_main_loop_new(NULL, FALSE);
    g_main_loop_run(loop);
    g_main_loop_unref(loop);
    
    return 0;
}
