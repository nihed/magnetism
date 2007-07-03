import hippo

from overview_layout import OverviewLayout

class OverviewTable(hippo.CanvasBox):
    """Box with a OverviewLayout layout manager and convenience for adding sectional headers"""
    def __init__(self, column_spacing=20, row_spacing=5, min_column_width=0, max_column_width=-1):
        super(OverviewTable, self).__init__()
        self.__layout = OverviewLayout(column_spacing=column_spacing, row_spacing=row_spacing,
                                       min_column_width=min_column_width, max_column_width=max_column_width)
        self.set_layout(self.__layout)
        
    def append_section_head(self, text, left_control=None, right_control=None):
        box = CanvasHBox(color=0xAAAAAAFF, border_bottom=1, border_color=0xAAAAAAFF)

        if left_control:
            box.append(left_control)
            left_control.set_property("padding-right", 8)
            
        box.append(hippo.CanvasText(text=text, font="Bold 14px", xalign=hippo.ALIGNMENT_START))

        if right_control:
            box.append(right_control, flags=hippo.PACK_END)
            right_control.set_property("padding-left", 8)

        self.__layout.add(box, is_header=True)

    def append_column_item(self, item):
        self.__layout.add(item)
