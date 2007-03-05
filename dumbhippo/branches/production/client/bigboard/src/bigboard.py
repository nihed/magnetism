class Stock(object):
    SIZE_BEAR = 1
    SIZE_BULL = 2

    SIZE_BULL_CONTENT_PX = 200
    SIZE_BEAR_CONTENT_PX = 36
    
    def __init__(self, id, ticker):
        self._id = id
        self._ticker = ticker
        self._bull_widgets = {}
        self._size = Stock.SIZE_BULL
        
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

    def get_size(self):
        return self._size

    # get the preferred width of the content at the current size
    def get_content_width(self):
        if self.get_size() == Stock.SIZE_BEAR:
            return Stock.SIZE_BEAR_CONTENT_PX
        else:
            return Stock.SIZE_BULL_CONTENT_PX
    
    def get_content(self, size):
        raise NotImplementedError()
