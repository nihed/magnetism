import gobject

class Task:
    def __init__(self, interval_msecs):
        self.__interval_msecs = interval_msecs
        self.__id = 0

    def do_periodic_task(self):
        pass

    def start(self):
        if self.__id != 0:
            return
        self.__id = gobject.timeout_add(self.__interval_msecs, lambda: self.do_periodic_task() or True)

    def stop(self):
        if self.__id == 0:
            return
        gobject.source_remove(self.__id)
        self.__id = 0


