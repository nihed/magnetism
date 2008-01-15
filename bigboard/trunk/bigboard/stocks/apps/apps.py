import logging, time, re, os, sys, string

from ddm import DataModel

import gmenu, gobject, pango, gnomedesktop, gtk
import gconf, hippo

import bigboard
import bigboard.globals as globals
import bigboard.global_mugshot as global_mugshot
import bigboard.libbig as libbig
from bigboard.libbig.gutil import *
import bigboard.apps_directory as apps_directory
from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard.libbig.xmlquery import query as xml_query, get_attrs as xml_get_attrs
import urlparse
import xml.dom, xml.dom.minidom
import bigboard.search as search

_logger = logging.getLogger("bigboard.stocks.AppsStock")

class Application(object):
    __slots__ = ['__app', '__install_checked', '__desktop_entry', '__menu_entry', '__resource', '__pinned']
    def __init__(self, resource=None, menu_entry=None):
        super(Application, self).__init__()
        self.__resource = resource
        self.__install_checked = False
        self.__desktop_entry = None
        self.__menu_entry = menu_entry 
        self.__pinned = False
        
    def get_id(self):
        return self.__resource and self.__resource.id or self.__menu_entry.get_desktop_file_path()

    def get_app_name_from_file_name(self):
        # this is intended for local apps
        if not self.__menu_entry:
            return ""

        file_path = self.__menu_entry.get_desktop_file_path()     
        app_name_start = file_path.rfind("/") + 1
        app_name_end = file_path.rfind(".")
        #_logger.debug("name %s file path %s start %s end %s", self.get_name(), file_path, app_name_start, app_name_end)
        if app_name_start < 0 or app_name_end < 0:
            return ""
        else:
            return file_path[app_name_start:app_name_end]

    def is_local(self):
        return self.__resource is None
        
    def get_resource(self):
        return self.__resource

    def get_menu(self):
        return self.__menu_entry

    def get_desktop(self):
        return self.__desktop_entry

    def get_exec_format_string(self):
        return self.__desktop_entry and self.__desktop_entry.get_string("Exec") or None

    def get_pinned(self):
        return self.__pinned

    def set_pinned(self, pinned):
        self.__pinned = pinned
    
    def get_name(self):
        return self.__resource and self.__resource.name or self.__menu_entry.get_name()
    
    def get_description(self):
        return self.__resource and self.__resource.description or ""

    def get_tooltip(self):
        return self.__resource and self.__resource.tooltip or ""
    
    def get_generic_name(self):
        return self.__resource and self.__resource.genericName or ""

    def get_usage_count(self):
        return self.__resource and int(self.__resource.usageCount) or 0

    def get_category(self):
        ## FIXME should this be category or categoryDisplayName ?       
        return self.__resource and self.__resource.categoryDisplayName or self.get_local_category()

    def get_local_category(self):
        return ((self.__menu_entry and self.__menu_entry.parent) and self.__menu_entry.parent.get_name()) or "Other"

    def get_comment(self):
        return self.__desktop_entry and self.__desktop_entry.get_localestring('Comment')

    def get_is_excluded(self):
        return self.__menu_entry and self.__menu_entry.get_is_excluded()
        
    def get_icon_url(self):
        return self.__resource and self.__resource.iconUrl
        
    def get_local_pixbuf(self):
        if self.__desktop_entry:
            # strip off .png
            icon_name = re.sub(r'\.[a-z]+$','', self.__menu_entry.get_icon()) 
            theme = gtk.icon_theme_get_default()
            try:
                pixbuf = theme.load_icon(icon_name, 48, 0)
            except gobject.GError, e:
                return None
            return pixbuf
        return None

    def set_resource(self, resource):
        self.__resource = resource

    def __lookup_desktop(self):
        if self.__menu_entry:
            entry_path = self.__menu_entry.get_desktop_file_path()
            try:
                desktop = gnomedesktop.item_new_from_file(entry_path, 0)
                if desktop:
                    return desktop            
            except gobject.GError, e:
                desktop = None
        if not self.__resource:
            return None
        names = self.__resource.desktopNames
        for name in names.split(';'):
            ad = apps_directory.get_app_directory()            
            menuitem = None
            try:
                menuitem = ad.lookup(name)
            except KeyError, e:
                pass
                
            if menuitem and not self.__menu_entry:
                self.__menu_entry = menuitem
                entry_path = menuitem.get_desktop_file_path()
            else:
                entry_path = None
                for dir in libbig.get_xdg_datadirs():
                    appdir = os.path.join(dir, 'applications')
                    path = os.path.join(appdir, name + '.desktop') 
                    if os.access(path, os.R_OK):
                        entry_path = path
                        break
                                                   
            if not entry_path:
                continue
            desktop = gnomedesktop.item_new_from_file(entry_path, 0)
            if desktop:
                return desktop
        return None

    def is_installed(self):
        if not self.__install_checked:
            self.recheck_installed()            
        self.__install_checked = True               
        return self.__desktop_entry is not None      
   
    def recheck_installed(self):
        old_installed = self.__desktop_entry is not None
        self.__desktop_entry = self.__lookup_desktop()

    ## called only by apps repo
    def _do_launch(self):
        if self.__desktop_entry:
            self.__desktop_entry.launch([])
        else:
            global_mugshot.get_mugshot().install_application(self.__resource.id, self.__resource.packageNames, self.__resource.desktopNames)            
    
    def launch(self):
        get_apps_repo().launch(self)        

    def __str__(self):
        return "<App:%s,local:%d,usageCount:%s>" % (self.get_name(), self.is_local(), self.get_usage_count())

    def __repr__(self):
        return self.__str__()

