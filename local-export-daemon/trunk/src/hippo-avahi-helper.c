/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>

#include "hippo-avahi-helper.h"
#include "avahi-glue.h"

/*
 * Information that can be used to identify a service as it is added/removed
 */
typedef struct {
    AvahiIfIndex interface;
    AvahiProtocol protocol;
    char *name;
    char *type;
    char *domain;
} ServiceId;


/* Declare this here to avoid adding some accessors */
struct _HippoAvahiService {
    GObject parent;

    AvahiServiceResolver *resolver;

    ServiceId id;
    char *host_name;
    char *session_id;
    AvahiAddress address;
    guint16 port;
    gboolean found;
    
    AvahiStringList *txt;
};

struct _HippoAvahiServiceClass {
    GObjectClass parent_class;
};

static HippoAvahiService *hippo_avahi_service_new(ServiceId *id);

static int
int_cmp(const int a,
        const int b)
{
    if (a < b)
        return -1;
    else if (a > b)
        return 1;
    else
        return 0;
}

static int
service_id_cmp(const void *a,
               const void *b)
{
    const ServiceId *service_id_a = a;
    const ServiceId *service_id_b = b;
    int v;

    v = int_cmp(service_id_a->interface, service_id_b->interface);
    if (v != 0)
        return v;
    v = int_cmp(service_id_a->protocol, service_id_b->protocol);
    if (v != 0)
        return v;
    /* do name first since it's most likely to be different, thus saving
     * the other comparisons. In fact type and domain will generally
     * all be the same I think.
     */
    v = strcmp(service_id_a->name, service_id_b->name);
    if (v != 0)
        return v;
    v = strcmp(service_id_a->type, service_id_b->type);
    if (v != 0)
        return v;
    v = strcmp(service_id_a->domain, service_id_b->domain);
    if (v != 0)
        return v;

    return 0;
}              

static void
service_id_free_fields(ServiceId *id)
{
    g_free(id->name);
    g_free(id->type);
    g_free(id->domain);
}

static void
service_id_init_fields(ServiceId       *id,
                       const ServiceId *orig)
{
    *id = *orig;
    id->name = g_strdup(orig->name);
    id->type = g_strdup(orig->type);
    id->domain = g_strdup(orig->domain);
}

/************************************************************************/

/* Enum is shared between HippoAvahiBrowser and HippoAvahiSessionBrowser; they could in theory
 * export a common interface, but not really worth it at this level of complexity.
 */
enum {
    SERVICE_ADDED,
    SERVICE_REMOVED,
    LAST_BROWSER_SIGNAL
};

static int browser_signals[LAST_BROWSER_SIGNAL];

struct _HippoAvahiBrowser {
    GObject parent;

    GTree *service_by_id;
    AvahiServiceBrowser *service_browser;
};

struct _HippoAvahiBrowserClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoAvahiBrowser, hippo_avahi_browser, G_TYPE_OBJECT)

static void
browser_on_service_found(HippoAvahiService  *service,
			 HippoAvahiBrowser *browser)
{
    g_signal_emit(browser, browser_signals[SERVICE_ADDED], 0, service);
	    
}

static void
browser_on_service_lost(HippoAvahiService *service,
			HippoAvahiBrowser *browser)
{
    g_signal_emit(browser, browser_signals[SERVICE_REMOVED], 0, service);
}

static void
browser_disconnect_service(HippoAvahiBrowser *browser,
                           HippoAvahiService *service)
{
    g_signal_handlers_disconnect_by_func(service, (void *)browser_on_service_found, browser);
    g_signal_handlers_disconnect_by_func(service, (void *)browser_on_service_lost, browser);
}

static gboolean
browser_disconnect_service_foreach(void *key,
				   void *value,
				   void *data)
{
    HippoAvahiBrowser *browser = data;
    HippoAvahiService *service = value;

    browser_disconnect_service(browser, service); 

    g_object_unref(service);

    return FALSE; /* keep going */
}

static void
hippo_avahi_browser_finalize(GObject *object)
{
    HippoAvahiBrowser *browser = HIPPO_AVAHI_BROWSER(object);

    g_tree_foreach(browser->service_by_id, browser_disconnect_service_foreach, browser);
    g_tree_destroy(browser->service_by_id);

    if (browser->service_browser)
	avahi_service_browser_free (browser->service_browser);
    
    G_OBJECT_CLASS(hippo_avahi_browser_parent_class)->finalize(object);
}

