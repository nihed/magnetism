import os, logging

import libbig

_logger = logging.getLogger("bigboard.Search")

class SearchResult(object):
    """A search result provided by a Stock to be displayed in a drop-down list of results."""
    
    def __init__(self, provider):
        super(SearchResult, self).__init__()
        self.__provider = provider

    def get_provider(self):
        return self.__provider

    def get_title_markup(self):
        """Returns a main title for the result; should bold the search terms using Pango markup."""
        return "FIXME"

    def get_detail_markup(self):
        """Returns detail text for the result; should bold the search terms using Pango markup."""        
        return "FIXME"

    def get_icon(self):
        """Returns an icon for the result"""
        return None

    def _on_highlighted(self):
        """Action when user has highlighted the result"""
        pass

    def _on_activated(self):
        """Action when user has activated the result"""
        raise Error("No activation action implemented for search result")
        
class SearchProvider(object):
    """An object that can provide SearchResult given a search string"""
    
    def __init__(self):
        super(SearchProvider, self).__init__()
        self.__id = None

    def set_id(self, id):
        self.__id = id

    def get_id(self):
        return self.__id

    def get_heading(self):
        """The human-readable heading to be displayed above search results from this provider"""
        raise Error("get_heading() method not implemented on SearchProvider %s" % id)

    def perform_search(self, query, consumer):
        """Performs a search and calls add_results() on the consumer as they arrive"""
        raise Error("perform_search() not implemented on SearchProvider %s" % id)
    
class SearchConsumer(object):
    """An object that hears back about search results"""

    def __init__(self):
        super(SearchConsumer, self).__init__()
    
    def clear_results(self):
        pass

    def add_results(self, results):
        """Called 0 to N times as results come in"""
        pass
    
__constructors = {}
__providers = {}
__enabled_counts = {}

def register_provider_constructor(id, constructor):
    """Register a function that returns a new search provider with the given ID.
    This allows the search system to lazily create that search provider as needed.
    This is optional, if your search provider is global; if it's per-stock you can
    also provide a constructor to enable_search_provider()."""
    if id in __constructors:
        raise Error("Already registered search provider constructor %s " % id)
    __constructors[id] = constructor        


def enable_search_provider(id, constructor = None):
    """Called when a provider should be enabled; may be called multiple times.
    Normally used to enable the provider for a stock when that stock is added to the sidebar.
    Constructor is only used if there's no global constructor registered and we are the
    first to enable."""
    if id in __constructors:
        constructor = __constructors[id]
    if not constructor:
        raise Error("No search provider constructor registered or provided for %s" % id)
    
    if id in __enabled_counts:
        __enabled_counts[id] = __enabled_counts[id] + 1
    else:
        __enabled_counts[id] = 1
        __providers[id] = constructor()
        __providers[id].set_id(id)

def disable_search_provider(id):
    if id in __enabled_counts:
        __enabled_counts[id] = __enabled_counts[id] - 1
        if __enabled_counts[id] == 0:
            if id not in __providers:
                raise Error("Disabled search provider %s was not present" % id)
            del __providers[id]
    else:
        raise Error("Search provider %s disabled but not enabled" % id)

def perform_search(query, consumer):
    """Clear results on consumer, ask all enabled providers to run the given search, and add the results to the consumer"""
    consumer.clear_results()
    for provider in __providers.values():
        provider.perform_search(query, consumer)
