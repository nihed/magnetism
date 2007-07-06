import hippo

from overview_layout import OverviewLayout

class OverviewTable(hippo.CanvasBox):
    """Box with a OverviewLayout layout manager and convenience for adding sectional headers"""
    
    def __init__(self, column_spacing=20, row_spacing=5, min_column_width=0, max_column_width=-1, **kwargs):
        super(OverviewTable, self).__init__(**kwargs)
        self.__layout = OverviewLayout(column_spacing=column_spacing, row_spacing=row_spacing,
                                       min_column_width=min_column_width, max_column_width=max_column_width)
        self.set_layout(self.__layout)
        
    def add_section_head(self, section_key, text, left_control=None, right_control=None):
        box = hippo.CanvasBox(orientation=hippo.ORIENTATION_HORIZONTAL, color=0xAAAAAAFF, border_bottom=1, border_color=0xAAAAAAFF)

        if left_control:
            box.append(left_control)
            left_control.set_property("padding-right", 8)
            
        box.append(hippo.CanvasText(text=text, font="Bold 14px", xalign=hippo.ALIGNMENT_START))

        if right_control:
            box.append(right_control, flags=hippo.PACK_END)
            right_control.set_property("padding-left", 8)

        box._overview_order = 2 * section_key + 0
        self.__layout.add(box, is_header=True)

        return box

    def add_column_item(self, section_key, item, compare_func=None):
        item._overview_order = 2 * section_key + 1

        def compare_items(a,b):
            c = cmp(a._overview_order, b._overview_order)
            if c != 0:
                return c

            if compare_func != None:
                return compare_func(a,b)
            else:
                return 1
        
        self.__layout.add(item, compare_func=compare_items)