static void
hippo_avahi_browser_init(HippoAvahiBrowser *browser)
{
    browser->service_by_id = g_tree_new(service_id_cmp);
}

static void
hippo_avahi_browser_class_init(HippoAvahiBrowserClass *browser_class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(browser_class);
    
    object_class->finalize = hippo_avahi_browser_finalize;
    
    browser_signals[SERVICE_ADDED] = g_signal_new ("service-added",
						   G_TYPE_FROM_CLASS (object_class),
						   G_SIGNAL_RUN_LAST,
						   0,
						   NULL, NULL,
						   g_cclosure_marshal_VOID__OBJECT,
						   G_TYPE_NONE, 1, HIPPO_TYPE_AVAHI_SERVICE);
    browser_signals[SERVICE_REMOVED] = g_signal_new ("service-removed",
						     G_TYPE_FROM_CLASS (object_class),
						     G_SIGNAL_RUN_LAST,
						     0,
						     NULL, NULL,
						     g_cclosure_marshal_VOID__OBJECT,
						     G_TYPE_NONE, 1, HIPPO_TYPE_AVAHI_SERVICE);
}

static void
browse_callback(AvahiServiceBrowser   *b,
                AvahiIfIndex           interface,
                AvahiProtocol          protocol,
                AvahiBrowserEvent      event,
                const char            *name,
                const char            *type,
                const char            *domain,
                AvahiLookupResultFlags flags,
                void                  *data)
{
    HippoAvahiBrowser *browser = data;
    
    /* Called whenever a new services becomes available on the LAN or is removed from the LAN */
    ServiceId id;
    
    id.interface = interface;
    id.protocol = protocol;
    id.name = (char*) name;
    id.type = (char*) type;
    id.domain = (char*) domain;
    
    switch (event) {
    case AVAHI_BROWSER_FAILURE:
        /* Nothing sane to do here - help */
        g_printerr("Avahi browser failed: %s\n", avahi_strerror(avahi_client_errno(avahi_service_browser_get_client(b))));
        return;

    case AVAHI_BROWSER_NEW:
        {
            HippoAvahiService *service;

            g_debug("Browsed %s", name);

	    service = g_tree_lookup(browser->service_by_id, &id);
	    if (service != NULL) {
		g_warning("Got AVAHI_BROWSER_NEW for a service we already know about");
		return;
	    }
            
            service = hippo_avahi_service_new(&id);
	    g_tree_insert(browser->service_by_id, &service->id, service);

	    g_signal_connect(service, "found",
			     G_CALLBACK(browser_on_service_found), browser);
	    g_signal_connect(service, "lost",
			     G_CALLBACK(browser_on_service_lost), browser);
        }
        break;

    case AVAHI_BROWSER_REMOVE:
        {
	    HippoAvahiService *service;

	    service = g_tree_lookup(browser->service_by_id, &id);
	    if (service == NULL) {
		g_warning("Got AVAHI_BROWSER_NEW for a service we didn't know about");
		return;
	    }

	    browser_disconnect_service(browser, service);

	    g_tree_remove(browser->service_by_id, &id);
	    
	    if (service->found)
		g_signal_emit(browser, browser_signals[SERVICE_REMOVED], 0, service);
	    
	    g_object_unref(service);
        }
        break;

    case AVAHI_BROWSER_ALL_FOR_NOW:
    case AVAHI_BROWSER_CACHE_EXHAUSTED:
        g_debug("Avahi browser event %s",
                event == AVAHI_BROWSER_CACHE_EXHAUSTED ? "CACHE_EXHAUSTED" : "ALL_FOR_NOW");
        break;
        
    default:
        g_debug("Unknown Avahi browser event %d", event);
        break;
    }
}

