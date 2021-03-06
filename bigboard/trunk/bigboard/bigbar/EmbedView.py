import logging
import gtk
import gobject
import deskbar.interfaces.View
import deskbar.core.Utils
from deskbar.ui.cuemiac.CuemiacEntry import CuemiacEntry
from deskbar.ui.cuemiac.CuemiacModel import CuemiacModel
from deskbar.ui.cuemiac.CuemiacTreeView import CuemiacTreeView
from deskbar.ui.cuemiac.CuemiacActionsTreeView import CuemiacActionsTreeView, CuemiacActionsModel
from deskbar.ui.cuemiac.CuemiacItems import CuemiacCategory

_logger = logging.getLogger("bigboard.Deskbar")

class Border(gtk.Frame):
    def __init__(self, child, label=None, shadow=gtk.SHADOW_ETCHED_IN):
        super(Border, self).__init__(label)
        self.add(child)
        self.set_shadow_type(shadow)

class EmbedView(deskbar.interfaces.View):
    
    def __init__(self, controller, model):
        deskbar.interfaces.View.__init__(self, controller, model)
        self._controller.register_view(self)
        self._model.connect("query-ready", lambda s,m: gobject.idle_add(self.append_matches, s, m))
        
        self.default_entry_pixbuf = deskbar.core.Utils.load_icon("deskbar-applet-panel-h.png", width=23, height=14)
        self.entry = CuemiacEntry (self.default_entry_pixbuf)
        self.entry.connect("changed", self._controller.on_query_entry_changed)
        self.entry.connect("activate", self._controller.on_query_entry_activate)
        self.entry.connect("key-press-event", self._controller.on_query_entry_key_press_event)
        self.entry.connect("focus-out-event", self._controller.on_query_focus_out)
        self.entry.show()
        
        # Results TreeView
        self.treeview_model = CuemiacModel ()
        self.treeview_model.connect("category-added", lambda w, c, p: self.cview.expand_row(p, False) )
        
        self.cview = CuemiacTreeView (self.treeview_model)
        self.cview.connect ("match-selected", self._controller.on_match_selected)
        self.cview.connect ("match-selected", self.__on_match_selected)
        self.cview.connect ("do-default-action", self._controller.on_do_default_action)
        self.cview.connect ("do-default-action", self.__on_match_selected)
        self.cview.connect_after ("cursor-changed", self._controller.on_treeview_cursor_changed)
        self.cview.show()
        
        self.scrolled_results = gtk.ScrolledWindow ()
        self.scrolled_results.set_policy (gtk.POLICY_AUTOMATIC, gtk.POLICY_AUTOMATIC)
        self.scrolled_results.set_shadow_type(gtk.SHADOW_IN)
        self.scrolled_results.add(self.cview)
        self.scrolled_results.show()
        
        # Actions TreeView
        self.actions_model = CuemiacActionsModel()
        self.aview = CuemiacActionsTreeView(self.actions_model)
        self.aview.connect ("action-selected", self._controller.on_action_selected)
        self.aview.connect ("go-back", self.__on_go_back)
        self.aview.show()
        
        self.scrolled_actions = gtk.ScrolledWindow()
        self.scrolled_actions.set_policy (gtk.POLICY_AUTOMATIC, gtk.POLICY_AUTOMATIC)
        self.scrolled_actions.set_shadow_type(gtk.SHADOW_IN)
        self.scrolled_actions.add(self.aview)
        self.scrolled_actions.show()
     
        self._window = gtk.Window(gtk.WINDOW_POPUP)
        self._window.set_size_request(480, 360)
        self._window.set_decorated(False)
        self._window.set_focus_on_map(False)
        self._window.move(210, 10)
        hbox = gtk.VBox(spacing=6)
        self._window.add(Border(hbox))
        hbox.add(self.scrolled_results)
        #hbox.add(self.scrolled_actions)

    def __have_cview_selection(self):
        model, iter = self.cview.get_selection().get_selected()
        if iter == None:
            return False
        
        match = model[iter][model.MATCHES]
        if match.__class__ == CuemiacCategory:
            return False

        return True

    def __send_synthetic_focus_in(self):
        focus_in = gtk.gdk.Event(gtk.gdk.FOCUS_CHANGE)
        focus_in.window = self._window.window
        focus_in.in_ = True
        self._window.event(focus_in)

    def focus_results_from_top(self, event):
        self.cview.grab_focus()
        self.__send_synthetic_focus_in()
        self.cview.event(event)
        
    def focus_results_from_bottom(self, event):
        self.cview.grab_focus()
        self.__send_synthetic_focus_in()
        self.cview.event(event)
        
    def forward_key_to_results_tree(self, event):
        _logger.debug("Sending synthetic event to cview")
        self.cview.event(event)
        
    def clear_results(self):
        self.treeview_model.clear()
        self._window.hide()
        
    def clear_actions(self):
        self.actions_model.clear()
    
    def clear_query(self):
        self.entry.set_text("")
        self.update_entry_icon()
     
    def show_results(self):
        self._window.show_all()
    
    def get_entry(self):
        return self.entry
   
    def receive_focus(self, time):
        self.entry.grab_focus()
       
    def display_actions(self, actions, qstring):
        self.actions_model.clear()
        self.actions_model.add_actions(actions, qstring)
        self.aview.grab_focus()
    
    def append_matches (self, sender, matches):
        """
        We suppose that the results belong to the text
        that is currently in the entry
        """
        self.treeview_model.append (matches, self.entry.get_text())
        self.update_entry_icon()
        
    def update_entry_icon (self, icon=None):
        
        if icon == None:
            icon = self.default_entry_pixbuf
            if not (self.cview.get_toplevel().flags() & gtk.MAPPED):
                # The view is hidden, just show default icon
                self.entry.set_icon (icon)
                return
                
            path, column = self.cview.get_cursor ()
        
            if path != None:
                match = self.treeview_model[self.treeview_model.get_iter(path)][self.treeview_model.MATCHES]
                if not isinstance(match, CuemiacCategory):
                    icon=match.get_icon()
                
        self.entry.set_icon (icon)
         
    def __on_go_back(self, treeview):
        self.cview.grab_focus()
        return False

    def __on_match_selected(self, *args):
        self._window.hide()

    
