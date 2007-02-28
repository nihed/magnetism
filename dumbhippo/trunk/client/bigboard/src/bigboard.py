class Stock(object):
    SIZE_BEAR = 1
    SIZE_BULL = 2
    
    def __init__(self, name):
        self._stock_name = name
        self._bull_widgets = {}

    def append_bull(self, box, item):
        """Adds item to box, recording that this widget should
        only be displayed in "bull" size."""
        self._bull_widgets[item] = box
        box.append(item)
        
    def set_size(self, size):
        for item, box in self._bull_widgets.items():
            box.set_child_visible(item, size == Stock.SIZE_BULL)
    
    def get_content(self, size):
        raise NotImplementedError()
