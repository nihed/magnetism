#!/usr/bin/python

import os,sys,re,heapq,time,httplib,logging,threading

if sys.version_info[0] < 2 or sys.version_info[1] < 5:
    from pysqlite2 import dbapi2 as sqlite3
else:    
    import sqlite3
import boto

from boto.sqs.connection import SQSConnection
from boto.s3.connection import S3Connection
from boto.s3.key import Key

from turbogears import config
import turbojson.jsonify

from tasks import TaskKey

_logger = logging.getLogger('firehose.Master')

aws_config_path = os.path.expanduser('~/.aws')
execfile(aws_config_path)

DEFAULT_POLL_TIME_SECS = 45 * 60 # 45 minutes
MIN_POLL_TIME_SECS = 15
TASKSET_TIMEOUT_SECS = 7 * 60 # 7 minutes
MAX_TASKSET_SIZE = 30
MAX_TASKSET_WORKERS = 1

class TaskStatusHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        _logger.debug("handling POST")
        data = self.rfile.read()
        taskset_results = simplejson.load(data)
        poller = MasterPoller.get()
        poller.taskset_status(taskset_results)

class QueuedTask(object):
    __slots__ = ['eligibility', 'task']
    def __init__(self, eligibility, task):
        self.eligibility = eligibility
        self.task = task
        
    def __cmp__(self, other):
        return cmp(self.eligibility, other.eligibility)

_instance = None
class MasterPoller(object):
    
    @staticmethod
    def get():
        global _instance
        if _instance is None:
            _instance = MasterPoller()
        return _instance
    
    def __add_task_for_key(self, key):
        try:
            self.__task_lock.acquire()        
            taskkey = TaskKey(key)
            for qtask in self.__tasks:
                if qtask.task == taskkey:
                    return qtask
            newtask = QueuedTask(time.time(), taskkey)
            self.__tasks.append(newtask)
        finally:
            self.__task_lock.release()
    
    def add_task(self, taskkey):
        cursor = self.__conn.cursor()         
        self.__set_task_status(cursor, taskkey, None, None)
        self.__add_task_for_key(taskkey)
        
    def __set_task_status(self, cursor, taskkey, hashcode, timestamp):
        cursor.execute('''INSERT INTO Tasks VALUES (?, ?, ?)''',
                       taskkey, hashcode, timestamp)
        
    def taskset_status(self, results):
        _logger.debug("got %d results", len(results))
        cursor = self.__conn.cursor()        
        for (taskkey,hashcode,timestamp) in results:
            self.__set_task_status(cursor, taskkey, hashcode, timestamp)

    def __init__(self):
        self.__tasks = []
        self.__poll_task = None        
        self.__task_lock = threading.Lock()
        
        # Default to one slave on localhost
        self.__worker_urls = ['localhost:%d' % (int(config.get('firehose.slaveport')),)]
        _logger.debug("worker urls are %r", self.__worker_urls)
        
        path = config.get('firehose.taskdbpath')
        _logger.debug("connecting to %r", path)
        self.__conn = sqlite3.connect(path, isolation_level=None)
        cursor = self.__conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS Tasks (key TEXT UNIQUE ON CONFLICT IGNORE,
                                                            resulthash TEXT,
                                                            resulttime DATETIME)''')
        cursor.execute('''CREATE INDEX IF NOT EXISTS TasksIdx on Tasks (key)''')
        
        curtime = time.time()
        for v in cursor.execute('''SELECT key from Tasks'''):
            task = QueuedTask(curtime, TaskKey(v))
            heapq.heappush(self.__tasks, task)
            
    def start(self):
        self.__requeue_poll()

    def __unset_poll(self):
        try:
            self.__task_lock.acquire()
            if self.__poll_task is None:
                return
            turbogears.scheduler.cancel(self.__poll_task)
            self.__poll_task = None
        finally:
            self.__task_lock.release()
            
    def __activate_workers(self):
        raise NotImplementedError()
    
    def __enqueue_taskset(self, worker, taskset):
        jsonstr = turbogears.jsonify.encode(taskset)
        conn = httplib.HTTPConnection(worker)
        req = conn.request('POST', '/', jsonstr)
            
    def __do_poll(self):
        _logger.debug("in poll")
        self.__unset_poll()
        
        tasksets = []
        curtime = time.time()
        taskset_limit = curtime + TASKSET_TIMEOUT_SECS
        try:
            self.__task_lock.acquire()
            taskset = []
            for i,task in enumerate(self.__tasks):
                if i >= MAX_TASKSET_SIZE:
                    if len(tasksets) >= MAX_TASKSET_WORKERS:
                        break
                    tasksets.append(taskset)
                    taskset = []
                if task.eligibility < taskset_limit:
                    taskset.extend(task)
        finally:
            self.__task_lock.release()
        taskset_count = len(tasksets)
        if taskset_count > len(self.__worker_urls):
            self.__activate_workers()        
        for worker,taskset in zip(tasksets, self.__worker_urls):
            self.__enqueue_taskset(worker, taskset)
        self.__requeue_poll()

    def __requeue_poll(self):
        _logger.debug("doing poll requeue")
        try:
            self.__unset_poll()
            self.__task_lock.acquire()
                    
            assert self.__poll_task is None
            if len(self.__tasks) == 0:
                _logger.debug("no tasks")
                return
            curtime = time.time()
            next_timeout = self.__tasks[0].eligibility - curtime
            if next_timeout < MIN_POLL_TIME_SECS:
                next_timeout = MIN_POLL_TIME_SECS
            _logger.debug("requeuing check for %r", next_timeout)
            self.__poll_task = turbogears.scheduler.add_interval_task(action=self.__do_poll, taskname='FirehoseMasterPoll', initialdelay=next_timeout, interval=1)
        finally:
            self.__task_lock.release()
