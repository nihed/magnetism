/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#ifndef WITH_MAEMO
#include <libgnome/gnome-desktop-item.h>
#endif
#include <libgnomevfs/gnome-vfs.h>
#include <gtk/gtk.h>
#include <gdk/gdkx.h>

#include "hippo-distribution.h"

/* Method for testing: yum in a gnome-terminal */
#define ENABLE_INSTALL_PACKAGE_YUM 0

/* This file contains functions to do various distribution speciifc functions like
 * determining the distribution name and version, checking if a package is installed,
 * installing a package, and so forth.
 *
 * The design of this file is that all the code for the different distributions is
 * compiled on every distribution, and run-time checks are used to determine which
 * code is executed on a particular system. (Currently, all functionality is done
 * by checking the contents of files or executing external commands.) This approach
 * has the advantage of keeping all the code tested for compilation and is also
 * more robust against moving a compiled version of the code between similar
 * distributions or between distribution versions.
 *
 * However, if it becomes necessary to link to libraries to perform certain checks,
 * extra code paths could be added protected with #ifdefs.
 *
 *
 * When we receive a package name specification from the server, it will be in the
 * form:
 *
 *   fedora,epel=inkscape;ubuntu=Inkscape
 *
 * (a hypthetical example.) fedora,epel and ubuntu are "sources". Each distribution
 * has a source that is the lower-cased version of the distribution name, but there
 * can also be additional sources that could be added by the user, exemplified
 * here by EPEL (Extra Packages for Enterprise Linux: 3rd party packages for RHEL
 * and CentOS.) Ubuntu Universe would be another such example, since it needs to
 * be explicitly enabled on an Ubuntu system.
 *
 * To handle optionally sources like EPEL, runtime tests for the sources would have
 * to be added to this file; you'd probably want to write generic tests for, e.g.
 * whethera yum repository was enabled, then use tables to configure the sources
 * based on those generic tests.
 */

#define APPLICATION_DIR "/usr/share/applications"

typedef gboolean (*CheckDistributionFunction) (const char                 *filename,
                                               char                      **raw_name,
                                               char                      **raw_version);
typedef void     (*CheckPackageFunction)      (HippoDistribution         *distirbution,
                                               const char                *package_name,
                                               const char                *source,
                                               HippoCheckPackageCallback  callback,
                                               void                      *callback_data);
typedef void     (*InstallPackageFunction)    (const char                *package_name,
                                               HippoAsyncCallback         callback,
                                               void                      *callback_data);

static gboolean check_distribution_lsb_release    (const char                 *filename,
                                                   char                      **raw_name,
                                                   char                      **raw_version);
static gboolean check_distribution_redhat_release (const char                 *filename,
                                                   char                      **raw_name,
                                                   char                      **raw_version);
static void     check_package_rpm                 (HippoDistribution          *distribution,
                                                   const char                 *package_name,
                                                   const char                 *source,
                                                   HippoCheckPackageCallback   callback,
                                                   void                       *callback_data);
static void     install_package_pirut             (const char                 *package_name,
                                                   HippoAsyncCallback          callback,
                                                   void                       *callback_data);

#ifdef ENABLE_INSTALL_PACKAGE_YUM
static void     install_package_yum               (const char                 *package_name,
                                                   HippoAsyncCallback          callback,
                                                   void                       *callback_data);
#endif    

static InstallPackageFunction hippo_distribution_find_install_package_function(HippoDistribution *distribution,
                                                                               const char        *source);

static const struct {
    const char *source;
    CheckPackageFunction func;
    char *minimum_version;
} check_package_functions[] = {
    { "fedora", check_package_rpm, NULL }
};
    
static const struct {
    const char *source;
    InstallPackageFunction func;
    char *minimum_version;
} install_package_functions[] = {
    { "fedora", install_package_pirut, "6.91" },
#ifdef ENABLE_INSTALL_PACKAGE_YUM
    { "fedora", install_package_yum, NULL }
#endif
};
    