## one-shot object that does an http fetch              
class AppsHttpDownloader:
    def __init__(self, relative_url, handler):
        self.__relative_url = relative_url
        self.__handler = handler
        
    def __load_app_from_xml(self, node):
        id = node.getAttribute("id")
        #_logger.debug("parsing application id=%s", id)
        attrs = xml_get_attrs(node, ['id', 'rank', 'usageCount', 
                                     'iconUrl', 
                                     'category',
                                     'name', 'desktopNames', 'packageNames',
                                     ('tooltip', True),
                                     ('genericName', True)
                                    ])
        description = xml_query(node, 'description#')
        if description:
            attrs['description'] = description

        ## the old http format uses 'category' for what the data model
        ## calls 'categoryDisplayName'
        if attrs.has_key('category') and not attrs.has_key('categoryDisplayName'):
            attrs['categoryDisplayName'] = attrs['category']
            del attrs['category']

        return attrs
    
    def __parse_app_set(self, expected_name, doc=None, child_nodes=None):
        if doc:
            root = doc.documentElement
            if not root.nodeName == expected_name:
                self._logger.warn("invalid root node, expected %s", expected_name)
                return []
        else:
            root = None
        apps = []
        for node in (child_nodes or (root and root.childNodes) or []):
            if not (node.nodeType == xml.dom.Node.ELEMENT_NODE):
                continue
            app = self.__load_app_from_xml(node)
            apps.append(app)
        #_logger.debug("Parsed app set; pinned_apps = " + str(map(lambda a: a.get_id(), apps)))
        return apps

    ## this is called twice, which presumably has something to do with the refetch thing,
    ## I don't really understand what a "refetch" is and there's no comments in http.py
    def __on_got_xml(self, url, child_nodes, is_refetch=False):
        _logger.debug("Got XML reply from http request %s" % self.__relative_url)
        reply_root = child_nodes[0]
        apps = self.__parse_app_set('applications',
                                    child_nodes=reply_root.childNodes)
        self.__handler(apps)

    def __on_error(self, url, args):
        if 'status' in args and args['status'] == '504':
            _logger.debug("don't have local cache for %s", url)
        else:
            _logger.error("failed to get http request %s: %s", self.__relative_url, args)
        self.__handler([])
        
    def __do_download(self):
        baseurl = globals.get_baseurl()
        if not baseurl:
            raise Exception("Don't have base url yet but trying to do an http request")

        url = urlparse.urljoin(baseurl, self.__relative_url)
        _logger.debug("Sending http request for %s" % url)
        
        AsyncHTTPFetcher().xml_method_refetch(url,
                                              {},
                                              self.__on_got_xml,
                                              self.__on_error,
                                              self.__on_error)

    def go(self):
        self.__do_download()