HippoAvahiBrowser *
hippo_avahi_browser_new(const char *type)
{
    HippoAvahiBrowser *browser = g_object_new(HIPPO_TYPE_AVAHI_BROWSER, NULL);
    
    /* We want to only browse services available on IPv4 for several reasons:
     *
     * A) IPV6 local link addresses are basically broken with Linux kernels as of
     *    July 2007, since you have to specify the interface when connecting.
     * B) It basically doesn't make sense to integrate mDNS/DNS-SD results from
     *    multiple independent service, so we should be browsing IPV4 *or*
     *    IPV6, not both.
     *
     * FIXME: the IPV4 vs. IPV6 decision really should be made at the Avahi system
     *   configuration level, not in individual apps, this is just a workaround
     *   for standard Avahi system level configuration, and also for the lack of a
     *   good system level policy (http:///avahi.org/ticket/150). But we're still
     *   a long ways from the time that browsing IPV6-only makes sense.
     */
    browser->service_browser = avahi_service_browser_new(avahi_glue_get_client(),
							 AVAHI_IF_UNSPEC, AVAHI_PROTO_INET, type,
							 NULL, /* domain */
							 0, /* flags */
							 browse_callback,
							 browser);
    
    if (browser->service_browser == NULL)
        g_printerr("Failed to create service browser: %s\n", avahi_strerror(avahi_client_errno(avahi_glue_get_client())));

    return browser;
}

/************************************************************************/

static int session_browser_signals[LAST_BROWSER_SIGNAL];

struct _HippoAvahiSessionBrowser {
    GObject parent;
    
    HippoAvahiBrowser *base_browser;
    GHashTable *services_by_session_id;
    GHashTable *all_services;
};

struct _HippoAvahiSessionBrowserClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoAvahiSessionBrowser, hippo_avahi_session_browser, G_TYPE_OBJECT)

static void
session_browser_add_service_to_id_hash(HippoAvahiSessionBrowser *session_browser,
				       const char               *session_id,
				       HippoAvahiService        *service)
{
    GSList *old_services = g_hash_table_lookup(session_browser->services_by_session_id, session_id);
    GSList *new_services = g_slist_append(old_services, g_object_ref(service));

    if (new_services != old_services) {
	/* If there are multiple services for the session ID they are ordered with the first added
	 * first in the list. This is the service that we expose publicly. Adding subsequent services
	 * does not result in an immediate ::service-added, but we may send one laster if the first
	 * added service goes away.
	 */
	g_hash_table_replace(session_browser->services_by_session_id, g_strdup(session_id), new_services);
	g_signal_emit(session_browser, session_browser_signals[SERVICE_ADDED], 0, service);
    }
}

static void
session_browser_remove_service_from_id_hash(HippoAvahiSessionBrowser *session_browser,
					    const char               *session_id,
					    HippoAvahiService        *service)
{
    GSList *old_services = g_hash_table_lookup(session_browser->services_by_session_id, session_id);
    GSList *new_services = g_slist_remove(old_services, service);

    if (new_services != old_services) {
	/* If there are multiple services for the same sesssion ID and the first one goes away, then we
	 * first signal that service going away, and then signal the first remaining service
	 * being added in its place.
	 */
	if (new_services == NULL)
	    g_hash_table_remove(session_browser->services_by_session_id, session_id);
	else
	    g_hash_table_replace(session_browser->services_by_session_id, g_strdup(session_id), new_services);

	g_signal_emit(session_browser, session_browser_signals[SERVICE_REMOVED], 0, service);
	if (new_services != NULL)
	    g_signal_emit(session_browser, session_browser_signals[SERVICE_ADDED], 0, new_services->data);
    }

    g_object_unref(service);
}

static void
on_service_txt_changed(HippoAvahiService        *service,
		       HippoAvahiSessionBrowser *session_browser)
{
    const char *old_session_id;
    const char *new_session_id;

    old_session_id = g_object_get_data(G_OBJECT(service), "hippo-avahi-session-id");
    new_session_id = hippo_avahi_service_get_session_id(service);

    if (old_session_id == new_session_id ||
	(old_session_id != NULL && new_session_id != NULL && strcmp(old_session_id, new_session_id) == 0)) {
	return;
    }

    if (old_session_id != NULL)
	session_browser_remove_service_from_id_hash(session_browser, old_session_id, service);
    
    g_object_set_data_full(G_OBJECT(service), "hippo-avahi-session-id", g_strdup(new_session_id), (GDestroyNotify)g_free);
    if (new_session_id != NULL)
	session_browser_add_service_to_id_hash(session_browser, new_session_id, service);
}

