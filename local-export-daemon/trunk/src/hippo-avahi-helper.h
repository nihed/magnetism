/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_AHAVI_HELPER_H__
#define __HIPPO_AHAVI_HELPER_H__

#include <avahi-client/lookup.h>
#include <glib-object.h>

typedef struct _HippoAvahiBrowser HippoAvahiBrowser;
typedef struct _HippoAvahiBrowserClass HippoAvahiBrowserClass;

#define HIPPO_TYPE_AVAHI_BROWSER              (hippo_avahi_browser_get_type ())
#define HIPPO_AVAHI_BROWSER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_AVAHI_BROWSER, HippoAvahiBrowser))
#define HIPPO_AVAHI_BROWSER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_AVAHI_BROWSER, HippoAvahiBrowserClass))
#define HIPPO_IS_AVAHI_BROWSER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_AVAHI_BROWSER))
#define HIPPO_IS_AVAHI_BROWSER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_AVAHI_BROWSER))
#define HIPPO_AVAHI_BROWSER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_AVAHI_BROWSER, HippoAvahiBrowserClass))

GType hippo_avahi_browser_get_type(void);

HippoAvahiBrowser *hippo_avahi_browser_new(const char *type);

/**
 * HippoAvahiSessionBrowser:
 *
 * Like HippoAvahiBrowser but associates service information with user sessions in a 1:1 fashion.
 */
typedef struct _HippoAvahiSessionBrowser HippoAvahiSessionBrowser;
typedef struct _HippoAvahiSessionBrowserClass HippoAvahiSessionBrowserClass;

#define HIPPO_TYPE_AVAHI_SESSION_BROWSER              (hippo_avahi_session_browser_get_type ())
#define HIPPO_AVAHI_SESSION_BROWSER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_AVAHI_SESSION_BROWSER, HippoAvahiSessionBrowser))
#define HIPPO_AVAHI_SESSION_BROWSER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_AVAHI_SESSION_BROWSER, HippoAvahiSessionBrowserClass))
#define HIPPO_IS_AVAHI_SESSION_BROWSER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_AVAHI_SESSION_BROWSER))
#define HIPPO_IS_AVAHI_SESSION_BROWSER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_AVAHI_SESSION_BROWSER))
#define HIPPO_AVAHI_SESSION_BROWSER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_AVAHI_SESSION_BROWSER, HippoAvahiSessionBrowserClass))

GType hippo_avahi_session_browser_get_type(void);

HippoAvahiSessionBrowser *hippo_avahi_session_browser_new(const char *type);

typedef struct _HippoAvahiService HippoAvahiService;
typedef struct _HippoAvahiServiceClass HippoAvahiServiceClass;

#define HIPPO_TYPE_AVAHI_SERVICE              (hippo_avahi_service_get_type ())
#define HIPPO_AVAHI_SERVICE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_AVAHI_SERVICE, HippoAvahiService))
#define HIPPO_AVAHI_SERVICE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_AVAHI_SERVICE, HippoAvahiServiceClass))
#define HIPPO_IS_AVAHI_SERVICE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_AVAHI_SERVICE))
#define HIPPO_IS_AVAHI_SERVICE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_AVAHI_SERVICE))
#define HIPPO_AVAHI_SERVICE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_AVAHI_SERVICE, HippoAvahiServiceClass))

GType hippo_avahi_service_get_type(void);

const char *  hippo_avahi_service_get_name       (HippoAvahiService *service);
const char *  hippo_avahi_service_get_host_name  (HippoAvahiService *service);
void          hippo_avahi_service_get_address    (HippoAvahiService *service,
						  AvahiAddress      *address);
guint16       hippo_avahi_service_get_port       (HippoAvahiService *service);
const char *  hippo_avahi_service_get_session_id (HippoAvahiService *service);

char *hippo_avahi_service_get_txt_property(HippoAvahiService *service,
					   const char        *key);

#endif /* __HIPPO_AHAVI_HELPER_H__ */