static const struct {
    const char *filename;
    CheckDistributionFunction func;
} check_distribution_functions[] =  {
    { "/etc/redhat-release", check_distribution_redhat_release },
    { "/etc/fedora-release", check_distribution_redhat_release },
    { NULL,                  check_distribution_lsb_release }
};

struct HippoDistribution {
    char *name;
    char *version;
    char *architecture;
    GSList *sources;
};


/************************************************************
 *                       Utilities                          *
 ************************************************************/

typedef void (*ExecuteCommandCallback) (GError                    *error,
                                        int                        status,
                                        const char                *output,
                                        void                      *callback_data);

typedef struct{
    ExecuteCommandCallback callback;
    void *callback_data;

    GError *error;
    int status;
    GString *output;
    
    gboolean exited;
    gboolean closed;
} ExecuteCommandAsyncClosure;
    
static void
hippo_execute_child_watch_finish(ExecuteCommandAsyncClosure *closure)
{
    closure->callback(closure->error, closure->status, closure->output->str, closure->callback_data);
    if (closure->error)
        g_error_free(closure->error);
    
    g_string_free(closure->output, TRUE);
    g_free(closure);
}
     
static void
hippo_execute_async_child_watch(GPid     pid,
                                int      status,
                                gpointer data)
{
    ExecuteCommandAsyncClosure *closure = data;

    closure->exited = TRUE;
    closure->status = status;

    if (closure->exited && closure->closed)
        hippo_execute_child_watch_finish(closure);
}
    
static gboolean
hippo_execute_async_io_watch(GIOChannel   *source,
                             GIOCondition  condition,
                             gpointer      data)
{
    ExecuteCommandAsyncClosure *closure = data;
    GIOStatus status;
    char buf[1024];
    gsize bytes_read;
    
    status = g_io_channel_read_chars(source, buf, sizeof(buf), &bytes_read, &closure->error);
    switch (status) {
    case G_IO_STATUS_ERROR:
        g_io_channel_close(source);
        closure->closed = TRUE;
        if (closure->exited && closure->closed)
            hippo_execute_child_watch_finish(closure);
        return FALSE;
    case G_IO_STATUS_NORMAL:
        g_string_append_len(closure->output, buf, bytes_read);
        break;
    case G_IO_STATUS_EOF:
        g_io_channel_close(source);
        closure->closed = TRUE;
        if (closure->exited && closure->closed)
            hippo_execute_child_watch_finish(closure);
        
        return FALSE;
    case G_IO_STATUS_AGAIN:
        /* Should not be reached */
        break;
    }

    return TRUE;
}

static void
hippo_execute_command_async(char                  **argv,
                            ExecuteCommandCallback  callback,
                            void                   *callback_data)
{
    ExecuteCommandAsyncClosure *closure;
    GPid child_pid;
    int out_fd;
    GIOChannel *channel;
    const char *locale_encoding;

    closure = g_new(ExecuteCommandAsyncClosure, 1);
    
    closure->callback = callback;
    closure->callback_data = callback_data;
    
    closure->error = NULL;
    closure->status = -1;
    closure->output = g_string_new(NULL);

    closure->closed = FALSE;
    closure->exited = FALSE;
    
    if (!g_spawn_async_with_pipes(NULL, argv, NULL,
                                  G_SPAWN_DO_NOT_REAP_CHILD,
                                  NULL, NULL,
                                  &child_pid,
                                  NULL, &out_fd, NULL,
                                  &closure->error)) {
        hippo_execute_child_watch_finish(closure);
        return;
    }

    channel = g_io_channel_unix_new(out_fd);
    
    g_get_charset(&locale_encoding);
    g_io_channel_set_encoding(channel, locale_encoding, NULL);

    g_io_add_watch(channel,
                   G_IO_IN | G_IO_HUP,
                   hippo_execute_async_io_watch,
                   closure);
    g_io_channel_unref(channel);

    g_child_watch_add(child_pid,
                      hippo_execute_async_child_watch,
                      closure);
}