class AppsRepo(gobject.GObject):
    __gsignals__ = {
        "enabled-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()), 
        "all-apps-loaded" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, ()),
        "my-pinned-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "my-top-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "global-top-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "local-apps-changed" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,)),
        "app-launched" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_PYOBJECT,))
        }
    def __init__(self, *args, **kwargs):
        super(AppsRepo, self).__init__(*args, **kwargs)        

        self.__model = bigboard.globals.get_data_model()
        self.__model.add_ready_handler(self.__on_ready)
        
        self.__myself = None
    
        self.__global_top_apps = []
        self.__my_top_apps = []
        self.__my_pinned_apps = []
        
        self.__ddm_apps = {} # app_resource.id -> Application object
        # apps installed locally and not known in data model
        self.__local_apps = {} # desktop name -> Application object

        ## all apps (local or data model)
        self.__all_apps = set()

        ad = apps_directory.get_app_directory()
        ad.connect('changed', self.__on_local_apps_changed)
        for menu in ad.get_apps():
            self.get_app_for_menu_entry(menu)

        self.__category_queries = {} ## category name to Query
        self.__search_queries = {} ## search terms to Query

        self.__get_all_apps_pending = False
        self.__got_all_apps = False
        self.__get_popular_apps_pending = False
        self.__got_popular_apps = False
        
        if self.__model.ready:
            self.__on_ready()        

    def __on_ready(self):
        # When we disconnect from the server we freeze existing content, then on reconnect
        # we clear everything and start over.
        _logger.debug("Data model now ready")

        if self.__model.self_resource != None:
            query = self.__model.query_resource(self.__model.self_resource, "topApplications[+;description;category;categoryDisplayName;packageNames];pinnedApplications[+;description;category;categoryDisplayName;packageNames];applicationUsageEnabled;applicationUsageStart")
            query.add_handler(self.__on_got_self)
            query.add_error_handler(lambda code, msg: self.__on_query_error("self resource", code, msg))
            query.execute()

            query = self.__model.query(("http://online.gnome.org/p/application", "getPopularApplications"),
                                       "+;description;category;categoryDisplayName;packageNames",
                                       single_result=False,
                                       start=0)

            query.add_handler(self.__on_got_global_popular_apps)
            query.add_error_handler(lambda code, msg: self.__on_query_error("getPopularApplications", code, msg))
            query.execute()
        else:
            if not self.__got_popular_apps and not self.__get_popular_apps_pending:
                _logger.debug("will get popular apps from http")
                self.__get_popular_apps_pending = True
                downloader = AppsHttpDownloader('/xml/popularapplications',
                                                self.__on_got_global_popular_apps_from_http)
                downloader.go()

        # getAllApplications is just too slow over XMPP, so we always do it over HTTP
        #
        # We possibly should remove self.__got_all_apps and just re-download whenever we
        # become ready, since there might have been changes to the apps database while
        # we were disconnected.
        #
        # We probably should also redownload periodically even if we stay connected.
        #
        # These changes would require some careful examination of the caching setup to
        # avoid overloading the server.
        #
        if not self.__got_all_apps and not self.__get_all_apps_pending:
            ## do the getAllApplications last since it emits the all-apps-loaded signal when complete

            #query = self.__model.query(("http://online.gnome.org/p/application", "getAllApplications"),
            #"+;description;category;categoryDisplayName;packageNames",
            #single_result=False)

            #query.add_handler(self.__on_got_global_all_apps)
            #query.add_error_handler(lambda code, msg: self.__on_query_error("getAllApplications", code, msg))
            #query.execute()

            self.__get_all_apps_pending = True

            downloader = AppsHttpDownloader('/xml/allapplications',
                                            self.__on_got_global_all_apps_from_http)
            downloader.go()

    def __on_query_error(self, where, error_code, message):
        _logger.warn("Query '" + where + "' failed, code " + str(error_code) + " message: " + str(message))

    def __on_got_self(self, myself):
        _logger.debug("Got myself from data model")
        self.__myself = myself
        myself.connect(self.__on_my_top_apps_changed, "topApplications")
        myself.connect(self.__on_my_pinned_apps_changed, "pinnedApplications")
        myself.connect(self.__on_usage_enabled_changed, "applicationUsageEnabled");
        self.__on_my_top_apps_changed(myself)
        self.__on_my_pinned_apps_changed(myself)
        self.__on_usage_enabled_changed(myself)

    def __on_my_top_apps_changed(self, myself):
        _logger.debug("My top apps from data model: " + str(myself.topApplications))
        self.__my_top_apps = map(self.get_app_for_resource, myself.topApplications)        
        self.emit("my-top-apps-changed", self.__my_top_apps)

    def __on_my_pinned_apps_changed(self, myself):
        _logger.debug("My pinned apps from data model: " + str(myself.pinnedApplications))
        pinned_ids = {}
        self.__my_pinned_apps = map(self.get_app_for_resource, myself.pinnedApplications)
        for app in self.__my_pinned_apps:
            pinned_ids[app.get_id()] = app

        for id,app in self.__ddm_apps.iteritems():
            if id in pinned_ids:
                app.set_pinned(True)
            else:
                app.set_pinned(False)
        
        self.emit("my-pinned-apps-changed", self.__my_pinned_apps)

    def __on_usage_enabled_changed(self, myself):
        _logger.debug("application usage enabled: " + str(myself.applicationUsageEnabled))
        self.emit("enabled-changed")

    def __on_got_global_popular_apps(self, app_resources):
        _logger.debug("Got %d global popular apps" % len(app_resources))
        self.__global_top_apps = map(self.get_app_for_resource, app_resources)

        if len(app_resources) > 0:
            self.__got_popular_apps = True

        self.__get_popular_apps_pending = False
        self.emit("global-top-apps-changed", self.__global_top_apps)

    def __on_got_global_popular_apps_from_http(self, app_attrs):
        _logger.debug("Got %d global popular apps from http" % len(app_attrs))
        self.__global_top_apps = map(self.get_app_for_attrs, app_attrs)

        if len(app_attrs) > 0:
            self.__got_popular_apps = True

        self.__get_popular_apps_pending = False
        self.emit("global-top-apps-changed", self.__global_top_apps)

    def __on_got_global_all_apps(self, app_resources):
        _logger.debug("Got %d apps for all apps" % len(app_resources))

        ## be sure they are all in self.__ddm_apps
        for resource in app_resources:
            self.get_app_for_resource(resource)

        if len(app_resources) > 0:
            self.__got_all_apps = True

        self.__get_all_apps_pending = False
        self.emit("all-apps-loaded")

    def __on_got_global_all_apps_from_http(self, app_attrs):
        _logger.debug("Got %d apps for all apps from http" % len(app_attrs))

        for attrs in app_attrs:
            self.get_app_for_attrs(attrs)

        if len(app_attrs) > 0:
            self.__got_all_apps = True

        self.__get_all_apps_pending = False
        self.emit("all-apps-loaded")
            
    def __on_local_apps_changed(self, ad):
        for menu in ad.get_apps():
            self.get_app_for_menu_entry(menu)
        
        for id,app in self.__ddm_apps.iteritems():
            app.recheck_installed()
            
        self.emit("local-apps-changed", self.__local_apps.itervalues())

    def launch(self, app):
        app._do_launch()
        self.emit('app-launched', app)

    def get_app_for_resource(self, app_resource):
        if not self.__ddm_apps.has_key(app_resource.id):
            ad = apps_directory.get_app_directory()
            for desktop_name in app_resource.desktopNames.split(';'):
                try:
                    target_menuitem = ad.lookup(desktop_name)
                except KeyError, e:
                    continue
                if self.__local_apps.has_key(target_menuitem.get_name()):
                    #_logger.debug("moving app %s from local to apps", target_menuitem.get_name())
                    existing_app = self.__local_apps[target_menuitem.get_name()]
                    del self.__local_apps[target_menuitem.get_name()]
                    existing_app.set_resource(app_resource)
                    self.__ddm_apps[app_resource.id] = existing_app
                    return existing_app
            #_logger.debug("creating app %s", mugshot_app.get_id())
            app = Application(resource=app_resource)            
            self.__ddm_apps[app_resource.id] = app
            self.__all_apps.add(app)
        return self.__ddm_apps[app_resource.id]

    def get_app_for_attrs(self, app_attrs):
        ## a little hack - create a fake Resource
        class FakeResource:
            def __init__(self, attrs):
                self.__attrs = attrs

            def __getattr__(self, name):
                try:
                    return self.__attrs[name]
                except KeyError:
                    raise AttributeError(name)

        return self.get_app_for_resource(FakeResource(app_attrs))

    def get_app_for_menu_entry(self, menu):
        if self.__local_apps.has_key(menu.get_name()):
            return self.__local_apps[menu.get_name()]
        app = None
        for appvalue in self.__ddm_apps.itervalues():
            if not appvalue.get_menu():
                continue
            if appvalue.get_menu().get_name() == menu.get_name():
                app = appvalue
                break
        if app is None:
            app = Application(menu_entry=menu)
            #_logger.debug("creating local app %s", menu.get_name())
            self.__local_apps[menu.get_name()] = app
            self.__all_apps.add(app)
        return app

    def get_all_apps(self):
        return self.__all_apps

    def get_data_model_apps(self):
        return self.__ddm_apps.itervalues()

    def get_local_apps(self):
        return self.__local_apps.values()    

    def get_pinned_apps(self):
        return self.__my_pinned_apps

    def get_global_top_apps(self):
        return self.__global_top_apps

    def get_my_top_apps(self):
        return self.__my_top_apps

    def get_app_usage_enabled(self):
        usage = False
        if self.__myself:
            usage = self.__myself.applicationUsageEnabled
        return usage

    def set_app_id_pinned(self, id, pinned):
        _logger.debug("Requesting that id %s be set pinned=%d" % (id, pinned))
        query = self.__model.update(("http://online.gnome.org/p/application", "setPinned"),
                                    appId=id, pinned=pinned)
        query.add_error_handler(lambda code, msg: self.__on_query_error("setPinned", code, msg))        
        query.execute()

    def set_app_pinned(self, app, pinned):
        self.set_app_id_pinned(app.get_id(), pinned)

    def pin_stuff_if_we_have_none(self):
        ## if no apps are pinned and application usage is enabled, initially pin
        ## some stuff
        if self.__myself and (len(self.__my_pinned_apps) == 0): 
            if self.__myself.applicationUsageStart > 0:
                _logger.debug("no static set")           
                app_stalking_duration = (time.mktime(time.gmtime())) - (int(self.__myself.applicationUsageStart)/1000) 
                _logger.debug("comparing stalking duration %s to statification time %s", app_stalking_duration, self.STATIFICATION_TIME_SEC)                  
                if app_stalking_duration > self.STATIFICATION_TIME_SEC and len(self.__myself.topApplications) > 0:
                    # We don't have a static set yet, time to make it
                    pinned_ids = []
                    for app_resource in self.__myself.topApplications:
                        if len(pinned_ids) >= self.STATIC_SET_SIZE:
                            break
                        pinned_ids.append(app_resource.id)

                    _logger.debug("creating initial pin set: %s", pinned_ids)
                    for id in pinned_ids:
                        self.set_app_id_pinned(id, True)
        
                        ## change notification should come in as the new pinned apps are
                        ## created, and then we'll re-sync in response

    def __on_search_results(self, results_handler, category, search_terms, app_resources):
        _logger.debug("Got search results for search_terms='%s'", search_terms);
        ## on the first results_handler to be called, we'll need to remove the pending
        ## query
        if search_terms:
            if search_terms in self.__search_queries:
                del self.__search_queries[search_terms]
        else:
            if category in self.__category_queries:
                del self.__category_queries[category]

        ## but on every results_handler we need to invoke it.
        ## (it would be nice to do the below map() only once, but too hard)
        applications = map(self.get_app_for_resource, app_resources)
        results_handler(applications, category, search_terms)

    def __on_search_error(self, code, msg, results_handler, category, search_terms):
        _logger.warn("Got search error %d %s" % (code, msg))
        ## as if we got empty results
        self.__on_search_results(results_handler, category, search_terms, [])

    ## FIXME this is a little weird; if you have search_terms, then the category is ignored.
    def search(self, category, search_terms, results_handler):
        _logger.debug("search for category %s search_terms %s" % (category, search_terms))

        ## this is not really right - there's more work to disable the search box if not .global_resource.online, 
        ## etc.
        if not self.__model.ready:
            _logger.debug("search not working since not ready, FIXME")
            return
 
        ## we want to avoid doing the same search twice in parallel, so if we already have
        ## a pending query for a given category or terms, we use it.

        query = None
        need_execute = True
        if search_terms:
            if search_terms in self.__search_queries:
                query = self.__search_queries[search_terms]
                need_execute = False
            else:
                query = self.__model.query(("http://online.gnome.org/p/application", "searchApplications"),
                                           "+;description;category;categoryDisplayName;packageNames",
                                           single_result=False,
                                           search=search_terms)
        else:
            # note that category may be None here, in which case we just do a search
            # for the top popular apps            
            if category in self.__category_queries:                
                query = self.__category_queries[category]
                need_execute = False
            else:
                query = self.__model.query(("http://online.gnome.org/p/application", "getPopularApplications"),
                                           "+;description;category;categoryDisplayName;packageNames",
                                           single_result=False,
                                           category=category)
        
        query.add_handler(lambda app_resources:
                          self.__on_search_results(results_handler, category, search_terms, app_resources))
        query.add_error_handler(lambda code, msg:
                                self.__on_search_error(code, msg, results_handler, category, search_terms))

        if need_execute:
            query.execute()
            
    def search_local_fast_sync(self, search_terms):
        ws_re = re.compile('\s+')

        def get_searchable_values(app):
            ## these should be in order of "relevance" i.e. we give a higher result score
            ## for matching earlier searchable values
            return map(string.lower, (app.get_name(), app.get_generic_name(), app.get_exec_format_string()))
        def app_matches(app):
            match_rank = 0            
            for term in ws_re.split(search_terms):
                term = term.lower()
                searchable_values = get_searchable_values(appvalue)
                for i, attr in enumerate(searchable_values):
                    if not attr:
                        continue
                    for word in ws_re.split(attr):
                        if word.startswith(term):
                            match_rank = match_rank + (len(searchable_values) - i)
            return match_rank

        results = []
        for appvalue in self.__all_apps:
            if not appvalue.is_installed():
                continue
            rank = app_matches(appvalue)
            if rank > 0:
                results.append( (rank, appvalue.get_usage_count(), appvalue) )

        ## sort descending by rank then usage count
        results.sort(lambda a, b: a[0] == b[0] and \
                     cmp(b[1], a[1]) or \
                     cmp(b[0], a[0]))

        _logger.debug("search results %s" % (str(results)))

        return map(lambda r: r[2], results)

