#ifndef __HIPPO_PLATFORM_H__
#define __HIPPO_PLATFORM_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoPlatform      HippoPlatform;
typedef struct _HippoPlatformClass HippoPlatformClass;

#define HIPPO_TYPE_PLATFORM              (hippo_platform_get_type ())
#define HIPPO_PLATFORM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PLATFORM, HippoPlatform))
#define HIPPO_PLATFORM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PLATFORM, HippoPlatformClass))
#define HIPPO_IS_PLATFORM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PLATFORM))
#define HIPPO_IS_PLATFORM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PLATFORM))
#define HIPPO_PLATFORM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_PLATFORM, HippoPlatformClass))

struct _HippoPlatformClass {
	GTypeInterface base_iface;

	gboolean  (* read_login_cookie)   (HippoPlatform  *platform,
	                                   HippoBrowserKind *origin_browser_p,
	                                   char          **username,
	                                   char          **password);
	void      (* delete_login_cookie) (HippoPlatform  *platform);                                   
	
	const char* (* get_jabber_resource) (HippoPlatform  *platform);
	
	/* Preferences */
	char*     (* get_message_server)  (HippoPlatform *platform);
	char*     (* get_web_server)      (HippoPlatform *platform);
	gboolean  (* get_signin)          (HippoPlatform *platform);
	
	void     (* set_message_server)  (HippoPlatform *platform, const char *value);
	void     (* set_web_server)      (HippoPlatform *platform, const char *value);
	void     (* set_signin)          (HippoPlatform *platform, gboolean    value);
};

GType        	 hippo_platform_get_type               (void) G_GNUC_CONST;

gboolean         hippo_platform_read_login_cookie      (HippoPlatform    *platform,
                 	                                    HippoBrowserKind *origin_browser_p,
	                                                    char            **username_p,
                  	                                    char            **password_p);
void             hippo_platform_delete_login_cookie    (HippoPlatform *platform); 	                                    
const char*      hippo_platform_get_jabber_resource    (HippoPlatform *platform); 

/* Preferences */
char*            hippo_platform_get_message_server     (HippoPlatform *platform); 
char*            hippo_platform_get_web_server         (HippoPlatform *platform); 
gboolean         hippo_platform_get_signin             (HippoPlatform *platform); 

void             hippo_platform_set_message_server     (HippoPlatform  *platform,
                                                        const char     *value); 
void             hippo_platform_set_web_server         (HippoPlatform  *platform,
                                                        const char     *value); 
void             hippo_platform_set_signin             (HippoPlatform  *platform,
                                                        gboolean        value);
                                                       
/* Convenience wrappers on get_server stuff that parse the host/port */
void             hippo_platform_get_message_host_port  (HippoPlatform  *platform,
                                                        char          **host_p,
                                                        int            *port_p);
void             hippo_platform_get_web_host_port      (HippoPlatform  *platform,
                                                        char          **host_p,
                                                        int            *port_p);

G_END_DECLS

#endif /* __HIPPO_PLATFORM_H__ */