static gboolean
hippo_execute_command_sync(char                  **argv,
                           GError                **error_out,
                           int                    *status_out,
                           char                  **output_out)
{
    GPid child_pid;
    int out_fd;
    GIOChannel *channel;
    const char *locale_encoding;
    char *output = NULL;
    int status = -1;
    GError *error = NULL;
    gboolean success;

    g_return_val_if_fail(error_out == NULL || *error_out == NULL, FALSE);

    if (!g_spawn_async_with_pipes(NULL, argv, NULL,
                                  G_SPAWN_DO_NOT_REAP_CHILD,
                                  NULL, NULL,
                                  &child_pid,
                                  NULL, &out_fd, NULL,
                                  &error))
        goto out;

    channel = g_io_channel_unix_new(out_fd);
    
    g_get_charset(&locale_encoding);
    g_io_channel_set_encoding(channel, locale_encoding, NULL);

    g_io_channel_read_to_end(channel, &output, NULL, &error);
    
    g_io_channel_close(channel);
    g_io_channel_unref(channel);
    
 again:
    if (waitpid((pid_t)child_pid, &status, 0) < 0) {
        if (errno == EINTR)
            goto again;

        g_warning("waitpid failed: %s", g_strerror(errno));
    }

 out:
    success = error == NULL && status == 0;

    if (error)
        g_propagate_error(error_out, error);
    if (status_out)
        *status_out = status;

    if (success) {
        if (output_out)
            *output_out = output;
    } else {
        g_free(output);
    }
    
    return success;
}

static char *
dup_strip_and_check_empty(char *start, char *end)
{
    char *result = g_strstrip(g_strndup(start, end - start));
    if (!*result) {
        g_free(result);
        return NULL;
    }

    return result;
}


/************************************************************/

static char *
find_lsb_release_field(char *output,
                       char *field_name)
{
    char *line;
    char *s, *t;

    line = output;
    while (TRUE) {
        if (!g_str_has_prefix(line, field_name))
            goto next_line;
        
        s = line + strlen(field_name);
        while (g_ascii_isspace(*s))
            s++;
            
        if (*s != ':')
            goto next_line;

        s++;
        t = strchr(s, '\n');
        if (t == NULL)
            t = s + strlen(s);

        return dup_strip_and_check_empty(s, t);

    next_line:
        line = strchr(line, '\n');
        if (line == NULL)
            break;
        line++;
    }

    return NULL;
}

static gboolean
check_distribution_lsb_release(const char   *filename,
                               char        **raw_name,
                               char        **raw_version)
{
    char *output;
    int status;
    GError *error = NULL;
    char *name, *version;
    
    const char *args[] = {
        "/usr/bin/lsb_release",
        "-i", "-r",
        NULL,
    };

    if (!hippo_execute_command_sync((char **)args, &error, &status, &output)) {
        if (error != NULL) {
            g_debug("Couldn't execute lsb_release: %s", error->message);
            g_error_free(error);
        } else {
            g_debug("lsb_release returned status: %d", status);
        }

        return FALSE;
    }

    name = find_lsb_release_field(output, "Distributor ID");
    version = find_lsb_release_field(output, "Release");

    g_free(output);
    
    if (name && version) {
        *raw_name = name;
        *raw_version = version;

        return TRUE;
    } else {
        g_free(name);
        g_free(version);
        
        g_debug("Couldn't parse the output of lsb_release");
        return FALSE;
    }
}

/************************************************************/

static gboolean
check_distribution_redhat_release(const char   *filename,
                                  char        **raw_name,
                                  char        **raw_version)
{
    char *contents;
    GError *error = NULL;
    char *name = NULL;
    char *version = NULL;
    char *s, *t;
    
    if (!g_file_get_contents(filename, &contents, NULL, &error)) {
        if (!g_error_matches(error, G_FILE_ERROR, G_FILE_ERROR_NOENT))
            g_debug("%s", error->message);
        g_error_free(error);
        
        return FALSE;
    }

    s = strchr(contents, '\n');
    if (s != NULL)
        s = '\0';

    /* Look for \s+release\s+ */

    s = contents;
    while (TRUE) {
        s = strstr(s, "release");
        if (s == NULL)
            break;
        
        if (s != contents && g_ascii_isspace(*(s - 1)) && g_ascii_isspace(*(s + strlen("release"))))
            break;
            
        s += strlen("release");
    }

    if (s != NULL) {
        name = dup_strip_and_check_empty(contents, s);

        s += strlen("release");
        t = strchr(s, '(');
        if (t == NULL)
            t = s + strlen(s);

        version = dup_strip_and_check_empty(s, t);
    }
    
    g_free(contents);
    
    if (name && version) {
        *raw_name = name;
        *raw_version = version;
        
        return TRUE;
    } else {
        g_free(name);
        g_free(version);
        
        g_debug("Couldn't parse the contents of '%s'", filename);
        return FALSE;
    }
}

