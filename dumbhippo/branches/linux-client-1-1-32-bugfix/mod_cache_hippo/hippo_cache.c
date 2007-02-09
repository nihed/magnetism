/* Copyright 2006, Red Hat Inc.
 *
 * Based on code:
 *  
 * Copyright 2001-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define CORE_PRIVATE

#include "mod_cache.h"

#include <ap_provider.h>

/* Check if the cookie value passed in is a DumbHippo auth cookie for the
 * server name found in the configuration. 
 * 
 * The format we are looking for is:
 *  
 *  host=foo.example.com&name=GUID&password=LONGHEXNUMBER
 *
 * Where the order of the parameters isn't significant.
 */
static int hippo_auth_is_for_server(cache_server_conf *conf, const char *cookie, const char *end)
{
    const char *p = cookie;

    while (p < end) {
        const char *next_amp = strchr(p, '&');
        if (next_amp == NULL)
            next_amp = end;
        if (strncmp(p, "host=", 5) == 0) {
	    p += 5;
	    if (next_amp - p == conf->hippo_server_name_len &&
		strncmp(p, conf->hippo_server_name, next_amp - p) == 0)
		return 1;
        } 
        p = next_amp + 1;
    }
    
    return 0;
}

/* Check if the cookie header value passed in (The Cookie: part has
 * already been stripped) contains a DumbHippo authorization cookie
 * for the server name found in the configuration.
 *
 * The reason we need to actually parse the DumbHippo authorization
 * cookies and check the server name is we have issues where 
 * a cookie from http://mugshot.org can be sent to
 * http://dogfood.mugshot.org, for example.
 **/
static int hippo_has_auth_cookie(cache_server_conf *conf, const char *header)
{
    /* The Cookie: header was defined successively in:
     *
     *  http://wp.netscape.com/newsref/std/cookie_spec.html
     *  http://www.ietf.org/rfc/rfc2109.txt
     *  http://www.ietf.org/rfc/rfc2965.txt
     *
     * With more elaborations in each, but all valid Cookie: headers
     * fit into the pattern:
     *
     *  Cookie: token "=" (token|quoted_string) (";"|",') 0*(token "=" (token|quoted_string))
     *
     * Though not all such strings are valid cookies (For example, the
     * first token is always supposed to be $Version, and ',' can only
     * be used to separate cookies, and cannot be used to separate a
     * a cookie from the following parameters.) We don't try to 
     * validate, but simply parse the header according to the above syntax, 
     * and look for:
     *  
     *   "auth" "=" (token | quoted_string)
     *
     * Similarly, we ignore the detailed HTTPD spec for token, and just look
     * for whitespace or the specific separator that follows.
     */
    const char *p = header;

    while (*p) {
	const char *name_start;
	const char *name_end;
	const char *value_start;
	const char *value_end;
	
	/* Find the leading token, which is the cookie name (or
         * a reserved value like $Version or $Path)
	 */
	while (apr_isspace(*p))
	    ++p;

	name_start = p;
	while (*p && *p != '=' && !apr_isspace(*p))
	    ++p;
	name_end = p;

	while (apr_isspace(*p))
	    ++p;

	/* Look for and skip the separating "="
	 */
	if (*p != '=')
	    return 0; /* parse error */
	++p;

	while (apr_isspace(*p))
	    ++p;

	if (!*p)
	    return 0; /* parse error */

	/* Now we have the value, which is either a quoted string or a
	 * a token
	 */
	
	if (*p == '"') {
	    /* Quoted string follows */
	    
	    ++p;
	    value_start = p;
	    while (1) {
		if (!*p) {
		    return 0; /* parse error */
		} else if (*p == '\\') {
		    // We don't have to actually unescape here ...
		    // since it never matters to us whether a cookie
		    // has a " or a \" in it.
		    ++p;
		    if (!*p)
			return 0; /* parse error */
		    ++p;
		} else if (*p == '"') {
		    break;
		} else {
		    ++p;
		}
	    }
	    value_end = p;
	    ++p;
	} else {
	    /* Unquoted token */
	    
	    value_start = p;
	    while (*p && *p != ',' && *p != ';' && !apr_isspace(*p))
		++p;
	    value_end = p;
	}

	/* Did we find the cookie we were looking for? */

	if (name_end - name_start == 4 &&
	    strncmp(name_start, "auth", 4) == 0 &&
	    hippo_auth_is_for_server(conf, value_start, value_end))
	    return 1;

	while (apr_isspace(*p))
	    ++p;

	if (*p) {
	    /* Look for and skip the separator between name=value pairs
	     */
	    if (*p != ',' && *p != ';')
		return 0; /* parse error */
	    ++p;
	    
	    while (apr_isspace(*p))
		++p;
	}
    }

    return 0;
}

#ifdef TEST

/* Unit tests for the string parsing above */

#include <stdio.h>

