import gobject

class Task:
    def __init__(self, interval_msecs, initial_interval=-1):
        if initial_interval >= 0:
            self.__initial_interval = initial_interval
        else:
            self.__initial_interval = interval_msecs
        self.__interval_msecs = interval_msecs
        self.__id = 0
        self.__pending = False
        self.__first_time = False

    # this gets overridden
    def do_periodic_task(self):
        pass

    # this returns True to stay installed
    def __do_periodic_task_if_not_pending(self):
         # pending flag is to prevent "pile up" if we spend longer than the interval on the task
        if self.__pending: 
            return True

        self.__pending = True

        if self.__first_time:
            self.__first_time = False
            gobject.source_remove(self.__id)
            self.__id = gobject.timeout_add(self.__interval_msecs, self.__do_periodic_task_if_not_pending)
        
        self.do_periodic_task()

        self.__pending = False

        return True
        
    def start(self):
        if self.__id != 0:
            return
        self.__first_time = True
        self.__id = gobject.timeout_add(self.__initial_interval, self.__do_periodic_task_if_not_pending)

    def stop(self):
        if self.__id == 0:
            return
        gobject.source_remove(self.__id)
        self.__id = 0
          
    def is_running(self):
        return self.__id != 0