/************************************************************/

typedef struct {
    HippoDistribution *distribution;
    char *source;
    HippoCheckPackageCallback callback;
    void *callback_data;
} CheckPackageRpmClosure;

static void
check_package_rpm_callback(GError      *error,
                           int          status,
                           const char  *output,
                           void        *callback_data)
{
    CheckPackageRpmClosure *closure = callback_data;
    char *version = NULL;

    if (error != NULL) {
        g_warning("Could not run RPM to get package status: %s\n", error->message);
    } else if (status == 0) {
        char *s;

        version = g_strdup(output);
        s = strchr(version, '\n');
        if (s != NULL)
            *s = '0';

        g_strstrip(version);
        if (strcmp(version, "") == 0) {
            g_free(version);
            version = NULL;
        }
    }

    closure->callback(version != NULL,
                      hippo_distribution_find_install_package_function(closure->distribution, closure->source) != NULL,
                      version,
                      closure->callback_data);
    g_free(version);
    g_free(closure->source);
    g_free(closure);
}

static void
check_package_rpm(HippoDistribution         *distribution,
                  const char                *package_name,
                  const char                *source,
                  HippoCheckPackageCallback  callback,
                  void                      *callback_data)
{
    const char *args[] = {
        "/bin/rpm",
        "-q",
        "--queryformat=%{version}-%{release}\\n",
        NULL,
        NULL,
    };
    CheckPackageRpmClosure *closure;

    closure = g_new(CheckPackageRpmClosure, 1);
    closure->distribution = distribution;
    closure->source = g_strdup(source);
    closure->callback = callback;
    closure->callback_data = callback_data;
    
    args[3] = package_name;
    hippo_execute_command_async((char **)args, check_package_rpm_callback, closure);
}


/************************************************************/

typedef struct {
    HippoAsyncCallback callback;
    void *callback_data;
} InstallPackageExecClosure;

static void
install_package_exec_callback(GError      *error,
                              int          status,
                              const char  *output,
                              void        *callback_data)
{
    InstallPackageExecClosure *closure = callback_data;

    closure->callback(error, closure->callback_data);
    
    g_free(closure);
}

static void
install_package_exec(char              **args,
                     HippoAsyncCallback  callback,
                     void               *callback_data)
{
    InstallPackageExecClosure *closure;

    closure = g_new(InstallPackageExecClosure, 1);
    closure->callback = callback;
    closure->callback_data = callback_data;
    
    hippo_execute_command_async((char **)args, install_package_exec_callback, closure);
}


/************************************************************/

static void
install_package_pirut(const char         *package_name,
                      HippoAsyncCallback  callback,
                      void               *callback_data)
{
    const char *args[] = {
        "/usr/bin/system-install-packages",
        NULL,
        NULL,
    };

    args[1] = package_name;
    install_package_exec((char **)args, callback, callback_data);
}

#ifdef ENABLE_INSTALL_PACKAGE_YUM

/************************************************************/

static void
install_package_yum(const char         *package_name,
                    HippoAsyncCallback  callback,
                    void               *callback_data)
{
    const char *args[] = {
        "/usr/bin/gnome-terminal",
        "--disable-factory",
        "-x",
        "sudo",
        "yum",
        "install",
        NULL,
        NULL,
    };

    args[6] = package_name;
    install_package_exec((char **)args, callback, callback_data);
}
#endif


/************************************************************/

