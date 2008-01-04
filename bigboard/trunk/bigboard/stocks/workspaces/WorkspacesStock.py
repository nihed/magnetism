import logging
import wnck
import hippo
import gtk, gobject

from bigboard.stock import Stock

_logger = logging.getLogger('bigboard.stocks.WorkspacesStock')

# This is not 100% correct because a mix of workspaces and viewports
# will most probably still cause problems.
# vuntz will add native support for viewspaces to libwnck soon
class Viewspace:
    """An object representing a workspace or a viewport."""
    def __init__(self, screen, workspace, num, vp_x=-1, vp_y=-1):
        _logger.debug("Constructing Viewspace offset %s, %s", vp_x, vp_y)
        self.__screen = screen
        self.__workspace = workspace
        self.__vp_x = vp_x
        self.__vp_y = vp_y
        self.__num = num
    
    def get_number(self):
        """ Get the viewspace number"""
        return self.__num
    
    def activate(self):
        """Activate the viewspace"""
        if self.is_virtual():
            self.__screen.move_viewport(self.__vp_x, self.__vp_y)            
        else:
            self.__workspace.activate(gtk.get_current_event_time())
     
    def get_screen(self):
        return self.__screen
    
    def get_workspace(self):
        return self.__workspace
        
    def get_viewport_x(self):
        return self.__vp_x
   
    def get_viewport_y(self):
        return self.__vp_y
        
    def is_virtual(self):
        return (self.__vp_x != -1 and self.__vp_y != -1)
      
    def is_active(self):
        if self.is_virtual():
            if self.__screen.get_active_workspace() == self.__workspace:
                return (self.__vp_x >= self.__workspace.get_viewport_x() and self.__vp_x < (self.__workspace.get_viewport_x() + self.__screen.get_width()) and self.__vp_y >= self.__workspace.get_viewport_y() and self.__vp_y < (self.__workspace.get_viewport_y() + self.__screen.get_height()))
            else:
                return False            
        else:
            return (self.__screen.get_active_workspace() == self.__workspace)
            
class ViewspaceBox(hippo.CanvasBox):
    """A box displaying a viewspace and its windows"""
    def __init__(self, viewspace, width, height):
        super(ViewspaceBox, self).__init__(box_width=width, box_height=height)
        self.__viewspace = viewspace
        self.__width = width
        self.__height = height
        
    def do_paint_below_children(self, cr, damaged):
        windows = self.__viewspace.get_screen().get_windows_stacked()
        screen = self.__viewspace.get_screen()
        
        # Draw background
        cr.rectangle(0, 0, self.__width, self.__height)
        if self.__viewspace.is_active():
            cr.set_source_rgb(1, 0.5, 0.5)
        else:
            cr.set_source_rgb(1, 0, 0)
        cr.fill()
        
        # Move origin because of the border
        cr.translate(1, 1)
        cr.set_line_width(1)
        
        # Inner size: Box size minus 1px border on each side
        inner_width  = self.__width  - 2
        inner_height = self.__height - 2
        
        gdk_context = gtk.gdk.CairoContext(cr)
        
        for window in windows:
            if window.get_workspace() != self.__viewspace.get_workspace() or window.is_minimized():
                continue
                
            (x, y, width, height) = window.get_geometry()

            # Make it relative to the current viewport
            x -= self.__viewspace.get_viewport_x()
            y -= self.__viewspace.get_viewport_y()
            
            # When the viewspace is moved, the positions of the windows change as well,
            # so we have to add that again
            x += self.__viewspace.get_workspace().get_viewport_x()
            y += self.__viewspace.get_workspace().get_viewport_y()
            
            # Scale positions
            scaled_x = (x * inner_width) / screen.get_width()
            scaled_y = (y * inner_height) / screen.get_height()
            scaled_width = (width * inner_width) / screen.get_width()
            scaled_height = (height * inner_height) / screen.get_height()
            
            (x, y, width, height) = (scaled_x, scaled_y, scaled_width, scaled_height)
            
            if x < 0:
                if (x + width) < 0:
                    continue
                else:
                    # Crop left border
                    width = width + x
                    x = 0
                    
            if x > inner_width:
                continue
                
            if (x + width) > inner_width:
                # Crop right border
                width = inner_width - x
            
            if y < 0:
                if (y + height) < 0:
                    continue
                else:
                    # Crop top border
                    height = height + y
                    y = 0
                    
            if y > inner_height:
                continue
                
            if (y + height) > inner_height:
                # Crop bottom border
                height = inner_height - y
            
            # Draw the window
            cr.rectangle(x, y, width, height)
            if window.is_active():
                cr.set_source_rgb(1, 1, 1)
            else:
                cr.set_source_rgb(0.9, 0.9, 0.9)
            cr.fill()
            cr.rectangle(x - 0.5, y - 0.5, width + 1, height + 1)
            cr.set_source_rgb(0.2, 0.2, 0.2)
            cr.stroke()
            
            # Draw window icon
            if not window.get_icon_is_fallback():
                pixbuf = window.get_mini_icon()                
                if (pixbuf.get_width() + 2) < scaled_width and (pixbuf.get_height() + 2) < scaled_height:
                   cr.rectangle(1, 1, inner_width, inner_height)
                   gdk_context.set_source_pixbuf(pixbuf, scaled_x + (scaled_width - pixbuf.get_width()) / 2, scaled_y + (scaled_height - pixbuf.get_height()) / 2)
                   cr.fill()
        
        # Draw 2px border
        cr.translate(0, 0)
        cr.set_line_width(2)
        cr.rectangle(0, 0, self.__width, self.__height)
        cr.set_source_rgb(0, 0, 0)
        cr.stroke()
        
        # Draw the workspace number
        cr.set_font_size(20)
        cr.set_source_rgb(0, 0, 0)
        (text_x_bearing, text_y_bearing, text_width, text_height, _, _) = cr.text_extents(str(self.__viewspace.get_number()))
        cr.move_to(self.__width / 2 - text_width / 2 - text_x_bearing, self.__height / 2 - text_height / 2 - text_y_bearing)
        cr.show_text(str(self.__viewspace.get_number()))

