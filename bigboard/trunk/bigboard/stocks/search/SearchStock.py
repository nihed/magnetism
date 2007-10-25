import sys, logging, os, xml.sax.saxutils

import gobject, gtk, gnomeapplet, gconf

import hippo

import bigboard.libbig as libbig
from bigboard.libbig.imagecache import URLImageCache
from bigboard.libbig.http import AsyncHTTPFetcher
from bigboard.stock import Stock
from bigboard.big_widgets import CanvasMugshotURLImage, CanvasVBox
import bigboard.search as search

if __name__ == '__main__':
    def logger(domain, priority, msg):
        print msg
    libbig.logutil.init('DEBUG', ['bigboard.search.SearchStock'], '')
        
_logger = logging.getLogger("bigboard.stocks.SearchStock")

## this class is so each search has its own "context" and
## we ignore async search results from a search that is no longer
## the current search. Owner stores the current ResultsConsumer
## and delegate is another search.SearchConsumer
class ResultsConsumer(search.SearchConsumer):
    def __init__(self, owner, delegate):
        super(ResultsConsumer, self).__init__()
        self.__owner = owner
        self.__delegate = delegate
        
    def clear_results(self):
        if self.__owner.get_current_consumer() == self:
            self.__delegate.clear_results()
        else:
            _logger.debug("ignoring old async clear_results")

    def add_results(self, results):
        if self.__owner.get_current_consumer() == self:
            self.__delegate.add_results(results)
        else:
            _logger.debug("ignoring old async add_results")

    def set_query(self, query):
        if self.__owner.get_current_consumer() == self:
            self.__delegate.set_query(query)
        else:
            _logger.debug("ignoring old async set_query")