static const char *
get_canonical_name(const char *raw_name)
{
    static const struct {
        const char *raw_name;
        const char *canonical_name;
    } canonical_name_map[] = {
        { "Fedora Core", "Fedora" },
        { "FedoraCore",  "Fedora" },
    };

    guint i;

    for (i = 0; i < G_N_ELEMENTS(canonical_name_map); i++) {
        if (strcmp(canonical_name_map[i].raw_name, raw_name) == 0)
            return canonical_name_map[i].canonical_name;
    }

    return raw_name;
}

static void
find_architecture(HippoDistribution *distro)
{
    /* We really are interested in the architecture of this Mugshot binary for
     * determining the appropriate upgrade, not the architecture of the system;
     * someone might be running the x86 binary of Mugshot on a x86_64 machine
     * to match their usage of a 32-bit Firefox.
     */
#if 1
    distro->architecture = g_strdup(HOST_CPU);
#else    
    GError *error = NULL;
    int status;
    const char *args[] = {
        "/bin/uname",
        "-i",
        NULL
    };

    if (hippo_execute_command_sync((char **)args, &error, &status, &distro->architecture)) {
        g_strstrip(distro->architecture);
    } else {
        if (error) {
            g_warning("Couldn't run uname -i to get architecture: %s\n", error->message);
            g_error_free(error);
        } else {
            g_warning("uname -i failed");
        }
    }
#endif
}

static void
find_distribution_information(HippoDistribution *distro)
{
    guint i;

    for (i = 0; i < G_N_ELEMENTS(check_distribution_functions); i++) {
        char *raw_name;
        char *raw_version;
        
        if (check_distribution_functions[i].func(check_distribution_functions[i].filename, &raw_name, &raw_version)) {
            distro->name = g_strdup(get_canonical_name(raw_name));
            distro->version = g_strdup(raw_version);

            g_free(raw_name);
            g_free(raw_version);

            /* Default source for a distribution is the lower-cased version of the
             * distribution name.
             */
            if (distro->name) {
                char *source = g_ascii_strdown(distro->name, -1);
                distro->sources = g_slist_prepend(distro->sources, source);
            }
            
            break;
        }
    }

    find_architecture(distro);

    g_debug("Distribution Name: %s", distro->name ? distro->name : "<unknown>");
    g_debug("Distribution Version: %s", distro->version ? distro->version : "<unknown>");
    g_debug("Distribution Architecture: %s", distro->architecture ? distro->architecture : "<unknown>");
}

static void
hippo_distribution_init(HippoDistribution *distro)
{
    find_distribution_information(distro);
}

GQuark
hippo_distribution_error_quark(void) {
    return g_quark_from_static_string("hippo-distribution-error");
}

HippoDistribution *
hippo_distribution_get(void)
{
    static HippoDistribution *singleton_distro;

    if (singleton_distro == NULL) {
        singleton_distro = g_new0(HippoDistribution, 1);
        hippo_distribution_init(singleton_distro);
    }

    return singleton_distro;
}

const char *
hippo_distribution_get_name(HippoDistribution *distro)
{
    g_return_val_if_fail(distro != NULL, NULL);

    return distro->name;
}

const char *
hippo_distribution_get_version(HippoDistribution *distro)
{
    g_return_val_if_fail(distro != NULL, NULL);

    return distro->version;
}

const char *
hippo_distribution_get_architecture(HippoDistribution *distro)
{
    g_return_val_if_fail(distro != NULL, NULL);

    return distro->architecture;
}

static gboolean
validate_name(const char *name)
{
    const char *p;
    
    for (p = name; *p; p++) {
        char c = *p;
        
        if (!((c >= 'A' && c <= 'Z') ||
              (c >= 'a' && c <= 'z') ||
              (c >= '0' && c <= '9') ||
              (c == '.') ||
              (c == '_') ||
              (c == '-')))
            return FALSE;
    }

    return TRUE;
}

