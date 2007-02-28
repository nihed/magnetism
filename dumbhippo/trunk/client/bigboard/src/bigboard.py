class Stock(object):
    SIZE_BEAR = 1
    SIZE_BULL = 2
    
    def __init__(self, id, ticker):
        self._id = id
        self._ticker = ticker
        self._bull_widgets = {}
        
    def get_id(self):
        return self._id
    
    def get_ticker(self):
        return self._ticker

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