class ResultsView(search.SearchConsumer):

    RESULT_TYPE_MAX = 3
    
    def __init__(self, *args, **kwargs):
        super(ResultsView, self).__init__(*args, **kwargs)

        self.__query = ''
        
        ## model columns are
        ## 0 = SearchResult
        ## 1 = icon
        ## 2 = title + detail markup
        ## 3 = heading
        ## 4 = sort key, 0 for a heading, 1 for a non-heading
        
        self.__store = gtk.TreeStore(object, object, str, str, int, str)
        
        self.__view = gtk.TreeView(self.__store)
        self.__view.set_headers_visible(False)
        
        self.__icon_size = 24
        self.__view.insert_column_with_data_func(-1, '',
                                                 gtk.CellRendererPixbuf(),
                                                 self.__render_icon)
        column = gtk.TreeViewColumn('Result')
        
        renderer = gtk.CellRendererText()
        column.pack_start(renderer)
        #column.set_attributes(renderer, markup=2)
        column.set_cell_data_func(renderer, self.__cell_data_method)
        
        self.__view.append_column(column)

        self.__view.get_selection().connect('changed', self.__on_selection_changed)
        self.__view.connect('row-activated', self.__on_row_activated)

        self.__added_headings = set()
        
    def get_widget(self):
        return self.__view

    def __cell_data_method(self, column, renderer, model, iter):
        markup = model.get_value(iter, 2)
        result = model.get_value(iter, 0)
        renderer.set_property('markup', markup)
        if result:
            renderer.set_property('background', None)
        else:
            # this is a heading
            renderer.set_property('background', 'light blue')

    def __on_selection_changed(self, selection):
        (model, rows) = selection.get_selected_rows()
        if len(rows) > 0:
            iter = model.get_iter(rows[0]) # rows is a list of path
            result = model.get_value(iter, 0)
            ## result is None for a heading row            
            if result:
                result._on_highlighted()
            else:
                # don't select heading rows
                selection.unselect_iter(iter)
                next = model.iter_next(iter)
                if next:
                    result = model.get_value(next, 0) ## see if it's another heading
                    if result:
                        selection.select_iter(next)
                        ## keep the cursor with it
                        self.__view.set_cursor(model.get_path(next))
            
    def __on_row_activated(self, tree, path, view_column):
        model = tree.get_model()
        iter = model.get_iter(path)
        result = model.get_value(iter, 0)
        if result:
            result._on_activated()
            
    def __update_showing(self):
        toplevel = self.__view.get_toplevel()
        if not toplevel:
            return
        if self.__store.get_iter_first():
            toplevel.show()
        else:
            toplevel.hide()

    def __send_synthetic_focus_in(self):
        toplevel = self.__view.get_toplevel()
        if not toplevel or not toplevel.window:
            return
        
        focus_in = gtk.gdk.Event(gtk.gdk.FOCUS_CHANGE)
        focus_in.window = toplevel.window
        focus_in.in_ = True
        toplevel.event(focus_in)

    def navigate_up(self):
        #_logger.debug("moving up")
        self.__view.grab_focus()        
        self.__send_synthetic_focus_in()        
        self.__view.emit('move-cursor', gtk.MOVEMENT_DISPLAY_LINES, -1)

    def navigate_down(self):
        #_logger.debug("moving down")
        had_selection = self.__view.get_selection().count_selected_rows() > 0
        self.__view.grab_focus()        
        self.__send_synthetic_focus_in()
        ## don't move down if we just did focus in and focus the first one
        if had_selection:
            self.__view.emit('move-cursor', gtk.MOVEMENT_DISPLAY_LINES, 1)

    def navigate_activate(self):
        #_logger.debug("activating selection if any")        
        had_selection = self.__view.get_selection().count_selected_rows() > 0
        if not had_selection:
            return

        self.__view.grab_focus()        
        self.__send_synthetic_focus_in()
        self.__view.emit('select-cursor-row', True)

    def set_query(self, query):
        self.__query = query
        
    def clear_results(self):
        self.__store.clear()
        self.__added_headings = set()
        self.__update_showing()

    def __bold_query(self, text):        
        ## this assumes the indexes for the lower and the uppercase
        ## strings line up, which I'm not sure is really true i18n-wise,
        ## but I'm not sure of a better way to do this
        i = text.lower().find(self.__query.lower())
        if i < 0:
            return xml.sax.saxutils.escape(text)
        j = i + len(self.__query)
        markup = '%s<b>%s</b>%s' % (xml.sax.saxutils.escape(text[:i]),
                                    xml.sax.saxutils.escape(text[i:j]),
                                    xml.sax.saxutils.escape(text[j:]))
        return markup

    def add_results(self, results):
        for i,r in enumerate(results):
            if i >= self.RESULT_TYPE_MAX:
                break
            title_markup = self.__bold_query(r.get_title())
            detail_markup = self.__bold_query(r.get_detail())
            markup = "<span size='large'>%s</span>\n<span color='#aaaaaa'>%s</span>" % \
                   (title_markup, detail_markup)

            heading = r.get_provider().get_heading()
            if heading not in self.__added_headings:
                self.__store.append(None,
                                    [ None, None,
                                      xml.sax.saxutils.escape(heading),
                                      heading, 0, None ])
                self.__added_headings.add(heading)

            icon = r.get_icon()
            icon_is_url = False
            if icon and icon.startswith(os.sep):
                icon = gtk.gdk.pixbuf_new_from_file_at_size(icon, self.__icon_size, self.__icon_size)
            icon_url = r.get_icon_url()

            self.__store.append(None, [ r, icon,
                                        markup,
                                        heading, 1, icon_url ] )
            if icon_url:
                image_cache = URLImageCache()
                image_cache.get(icon_url, self.__handle_image_load, self.__handle_image_error, format='pixbuf')            

        ## sort headings before search entries
        self.__store.set_sort_column_id(4, gtk.SORT_ASCENDING)

        ## sort headings in alpha order
        self.__store.set_sort_column_id(3, gtk.SORT_ASCENDING)            

        self.__update_showing()
                    
    def __findobj(self, obj, idx=0):
        iter = self.__store.get_iter_first()
        while iter:
            val = self.__store.get_value(iter, idx)
            if val == obj:
                return iter
            iter = self.__store.iter_next(iter)
            
    def __render_icon(self, col, cell, model, iter):
        pixbuf = model.get(iter, 1)[0]  
        if isinstance(pixbuf, gtk.gdk.Pixbuf):
            cell.set_property('pixbuf', pixbuf)
        elif isinstance(pixbuf, basestring):
            cell.set_property('icon-name', pixbuf)
            cell.set_property('stock-size', gtk.ICON_SIZE_SMALL_TOOLBAR)
        else:
            cell.set_property('pixbuf', None)

    def __handle_image_load(self, url, pixbuf):
        _logger.debug("got load for %s: %s", url, pixbuf)
        iter = self.__findobj(url, 5)
        if not iter:
            _logger.debug("no result visible for %s", url)
            return
        pixbuf = pixbuf.scale_simple(self.__icon_size, self.__icon_size, gtk.gdk.INTERP_BILINEAR)        
        self.__store.set(iter, 1, pixbuf)
        path = self.__store.get_path(iter)        
        self.__store.row_changed(path, iter)
        
    def __handle_image_error(self, url, exc):
        # note exception is automatically added to log
        logging.error("failed to load image for '%s': %s", url, exc)  #FIXME queue retry                