static gboolean
find_package_name(HippoDistribution *distro,
                  const char        *package_names,
                  char             **package,
                  char             **source)
{
    char **pairs = g_strsplit(package_names, ";", -1);
    char **p;
    gboolean found = FALSE;
    
    for (p = pairs; *p && !found; p++) {
        char *pair = *p;
        char *equal = strchr(pair, '=');
        char *source_string;
        char **sources;
        char **s;
        
        if (equal == NULL) {
            g_warning("Bad package name component: '%s'", pair);
            continue;
        }

        source_string = g_strndup(pair, equal - pair);
        sources = g_strsplit(source_string, ",", -1);
        for (s = sources; *s && !found; s++) {
            char *source_name = g_ascii_strdown(*s, -1);
            GSList *l;
            
            g_strstrip(source_name);

            if (!validate_name(source_name)) {
                g_warning("Bad source name: '%s'", source_name);
                goto next_source;
            }

            for (l = distro->sources; l; l = l->next) {
                if (strcmp(l->data, source_name) == 0) {
                    char *package_name = g_strstrip(g_strdup(equal + 1));
                    if (!validate_name(package_name)) {
                        g_warning("Bad package name: '%s'", package_name);
                        g_free(package_name);
                        
                        goto next_source;
                    }
                    
                    *package = package_name;
                    *source = g_strdup(source_name);
                    found = TRUE;
                    
                    break;
                }
            }

        next_source:
            g_free(source_name);
        }

        g_strfreev(sources);
        g_free(source_string);
    }
    
    g_strfreev(pairs);

    return found;
}

/* cheesy comparison for dotted version strings; segments separated
 * by dots are compared by:
 *   primary:   numeric comparison of initial digit portion (empty == 0)
 *   secondary: string comparison of anything after initial digit portion
 *
 * so,
 *   1.01 == 1.1
 *   1.0.4 < 1.0.4a
 *   1.a < 1.1
 */
static int
compare_versions(const char *version_a, const char *version_b)
{
    while (TRUE) {
        const char *end_a;
        const char *end_b;
        int i_a, i_b;
        int string_cmp;
        
        i_a = atoi(version_a);
        i_b = atoi(version_b);

        if (i_a != i_b)
            return i_a < i_b ? -1 : 1;

        while (g_ascii_isdigit(*version_a))
            version_a++;
        while (g_ascii_isdigit(*version_b))
            version_b++;

        end_a = strchr(version_a, '.');
        if (end_a == NULL)
            end_a = version_a + strlen(version_a);
        end_b = strchr(version_b, '.');
        if (end_b == NULL)
            end_b = version_b + strlen(version_b);

        string_cmp = strncmp(version_a, version_b, MIN(end_a - version_a, end_b - version_b));
        if (string_cmp != 0)
            return string_cmp;

        if (end_a - version_a != end_b - version_b) {
            return end_a - version_a < end_b - version_b ? -1 : 1;
        }

        if (*end_a && *end_b) { /* Another segment for both */
            version_a = end_a + 1;
            version_b = end_b + 1;
        } else {
            if (!*end_a && !*end_b)
                return 0;
            else if (!*end_a)
                return -1;
            else
                return 1;
        }
    }
}

static InstallPackageFunction
hippo_distribution_find_install_package_function(HippoDistribution *distro,
                                                 const char        *source)
{
    guint i;
    
    for (i = 0; i < G_N_ELEMENTS(install_package_functions); i++) {
        if (strcmp(install_package_functions[i].source, source) == 0) {
            if (install_package_functions[i].minimum_version
                && compare_versions(distro->version, install_package_functions[i].minimum_version) < 0)
                continue;
            
            return install_package_functions[i].func;
        }
    }

    return NULL;
}

void
hippo_distribution_check_package(HippoDistribution         *distro,
                                 const char                *package_names,
                                 HippoCheckPackageCallback  callback,
                                 void                      *callback_data)
{
    char *package_name = NULL;
    char *source = NULL;
    guint i;

    g_return_if_fail(distro != NULL);

    if (!find_package_name(distro, package_names, &package_name, &source)) {
        (*callback) (FALSE, FALSE, NULL, callback_data);
        return;
    }

    for (i = 0; i < G_N_ELEMENTS(check_package_functions); i++) {
        if (strcmp(check_package_functions[i].source, source) == 0) {
            if (check_package_functions[i].minimum_version
                && compare_versions(distro->version, check_package_functions[i].minimum_version) < 0)
                continue;
            
            check_package_functions[i].func(distro, package_name, source, callback, callback_data);
            goto found;
        }
    }

    (*callback) (FALSE, FALSE, NULL, callback_data);

 found:
    g_free(package_name);
    g_free(source);
}