static void
test_auth_is_for_server(cache_server_conf *conf, const char *cookie, int expected) 
{
    int result = hippo_auth_is_for_server(conf, cookie, cookie + strlen(cookie));
    if (result != expected) {
	fprintf(stderr, "auth_is_for_server: '%s' => %d, expected %d\n", cookie, result, expected);
    }
}

static void
test_has_auth_cookie(cache_server_conf *conf, const char *header, int expected) 
{
    int result = hippo_has_auth_cookie(conf, header);
    if (result != expected) {
	fprintf(stderr, "has_auth_cookie: '%s' => %d, expected %d\n", header, result, expected);
    }
}


int main() 
{
    cache_server_conf conf;
    conf.hippo_server_name = "dogfood.dumbhippo.com";
    conf.hippo_server_name_len = strlen(conf.hippo_server_name);
	
    test_auth_is_for_server(&conf, "host=dogfood.dumbhippo.com", 1);
    test_auth_is_for_server(&conf, "host=dogfood.dumbhippo.com&name=GUID&password=LONGHEXNUMBER", 1);
    test_auth_is_for_server(&conf, "name=GUID&host=dogfood.dumbhippo.com&password=LONGHEXNUMBER", 1);
    test_auth_is_for_server(&conf, "name=GUID&password=LONGHEXNUMBER&host=dogfood.dumbhippo.com", 1);
    test_auth_is_for_server(&conf, "host=dumbhippo.com&name=GUID&password=LONGHEXNUMBER", 0);
    test_auth_is_for_server(&conf, "host=example.com&name=GUID&password=LONGHEXNUMBER", 0);
    test_auth_is_for_server(&conf, "host=example.com&name=GUID&password=LONGHEXNUMBER", 0);
    test_auth_is_for_server(&conf, "name=GUID&password=LONGHEXNUMBER&host=", 0);
    test_auth_is_for_server(&conf, "", 0);

    test_has_auth_cookie(&conf, "auth=host=dogfood.dumbhippo.com", 1);
    test_has_auth_cookie(&conf, "auth=\"host=dogfood.dumbhippo.com\"", 1);
    test_has_auth_cookie(&conf, "auth = \"host=dogfood.dumbhippo.com\"", 1);
    test_has_auth_cookie(&conf, "auth=host=dumbhippo.com", 0);
    test_has_auth_cookie(&conf, "$Version=1; Customer=\"WILE_E_COYOTE\"; $Path=\"/acme\"; auth=\"host=dogfood.dumbhippo.com\"; $Path=\"/\"", 1);
    test_has_auth_cookie(&conf, "$Version=1; auth=\"host=dumbhippo.com\" , auth=\"host=dogfood.dumbhippo.com\"", 1);
    test_has_auth_cookie(&conf, "$Version=1; auth=\"host=dumb\\\"hippo.com\", auth=\"host=dogfood.dumbhippo.com\"", 1);
    test_has_auth_cookie(&conf, "$Version=1; auth=\"host=dumbhippo.com, auth=\"host=dogfood.dumbhippo.com\"", 0);
    test_has_auth_cookie(&conf, "$Version=1; auth=\"host=dumbhippo.com\",", 0);
    test_has_auth_cookie(&conf, "", 0);
  
    return 0;
}

#else /* !TEST */
static int hippo_cache_check_always(request_rec *r,
                                    cache_server_conf *conf,
				    const char *url)
{
    int i;

    for (i = 0; i < conf->hippo_always->nelts; i++) {
        struct cache_disable *ent = 
                               (struct cache_disable *)conf->hippo_always->elts;
        if ((ent[i].url) && !strncasecmp(url, ent[i].url, ent[i].urllen)) {
            /* Stop searching now. */
            return 1;
        }
    }

    return 0;
}

/* return true if we should cache the URL when taking into account
 * DumbHippo specific caching configuration
 */
int hippo_cache_check(request_rec *r,
                      cache_server_conf *conf,
		      const char *url)
{
    const char *cookie_header;
    
    /* paranoia */
    if (!url) return 0;

    if (conf->hippo_server_name == NULL) {
	return 1;
    }

    /* Check if the URL is excepted from the cookie check */
    if (hippo_cache_check_always(r, conf, url)) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
	            "hippo_cache: %s is always eligible for caching", url);
	return 1;
    }

    cookie_header = apr_table_get(r->headers_in, "Cookie");
    if (cookie_header == NULL) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
	            "hippo_cache: no Cookie header sent for %s, can cache", url);
	return 1;
    }

    /* See if the auth= cookie is set for our configured server */
    if (!hippo_has_auth_cookie(conf, cookie_header)) {
        ap_log_error(APLOG_MARK, APLOG_DEBUG, 0, r->server,
		     "hippo_cache: Cookie header sent for %s doesn't have auth cookie, can cache", url);
	return 1;
    }
    
    return 0;
}
#endif /* TEST */