class SearchEntry(gtk.Entry):
    def __init__(self, *args, **kwargs):
        super(SearchEntry,self).__init__(*args, **kwargs)

        self.__current_consumer = None

        self.__results_window = gtk.Window(gtk.WINDOW_POPUP)
        self.__results_window.set_resizable(False)
        self.__results_window.set_focus_on_map(False)
        
        vbox = gtk.VBox()
        self.__results_window.add(vbox)
        
        self.__results_view = ResultsView()
        treeview = self.__results_view.get_widget()
        frame = gtk.Frame()
        frame.add(treeview)
        vbox.add(frame)
        vbox.show_all()

        self.__idle_search_id = 0
        self.connect('changed', self.__on_changed)
        self.connect('key-press-event', self.__on_key_press)
        self.connect('focus-out-event', self.__on_focus_out)

    def get_current_consumer(self):
        return self.__current_consumer

    def __on_changed(self, entry):
        if self.__idle_search_id == 0:
            self.__idle_search_id = gobject.timeout_add(200, self.__idle_do_search)
    
    def __force_search_update(self):
        if self.__idle_search_id > 0:
            gobject.source_remove(self.__idle_search_id)
            self.__idle_do_search()
    
    def __idle_do_search(self):
        self.__idle_search_id = 0
        ## Move the window first, THEN perform search,
        ## since the search can synchronously return
        ## results which shows the popup window.

        ## results view shows its own toplevel if it is not empty;
        ## kind of weird, but simple
        
        padding_left = 4
        unpadding_top = 20

        entry_toplevel = self.get_toplevel()
        if self.window and entry_toplevel and \
               isinstance(entry_toplevel, gtk.Window) and \
               entry_toplevel.window:
            rect = entry_toplevel.window.get_frame_extents()
            entry_origin = self.window.get_origin()
            self.__results_window.move(rect.x + rect.width + padding_left,
                                       entry_origin[1] - unpadding_top)

        query = self.get_text()
        _logger.debug("Searching for '%s'" % query)
        self.__current_consumer = ResultsConsumer(self, self.__results_view)
        search.perform_search(query, self.__current_consumer)

    def __on_focus_out(self, entry, event):
        _logger.debug("focus out of search entry")
        self.__results_window.hide()
        return False

    def __on_key_press(self, entry, event):
        ## we want to be able to hit Escape then tab out, so the result-navigation
        ## keys only work if the results popup is showing
        popup_showing = (gtk.VISIBLE & self.__results_window.flags()) != 0

        #_logger.debug("got key press " + str(event.keyval))

        if popup_showing:
            if event.keyval == gtk.keysyms.Up:
                self.__force_search_update()
                self.__results_view.navigate_up()
                return True
            elif event.keyval == gtk.keysyms.Down:
                self.__force_search_update()                
                self.__results_view.navigate_down()
                return True
            elif event.keyval == gtk.keysyms.Tab:
                self.__force_search_update()                
                self.__results_view.navigate_down()
                return True
            elif event.keyval == gtk.keysyms.KP_Enter or event.keyval == gtk.keysyms.Return or \
                 event.keyval == gtk.keysyms.ISO_Enter:
                self.__force_search_update()                
                self.__results_view.navigate_activate()
                self.__results_window.hide()
                return True
            elif event.keyval == gtk.keysyms.Escape:
                self.__results_window.hide()
                return True
            else:
                return False
        else:
            return False
    
class SearchStock(Stock):
    """Search.  It's what's for dinner."""
    __gsignals__ = {
        "match-selected" : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, [])
    }    
    def __init__(self, *args, **kwargs):
        super(SearchStock,self).__init__(*args, **kwargs)
        
        self.__box = hippo.CanvasBox()
        
        self.__entry = SearchEntry()
        self.__widget = hippo.CanvasWidget(widget=self.__entry)
        self.__box.append(self.__widget)
        self.__empty_box = CanvasVBox()

    def get_content(self, size):
        return size == self.SIZE_BULL and self.__box or self.__empty_box

    def focus(self):
        _logger.debug("doing focus")
        self.__entry.focus()
            
if __name__ == '__main__':

    class TestSearchResult(search.SearchResult):
        def __init__(self, provider, text):
            super(TestSearchResult, self).__init__(provider)
            self.__text = text

        def get_title(self):
            return self.__text

        def get_detail(self):
            return "Extra details about this, " + self.__text

        def get_icon(self):
            """Returns an icon for the result"""
            return 'gtk-open'

        def _on_highlighted(self):
            """Action when user has highlighted the result"""
            _logger.debug("highlighted result " + self.get_title())            

        def _on_activated(self):
            """Action when user has activated the result"""
            _logger.debug("activated result " + self.get_title())

    class TestSearchProvider(search.SearchProvider):    
        def __init__(self, heading):
            super(TestSearchProvider, self).__init__()
            self.__heading = heading

        def get_heading(self):
            return self.__heading

        def perform_search(self, query, consumer):
            stuff_to_search_in = [
                'Hello world',
                'abcdefg',
                'This is some text',
                'The quick brown fox jumped over the lazy dog',
                'Hello planet!',
                'abcxyz',
                'testing 123',
                'search',
                'search2',
                'searched' ]

            results = []
            for s in stuff_to_search_in:
                if query in s:
                    results.append(TestSearchResult(self, s))
            consumer.add_results(results)

    def construct_provider():
        return TestSearchProvider()

    search.register_provider_constructor('test1', lambda: TestSearchProvider("A_TestItems"))
    search.enable_search_provider('test1')

    search.register_provider_constructor('test2', lambda: TestSearchProvider("B_TestItems"))
    search.enable_search_provider('test2')

    search.register_provider_constructor('test3', lambda: TestSearchProvider("C_TestItems"))
    search.enable_search_provider('test3')

    search.register_provider_constructor('test4', lambda: TestSearchProvider("D_TestItems"))
    search.enable_search_provider('test4')

    window = gtk.Window()
    window.set_border_width(50) ## to test positioning of results window 
    entry = SearchEntry()
    entry.show()
    window.add(entry)
    window.show()

    loop = gobject.MainLoop()
    loop.run()