void
hippo_distribution_install_package(HippoDistribution   *distro,
                                   const char          *package_names,
                                   HippoAsyncCallback   callback,
                                   void                *callback_data)
{
    char *package_name = NULL;
    char *source = NULL;
    GError *error = NULL;
    InstallPackageFunction func;

    g_return_if_fail(distro != NULL);

    if (!find_package_name(distro, package_names, &package_name, &source)) {
        error = g_error_new(HIPPO_DISTRIBUTION_ERROR, HIPPO_DISTRIBUTION_ERROR_NO_SOURCE, "No package found for available sources");
        goto out;
    }

    func = hippo_distribution_find_install_package_function(distro, source);
    if (!func) {
        error = g_error_new(HIPPO_DISTRIBUTION_ERROR, HIPPO_DISTRIBUTION_ERROR_CANNOT_INSTALL, "Don't know how to install packages for source '%s'", source);
        goto out;
    }

    (*func) (package_name, callback, callback_data);

 out:
    if (error) {
        (*callback) (error, callback_data);
        g_error_free(error);
    }
    
    g_free(package_name);
    g_free(source);
}

static char *
find_desktop_file(HippoDistribution *distro,
                  const char        *desktop_names)
{
    char **names;
    char *found_file = NULL;
    int i;
        
    names = g_strsplit(desktop_names, ";", -1);
    for (i = 0; names[i]; i++) {
        char *filename;
        char *path;
        
        g_strstrip(names[i]);

        if (!validate_name(names[i])) {
            g_warning("Bad desktop name: '%s'", names[i]);
            continue;
        }
        
        filename = g_strconcat(names[i], ".desktop", NULL);
        path = g_build_filename(APPLICATION_DIR, filename, NULL);
        g_free(filename);

        if (g_file_test(path, G_FILE_TEST_EXISTS)) {
            found_file = path;
            break;
        }

        g_free(path);
    }

    g_strfreev(names);

    return found_file;
}
                  
void
hippo_distribution_check_application (HippoDistribution             *distro,
                                      const char                    *desktop_names,
                                      HippoCheckApplicationCallback  callback,
                                      void                          *callback_data)
{
    char *desktop_file;

    g_return_if_fail(distro != NULL);

    desktop_file = find_desktop_file(distro, desktop_names);
    
    (*callback)(desktop_file != NULL, callback_data);

    g_free(desktop_file);
}

static guint32
get_server_timestamp()
{
    GtkWidget *invisible = gtk_invisible_new();
    gtk_widget_realize(invisible);
    return gdk_x11_get_server_time(invisible->window);
    gtk_widget_destroy(invisible);
}

void
hippo_distribution_run_application   (HippoDistribution  *distro,
                                      const char         *desktop_names,
                                      guint32             launch_time,
                                      HippoAsyncCallback  callback,
                                      void               *callback_data)
{
#ifndef WITH_MAEMO
    GnomeDesktopItem *item = NULL;
    char *desktop_file = NULL;
    GError *error = NULL;
    
    g_return_if_fail(distro != NULL);

    /* This is idempotent and fairly cheap, so do it here to avoid initializing
     * gnome-vfs on application startup
     */
    gnome_vfs_init();

    desktop_file = find_desktop_file(distro, desktop_names);
    if (!desktop_file) {
        error = g_error_new(HIPPO_DISTRIBUTION_ERROR, HIPPO_DISTRIBUTION_ERROR_NO_APPLICATION, "Can't find application to launch");
        goto out;
    }

    item = gnome_desktop_item_new_from_file(desktop_file, GNOME_DESKTOP_ITEM_LOAD_NO_TRANSLATIONS, &error);
    if (!item)
        goto out;

    /* Nobody ever wants their windows to *always* get focus-steal-prevented; if we don't have a better
     * timestamp for the user action, use the current server timestamp.
     */
    if (launch_time == GDK_CURRENT_TIME)
        gnome_desktop_item_set_launch_time(item, get_server_timestamp());
    
    if (gnome_desktop_item_launch(item, NULL, 0, &error)) {
        (*callback)(NULL, callback_data);
    }

 out:
    if (error) {
        (*callback) (error, callback_data);
        g_error_free(error);
    }

    if (item)
        gnome_desktop_item_unref(item);
    
    g_free(desktop_file);
#endif
}