__apps_repo = None
def get_apps_repo():
    global __apps_repo
    if __apps_repo is None:
        __apps_repo = AppsRepo()
    return __apps_repo    

class AppSearchResult(search.SearchResult):
    def __init__(self, provider, app):
        super(AppSearchResult, self).__init__(provider)
        self.__app = app

    def get_title(self):
        return self.__app.get_name()

    def get_detail(self):
        return self.__app.get_description()

    def get_icon(self):
        """Returns an icon for the result"""
        return None
    
    def get_icon_url(self):
        return self.__app.get_icon_url()

    def _on_highlighted(self):
        """Action when user has highlighted the result"""
        pass

    def _on_activated(self):
        """Action when user has activated the result"""
        self.__app.launch()

class AppSearchProvider(search.SearchProvider):    
    def __init__(self, repo):
        super(AppSearchProvider, self).__init__()
        self.__repo = repo

    def get_heading(self):
        return "Applications"

    def __on_search_results(self, applications, category, search_terms, consumer):        
        results = []
        for a in applications:
            results.append(AppSearchResult(self, a))

        if len(results) > 0:
            consumer.add_results(results)
        
    def perform_search(self, query, consumer):
        results = map(lambda a: AppSearchResult(self, a), self.__repo.search_local_fast_sync(query))
        if results:
            consumer.add_results(results)
            
search.register_provider_constructor('apps', lambda: AppSearchProvider(get_apps_repo()))
