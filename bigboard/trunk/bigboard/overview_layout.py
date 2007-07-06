import gobject
import copy
import hippo

class OverviewLayout(gobject.GObject,hippo.CanvasLayout):
    """A Canvas Layout manager that arranges items in a grid with headings in-between

       There are two classes of items; header items get a line by themselves.
       Other items are wrapped into rows with a uniform column width. The column width
       is determined dynamically from the wrapped items and from the min_column_width
       and max_column_width keyword arguments of the constructor.

       You use the layout manager by creating a hippo.CanvasBox, and then setting the
       manager on the canvas box using box.set_layout(). Items then should be added
       to the layout rather than directly to the box.
       
    """

    def __init__(self, column_spacing=0, row_spacing=0, min_column_width=0, max_column_width=-1):
        """
        Create a new OverviewLayout object
        
        Arguments:
        column_spacing: Spacing between columns of items
        row_spacing: Spacing between rows. This is added between all rows, whether
           they are rows of items or header rows. You can add more spacing above
           or below a header item by setting i's padding.
        min_column_width: The minimum column width. Columns of items will
           be at least this wide. (default 0)
        max_column_width: The maximum column width. Columns may be wider if there
           is excess space left over in the layout, or if any item has a minimum
           width greater than this value. If < 0, no maximum. (default=-1)
           
        """

        gobject.GObject.__init__(self)
        self.__box = None
        self.__column_spacing = column_spacing
        self.__row_spacing = row_spacing
        self.__min_column_width = min_column_width
        self.__max_column_width = max_column_width
    
    def add(self, child, is_header=False, flags=0, compare_func=None):
        """
        Add an item to the layout.

        Arguments:
        is_header: if true, this item is a header that should be on a row all
           by itself. Otherwise, it will be layed out in columns with other
           non-header items (default=False).
        flags: flags to pass to hippo.CanvasBox.append(). Currently, all flags
           passed in are ignored by this layout manager. (default=0)
        compare_func: function used to sort the item on insert, if None, items
           are shown in the order they are inserted.
           
        """
        if self.__box == None:
            raise Exception("Layout must be set on a box before adding children")
        if compare_func != None:
            self.__box.insert_sorted(child, flags=flags, compare_func=compare_func)
        else:
            self.__box.append(child, flags=flags)
        box_child = self.__box.find_box_child(child)
        box_child.is_header = is_header
    
    def do_set_box(self, box):
        self.__box = box
    
    def do_get_width_request(self):
        max_item_min_width = 0
        max_item_natural_width = 0
        max_header_min_width = 0
        max_line_natural_width = 0

        line_natural_width = 0
        line_item_count = 0
        
        max_line_items = 0

        for box_child in self.__box.get_layout_children():
            (min_width, natural_width) = box_child.get_width_request()
            
            if box_child.is_header:
                max_line_natural_width = max(max_line_natural_width, natural_width)
                max_header_min_width = max(max_header_min_width, min_width)
                line_natural_width = 0
                line_item_count = 0
            else:
                max_item_min_width = max(max_item_min_width, min_width)
                max_item_natural_width = max(max_item_natural_width, natural_width)
                line_natural_width += natural_width + self.__column_spacing
                max_line_natural_width = max(max_line_natural_width, line_natural_width - self.__column_spacing)
                line_item_count += 1
                max_line_items = max(max_line_items, line_item_count)

        self.__max_line_items = max_line_items
        
        self.__column_width = max_item_natural_width
        
        if self.__max_column_width > 0 and self.__column_width > self.__max_column_width:
            # We'll never make the columns smaller than the maximum item *minimum* width,
            # the max_column_width setting only applies to the default size we derive
            # from item's natural widths
            self.__column_width = max(self.__max_column_width, max_item_min_width)
            
        if self.__column_width < self.__min_column_width:
            self.__column_width = self.__min_column_width

        return (max(max_item_min_width, max_header_min_width), max_line_natural_width)

    def __get_columns(self, width):
        if self.__column_width == 0: # No non-empty column items
            return (1, width)
        
        columns = (width + self.__column_spacing) // (self.__column_width + self.__column_spacing)
        if columns > self.__max_line_items:
            columns = self.__max_line_items
        
        # This really shouldn't happen, but avoid divide-by-zero if it does
        if columns == 0:
            return (1, self.__column_width)
        else:
            return (columns, width)
    
    def do_get_height_request(self, width):
        (columns, width) = self.__get_columns(width)

        total_min_height = 0
        total_natural_height = 0

        line_index = 0
        line_min_height = 0
        line_natural_height = 0
        line_item_count = 0
        item_x = 0

        self.__line_natural_height = []
        self.__line_min_height = []

        for box_child in self.__box.get_layout_children():
            if (box_child.is_header or line_item_count == columns) and line_item_count > 0:
                total_min_height += line_min_height
                total_natural_height += line_natural_height
                self.__line_min_height.append(line_min_height)
                self.__line_natural_height.append(line_natural_height)
                line_min_height = 0
                line_natural_height = 0
                line_item_count = 0
                item_x = 0
                
            if box_child.is_header:
                (min_height, natural_height) = box_child.get_height_request(width)
                total_min_height += min_height
                total_natural_height += natural_height
                self.__line_min_height.append(min_height)
                self.__line_natural_height.append(natural_height)
            else:
                # Computing the item_width this way has the advantage that all the widths
                # are guaranteed to sum to the total width
                new_x = ((line_item_count + 1) * (width + self.__column_spacing)) / columns
                item_width = new_x - item_x - self.__column_spacing
                
                (min_height, natural_height) = box_child.get_height_request(item_width)
                line_min_height = max(line_min_height, min_height)
                line_natural_height = max(line_natural_height, natural_height)

                item_x = new_x
                line_item_count += 1

        if line_item_count > 0:
            total_min_height += line_min_height
            total_natural_height += line_natural_height
            self.__line_min_height.append(line_min_height)
            self.__line_natural_height.append(line_natural_height)

        self.__line_count = len(self.__line_min_height)
        if self.__line_count > 0:
            total_spacing = self.__row_spacing * (self.__line_count - 1)
            total_min_height += total_spacing
            total_natural_height += total_spacing

        self.__min_height = total_min_height
        self.__natural_height = total_natural_height

        return (total_min_height, total_natural_height)
    
    def do_allocate(self, x, y, width, height, requested_width, requested_height, origin_changed):
        (columns, width) = self.__get_columns(width)

        # Shouldn't happen, do something sane if it does
        if height < self.__min_height:
            height = self.__min_height

        to_shrink = self.__natural_height - height
        if to_shrink >= 0:
            line_heights = copy.copy(self.__line_natural_height)
            # We were allocated less than our natural height. We want to shrink lines
            # as equally as possible, but no line more than it's maximum shrink.
            #
            # To do this, we process the lines in order of the available shrink from
            # least available shrink to most
            #
            shrinks = []
            for i in range(0, self.__line_count):
                shrinks.append((i, self.__line_natural_height[i] - self.__line_min_height[i]))
                shrinks.sort(key=lambda t: t[1])

            lines_remaining = self.__line_count
            for (i, shrink) in shrinks:
                # If we can shrink the rest of the lines equally, do that. Otherwise
                # shrink this line as much as possible
                if shrink * lines_remaining >= to_shrink:
                    shrink = to_shrink // lines_remaining

                line_heights[i] -= shrink
                lines_remaining -= 1
                to_shrink -= shrink
        else:
            line_heights = self.__line_natural_height

        line_index = 0
        line_item_count = 0
        item_x = 0
        item_y = 0
        
        for box_child in self.__box.get_layout_children():
            if (box_child.is_header or line_item_count == columns) and line_item_count > 0:
                item_y += line_heights[line_index] + self.__row_spacing
                line_item_count = 0
                line_index += 1
                item_x = 0
            
            if box_child.is_header:
                box_child.allocate(x, y + item_y, width, line_heights[line_index], origin_changed)
                item_y += line_heights[line_index] + self.__row_spacing
                line_index += 1
            else:
                new_x = ((line_item_count + 1) * (width + self.__column_spacing)) / columns
                item_width = new_x - item_x - self.__column_spacing
                
                box_child.allocate(x + item_x, y + item_y, item_width, line_heights[line_index], origin_changed)
                
                item_x = new_x
                line_item_count += 1

gobject.type_register(OverviewLayout)