# Needed for the do_paint_below_children() override to work
gobject.type_register(ViewspaceBox)

class WorkspacePager(hippo.CanvasBox):
    """A pager to switch between workspaces."""
    def __init__(self):
        super(WorkspacePager, self).__init__(orientation=hippo.ORIENTATION_VERTICAL, spacing=2)
        screen = wnck.screen_get_default()
        screen.connect('workspace-created', self.__on_workspace_created)
        screen.connect('workspace-destroyed', self.__on_workspace_destroyed)
        screen.connect('viewports-changed', self.__on_viewports_changed)
        screen.connect('active-workspace-changed', self.__on_active_workspace_changed)

        screen.connect('window-opened', self.__on_window_opened)
        screen.connect('active-window-changed', self.__on_active_window_changed)
        screen.connect('window-stacking-changed', self.__on_window_stacking_changed)
        screen.connect('showing-desktop-changed', self.__on_showing_desktop_changed)
       
    def redraw(self):
        self.emit_paint_needed(0, 0, -1, -1)

    def prepare_window(self, window):
        window.connect('geometry-changed', self.__on_window_geometry_changed)
    
    def __on_window_geometry_changed(self, window):
        _logger.debug("Callback: __on_window_geometry_changed")
        self.redraw()

    def __on_window_stacking_changed(self, screen):
        _logger.debug("Callback: __on_window_stacking_changed")
        self.redraw()
        
    def __on_window_opened(self, screen, window):
        _logger.debug("Callback: __on_window_opened")
        self.prepare_window(window)
        self.redraw()
        
    def __on_active_window_changed(self, screen, prevwin):
        _logger.debug("Callback: __on_active_window_changed")
        self.redraw()

    def __on_showing_desktop_changed(self, screen):
        self.redraw()

    def __on_workspace_created(self, screen, workspace):
        _logger.debug("Callback: __on_workspace_created, %i", workspace.is_virtual())
        self.__update()
        
    def __on_workspace_destroyed(self, screen, workspace):
        _logger.debug("Callback: __on_workspace_destroyed")
        self.__update()
        
    def __on_viewports_changed(self, screen):
        _logger.debug("Callback: __on_viewports_changed")
        self.__update()
    
    def __on_active_workspace_changed(self, screen, workspace):
        _logger.debug("Callback: __on_active_workspace_changed")
        self.__update()
    
    def __create_row(self):
        return hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, xalign=hippo.ALIGNMENT_CENTER, spacing=2)
    
    def __create_viewspace_list(self):
        screen = wnck.screen_get_default()
        n_workspaces = screen.get_workspace_count()
        _logger.debug("Got %s workspaces", n_workspaces)
        viewspaces = []
        viewspace_number = 1
        for workspace_number in range(n_workspaces):
            _logger.debug("Processing workspace: %s", workspace_number)
            workspace = screen.get_workspace(workspace_number)
            if workspace.is_virtual():
                viewport_cols = workspace.get_width() / screen.get_width()
                viewport_rows = workspace.get_height() / screen.get_height()
                _logger.debug("Workspace %s is a virtual workspace and contains %s cols and %s rows", workspace_number, viewport_cols, viewport_rows)
                for row in range(viewport_rows):
                    for col in range(viewport_cols):
                        viewspaces.append(Viewspace(screen, workspace, viewspace_number, screen.get_width() * col, screen.get_height() * row))
                        viewspace_number += 1
            else:
                _logger.debug("Workspace %s is an ordinary workspace, adding to list", workspace_number);
                viewspaces.append(Viewspace(screen, workspace, viewspace_number))
                viewspace_number += 1
        return viewspaces
        
    def __update(self):
        self.clear()
        viewspaces = self.__create_viewspace_list()
        num_in_row = 0
        current_row = self.__create_row()
        for viewspace in viewspaces:
            num_in_row += 1
            if num_in_row > 2:
                self.append(current_row)
                current_row = self.__create_row()
                num_in_row = 1
            current_row.append(self.__create_box(viewspace))
        if num_in_row != 0:
            self.append(current_row)
    
    def __create_box(self, viewspace):
        screen = wnck.screen_get_default()
        width = 74
        height = (width * screen.get_height()) / screen.get_width()
        box = ViewspaceBox(viewspace, width, height)
        def switch_viewspace(box, event, viewspace):
            viewspace.activate()
            return True
        box.connect('button-press-event', switch_viewspace, viewspace)
        return box
        

class WorkspacesStock(Stock):
    """Shows all workspaces."""
    def __init__(self, *args, **kwargs):
        Stock.__init__(self, *args, **kwargs)

        self._pager = WorkspacePager();
        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        self._box.append(self._pager)

    def get_content(self, size):
        return self._box