#ifdef TEST_DISTRIBUTION
/* Compile with:
 *
 *    gcc -o test-distribution hippo-distribution.c `pkg-config --cflags --libs gtk+-2.0 gnome-vfs-2.0 gnome-desktop-2.0` -Wall -DTEST_DISTRIBUTION -DHOST_CPU=\"i386\"
 */

static void
usage()
{
    g_printerr("Usage: test-distribution check_application DESKTOP_NAMES");
    g_printerr("       test-distribution run_application   DESKTOP_NAMES");
    g_printerr("       test-distribution check_package     PACKAGE_NAMES");
    g_printerr("       test-distribution install_package   PACKAGE_NAMES");
}

static void
test_check_application_cb(gboolean is_runnable,
                          void    *data)
{
    GMainLoop *loop = data;
    g_main_loop_quit(loop);

    g_print("check_application results:\n");
    g_print("    is_runnable: %d\n", is_runnable);
}

static void
test_run_application_cb(GError  *error,
                        void    *data)
{
    GMainLoop *loop = data;
    g_main_loop_quit(loop);

    if (error) {
        g_print("run_application() failed: %s\n", error->message);
    } else {
        g_print("run_application() succeeded\n");
    }
}

static void
test_check_package_cb(gboolean    is_installed,
                      gboolean    is_installable,
                      const char *installed_version,
                      void       *data)
{
    GMainLoop *loop = data;
    g_main_loop_quit(loop);

    g_print("check_package results:\n");
    g_print("    is_installed: %d\n", is_installed);
    g_print("    is_installable: %d\n", is_installable);
    g_print("    version: %s\n", installed_version ? installed_version : "<unknown>");
}

static void
test_install_package_cb(GError  *error,
                        void    *data)
{
    GMainLoop *loop = data;
    g_main_loop_quit(loop);

    if (error) {
        g_print("install_application() failed: %s\n", error->message);
    } else {
        g_print("install_application() succeeded\n");
    }
}

int
main(int argc, char **argv) {
    HippoDistribution *distro = hippo_distribution_get();
    GMainLoop *loop = g_main_loop_new(NULL, TRUE);

    /* Need to initialize GTK+ for startup notification */
    gtk_init(&argc, &argv);

    if (argc == 1)
        return 0;

    if (argc == 4 && strcmp(argv[1], "compare_versions") == 0) {
        char sym;
        switch(compare_versions(argv[2], argv[3])) {
        case 0:
            sym = '=';
            break;
        case -1:
            sym = '<';
            break;
        case 1:
            sym = '>';
            break;
        default:
            sym = '?';
            break;
        }
        printf("%c\n", sym);
        return 0;
    }

    if (argc != 3) {
        usage();
        return 1;
    }

    if (strcmp(argv[1], "check_application") == 0) {
        hippo_distribution_check_application(distro, argv[2], test_check_application_cb, loop);
    } else if (strcmp(argv[1], "run_application") == 0) {
        hippo_distribution_run_application(distro, argv[2], GDK_CURRENT_TIME, test_run_application_cb, loop);
    } else if (strcmp(argv[1], "check_package") == 0) {
        hippo_distribution_check_package(distro, argv[2], test_check_package_cb, loop);
    } else if (strcmp(argv[1], "install_package") == 0) {
        hippo_distribution_install_package(distro, argv[2], test_install_package_cb, loop);
    } else {
        usage();
        return 1;
    }

    if (g_main_loop_is_running(loop))
        g_main_loop_run(loop);

    return 0;
}
#endif