static void
session_browser_remove_service(HippoAvahiSessionBrowser *session_browser,
			       HippoAvahiService        *service)
{
    const char *old_session_id;

    g_signal_handlers_disconnect_by_func(service,
					 (void *)on_service_txt_changed,
					 session_browser);
    
    old_session_id = g_object_get_data(G_OBJECT(service), "hippo-avahi-session-id");
    if (old_session_id) {
	session_browser_remove_service_from_id_hash(session_browser, old_session_id, service);
	g_object_set_data(G_OBJECT(service), "hippo-avahi-session-id", NULL);
    }

    if (session_browser->all_services) /* Null during finalize */
	g_hash_table_remove(session_browser->all_services, service);
}

static void
session_browser_add_service(HippoAvahiSessionBrowser *session_browser,
			    HippoAvahiService        *service)
{
    g_signal_connect(service, "txt-changed",
		     G_CALLBACK(on_service_txt_changed), session_browser);
    
    on_service_txt_changed(service, session_browser);

    g_hash_table_insert(session_browser->all_services, service, service);
}

static void
session_browser_remove_service_foreach(void *key,
				       void *value,
				       void *data)
{
    HippoAvahiSessionBrowser *session_browser = data;
    HippoAvahiService *service = value;

    session_browser_remove_service(session_browser, service);
}

static void
hippo_avahi_session_browser_finalize(GObject *object)
{
    HippoAvahiSessionBrowser *session_browser = HIPPO_AVAHI_SESSION_BROWSER(object);
    GHashTable *all_services;

    all_services = session_browser->all_services;
    session_browser->all_services = NULL;
    g_hash_table_foreach(all_services, session_browser_remove_service_foreach, session_browser);
    g_hash_table_destroy(all_services);

    g_assert(g_hash_table_size(session_browser->services_by_session_id) == 0);
    g_hash_table_destroy(session_browser->services_by_session_id);
    
    g_object_unref(session_browser->base_browser);
    
    G_OBJECT_CLASS(hippo_avahi_session_browser_parent_class)->finalize(object);
}

static void
hippo_avahi_session_browser_init(HippoAvahiSessionBrowser *session_browser)
{
    session_browser->services_by_session_id = g_hash_table_new_full(g_str_hash, g_str_equal,
								    (GDestroyNotify)g_free, NULL);
    session_browser->all_services = g_hash_table_new(g_direct_hash, NULL);
}

static void
hippo_avahi_session_browser_class_init(HippoAvahiSessionBrowserClass *session_browser_class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(session_browser_class);
    
    object_class->finalize = hippo_avahi_session_browser_finalize;
    
    session_browser_signals[SERVICE_ADDED] = g_signal_new ("service-added",
							   G_TYPE_FROM_CLASS (object_class),
							   G_SIGNAL_RUN_LAST,
							   0,
							   NULL, NULL,
							   g_cclosure_marshal_VOID__OBJECT,
							   G_TYPE_NONE, 1, HIPPO_TYPE_AVAHI_SERVICE);
    session_browser_signals[SERVICE_REMOVED] = g_signal_new ("service-removed",
							     G_TYPE_FROM_CLASS (object_class),
							     G_SIGNAL_RUN_LAST,
							     0,
							     NULL, NULL,
							     g_cclosure_marshal_VOID__OBJECT,
							     G_TYPE_NONE, 1, HIPPO_TYPE_AVAHI_SERVICE);
}

static void
on_base_browser_service_added(HippoAvahiBrowser        *base_browser,
			      HippoAvahiService        *service,
			      HippoAvahiSessionBrowser *session_browser)
{
    session_browser_add_service(session_browser, service);
}

static void
on_base_browser_service_removed(HippoAvahiBrowser        *base_browser,
				HippoAvahiService        *service,
				HippoAvahiSessionBrowser *session_browser)
{
    session_browser_remove_service(session_browser, service);
}

