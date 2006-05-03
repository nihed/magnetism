#ifndef __HIPPO_COOKIES_LINUX_H__
#define __HIPPO_COOKIES_LINUX_H__

#include <hippo/hippo-common.h>

/* returns list of HippoCookie from the cookies.txt file we determine is relevant,
 * with the given domain, port, name
 */
GSList* hippo_load_cookies(const char *domain,
                           int         port,
                           const char *name);

#endif /* __HIPPO_COOKIES_LINUX_H__ */
