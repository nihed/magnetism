import gobject

class Task:
    def __init__(self, interval_msecs):
        self.__interval_msecs = interval_msecs
        self.__id = 0
        self.__pending = False

    # this gets overridden
    def do_periodic_task(self):
        pass

    # this returns True to stay installed
    def __do_periodic_task_if_not_pending(self):
         # pending flag is to prevent "pile up" if we spend longer than the interval on the task
        if self.__pending: 
            return True
        self.__pending = True
        self.do_periodic_task()
        self.__pending = False

        return True
        
    def start(self):
        if self.__id != 0:
            return
        self.__id = gobject.timeout_add(self.__interval_msecs, self.__do_periodic_task_if_not_pending)

    def stop(self):
        if self.__id == 0:
            return
        gobject.source_remove(self.__id)
        self.__id = 0