HippoAvahiSessionBrowser *
hippo_avahi_session_browser_new(const char *type)
{
    HippoAvahiSessionBrowser *session_browser = g_object_new(HIPPO_TYPE_AVAHI_SESSION_BROWSER, NULL);
    
    session_browser->base_browser = hippo_avahi_browser_new(type);
    
    g_signal_connect(session_browser->base_browser, "service-added",
		     G_CALLBACK(on_base_browser_service_added), session_browser);
    g_signal_connect(session_browser->base_browser, "service-removed",
		     G_CALLBACK(on_base_browser_service_removed), session_browser);
    
    return session_browser;
}

/************************************************************************/

enum {
    FOUND,
    LOST,
    ADDRESS_CHANGED,
    TXT_CHANGED,
    HOST_NAME_CHANGED,
    LAST_SERVICE_SIGNAL
};

static int service_signals[LAST_SERVICE_SIGNAL];

G_DEFINE_TYPE(HippoAvahiService, hippo_avahi_service, G_TYPE_OBJECT)

static void
hippo_avahi_service_finalize(GObject *object)
{
    HippoAvahiService *service = HIPPO_AVAHI_SERVICE(object);
    
    if (service->resolver) /* only NULL if we failed to create it in resolver_new */
	avahi_service_resolver_free(service->resolver);
        
    service_id_free_fields(&service->id);

    g_free(service->session_id);
    g_free(service->host_name);
    
    G_OBJECT_CLASS(hippo_avahi_service_parent_class)->finalize(object);
}

static void
hippo_avahi_service_init(HippoAvahiService *service)
{
}

static void
hippo_avahi_service_class_init(HippoAvahiServiceClass *service_class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(service_class);
    
    object_class->finalize = hippo_avahi_service_finalize;
    
    service_signals[FOUND] = g_signal_new("found",
					  G_TYPE_FROM_CLASS (object_class),
					  G_SIGNAL_RUN_LAST,
					  0,
					  NULL, NULL,
					  g_cclosure_marshal_VOID__VOID,
					  G_TYPE_NONE, 0);
    service_signals[LOST] = g_signal_new("lost",
					 G_TYPE_FROM_CLASS (object_class),
					 G_SIGNAL_RUN_LAST,
					 0,
					 NULL, NULL,
					 g_cclosure_marshal_VOID__VOID,
					 G_TYPE_NONE, 0);
    service_signals[ADDRESS_CHANGED] = g_signal_new("address-changed",
						    G_TYPE_FROM_CLASS (object_class),
						    G_SIGNAL_RUN_LAST,
						    0,
						    NULL, NULL,
						    g_cclosure_marshal_VOID__VOID,
						    G_TYPE_NONE, 0);
    service_signals[HOST_NAME_CHANGED] = g_signal_new("host-name-changed",
						      G_TYPE_FROM_CLASS (object_class),
						      G_SIGNAL_RUN_LAST,
						      0,
						      NULL, NULL,
						      g_cclosure_marshal_VOID__VOID,
						      G_TYPE_NONE, 0);
    service_signals[TXT_CHANGED] = g_signal_new("txt-changed",
						G_TYPE_FROM_CLASS (object_class),
						G_SIGNAL_RUN_LAST,
						0,
						NULL, NULL,
						g_cclosure_marshal_VOID__VOID,
						 G_TYPE_NONE, 0);
}

const char *
hippo_avahi_service_get_name(HippoAvahiService *service)
{
    g_return_val_if_fail(HIPPO_IS_AVAHI_SERVICE(service), NULL);

    return service->id.name;
}

const char *
hippo_avahi_service_get_host_name(HippoAvahiService *service)
{
    g_return_val_if_fail(HIPPO_IS_AVAHI_SERVICE(service), NULL);
    
    return service->host_name;
}

void
hippo_avahi_service_get_address(HippoAvahiService *service,
				AvahiAddress      *address)
{
    g_return_if_fail(HIPPO_IS_AVAHI_SERVICE(service));

    *address = service->address;
}

guint16
hippo_avahi_service_get_port(HippoAvahiService *service)
{
    g_return_val_if_fail(HIPPO_IS_AVAHI_SERVICE(service), 0);
    
    return service->port;
}

const char *
hippo_avahi_service_get_session_id(HippoAvahiService *service)
{
    g_return_val_if_fail(HIPPO_IS_AVAHI_SERVICE(service), NULL);

    return service->session_id;
}

