/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_COOKIES_LINUX_H__
#define __HIPPO_COOKIES_LINUX_H__

#include <hippo/hippo-common.h>

typedef void (* HippoCookiesMonitorFunc) (void *data);

/* returns list of HippoCookie from the cookies.txt file we determine is relevant,
 * with the given domain, port, name
 */
GSList* hippo_load_cookies(const char *domain,
                           int         port,
                           const char *name);

void    hippo_cookie_monitor_add    (HippoCookiesMonitorFunc  func,
                                     void                    *data);
void    hippo_cookie_monitor_remove (HippoCookiesMonitorFunc  func,
                                     void                    *data);

#endif /* __HIPPO_COOKIES_LINUX_H__ */