char *
hippo_avahi_service_get_txt_property(HippoAvahiService *service,
				     const char        *key)
{
    AvahiStringList *found;
    char *k;
    char *v;
    size_t len;
    
    g_return_val_if_fail(HIPPO_IS_AVAHI_SERVICE(service), NULL);

    if (service->txt == NULL)
	return NULL;
    
    found = avahi_string_list_find(service->txt, key);
    if (found == NULL)
        return NULL;

    k = NULL;
    v = NULL;
    len = 0;
    avahi_string_list_get_pair(found, &k, &v, &len);

    avahi_free(k);

    /* if the string isn't nul terminated then bail */
    if (v != NULL && (memchr(v, '\0', len) != NULL || v[len] != '\0')) {
        avahi_free(v);
        v = NULL;
    }

    g_debug("Value for %s is %s", key, v);
    
    return v;
}

static void
resolve_callback(AvahiServiceResolver  *r,
                 AvahiIfIndex           interface,
                 AvahiProtocol          protocol,
                 AvahiResolverEvent     event,
                 const char            *name,
                 const char            *type,
                 const char            *domain,
                 const char            *host_name,
                 const AvahiAddress    *address,
                 uint16_t               port,
                 AvahiStringList       *txt,
                 AvahiLookupResultFlags flags,
                 void                  *data)
{
    /* Called whenever a service has been resolved successfully or timed out */
    HippoAvahiService *service = data;

    switch (event) {
    case AVAHI_RESOLVER_FAILURE:
	/* This means that we didn't find the server for now, but it might
	 * reappear later
	 */
	service->found = FALSE;
	g_signal_emit(service, service_signals[LOST], 0);
        break;
        
    case AVAHI_RESOLVER_FOUND:
        g_debug("Resolved %s:%d", host_name, port);
        
        if (!service->found) {
	    service->found = TRUE;

	    g_free(service->host_name);
            service->host_name = g_strdup(host_name);
            service->address = *address;
            service->port = port;

	    avahi_string_list_free(service->txt);
	    service->txt = avahi_string_list_copy(txt);

	    g_free(service->session_id);
	    service->session_id = hippo_avahi_service_get_txt_property(service, "org.freedesktop.od.session");

	    g_signal_emit(service, service_signals[FOUND], 0);
        } else {
            /* a change notification */
	    
	    gboolean host_name_changed;
	    gboolean address_changed;
	    gboolean txt_changed;
	    
	    if (strcmp(service->host_name, host_name) != 0) {
                g_free(service->host_name);
                service->host_name = g_strdup(host_name);
		host_name_changed = TRUE;
            }

	    if (avahi_address_cmp(&service->address, address) != 0) {
		service->address = *address;
		address_changed = TRUE;
	    }

	    if (service->port != port) {
		service->port = port;
		address_changed = TRUE;
	    }
		
	    if (!avahi_string_list_equal(txt, service->txt)) {
		avahi_string_list_free(service->txt);
		service->txt = avahi_string_list_copy(txt);
		txt_changed = TRUE;

		g_free(service->session_id);
		service->session_id = hippo_avahi_service_get_txt_property(service, "org.freedesktop.od.session");
	    }

	    if (host_name_changed)
		g_signal_emit(service, service_signals[HOST_NAME_CHANGED], 0);
	    if (address_changed)
		g_signal_emit(service, service_signals[ADDRESS_CHANGED], 0);
	    if (txt_changed)
		g_signal_emit(service, service_signals[TXT_CHANGED], 0);
        }
        break;
    default:
        g_debug("Unknown resolver event");
        break;
    }
}

static HippoAvahiService *
hippo_avahi_service_new(ServiceId *id)
{
    HippoAvahiService *service = g_object_new(HIPPO_TYPE_AVAHI_SERVICE, NULL);

    service_id_init_fields(&service->id, id);

    service->resolver = avahi_service_resolver_new(avahi_glue_get_client(),
						   service->id.interface,
						   service->id.protocol,
						   service->id.name,
						   service->id.type,
						   service->id.domain,
						   AVAHI_PROTO_UNSPEC, 0,
						   resolve_callback,
						   service);

    if (service->resolver == NULL) {
        g_printerr("Failed to create service resolver: '%s': %s\n", service->id.name,
                   avahi_strerror(avahi_client_errno(avahi_glue_get_client())));
    }
    
    return service;
}

