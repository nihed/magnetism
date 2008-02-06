#!/usr/bin/python

import os,sys,re,heapq,time,httplib,logging,threading
import traceback,urlparse
import BaseHTTPServer,urllib

if sys.version_info[0] < 2 or sys.version_info[1] < 5:
    from pysqlite2 import dbapi2 as sqlite3
else:    
    import sqlite3
import simplejson
import boto
from boto.sqs.connection import SQSConnection
from boto.s3.connection import S3Connection
from boto.s3.key import Key

import turbogears
from turbogears import config

from firehose.jobs.tasks import TaskEntry
from firehose.jobs.poller import TaskPoller
from firehose.jobs.logutil import log_except

_logger = logging.getLogger('firehose.Master')
_logger.debug("hello master!")

aws_config_path = os.path.expanduser('~/.aws')
execfile(aws_config_path)

DEFAULT_POLL_TIME_SECS = 45 * 60 # 45 minutes
MIN_POLL_TIME_SECS = 15
TASKSET_TIMEOUT_SECS = 7 * 60 # 7 minutes
TASKSET_POLL_CHECK_SECS = 2 * 60 # 2 minutes
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
    __slots__ = ['eligibility', 'task',]
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
            _logger.debug("Constructing master poller instance")            
            _instance = MasterPoller()
        return _instance

    def __init__(self):
        global _instance
        assert _instance is None
        self.__tasks_queue = [] # priority queue
        self.__tasks_map = {} # maps id -> task
        self.__changed_buffer = []
        self.__changed_thread_queued = False
        self.__poll_task = None        
        self.__task_lock = threading.Lock()
        
        self.__client_url = config.get('firehose.clienturl')
        
        # Default to one slave on localhost
        self.__worker_endpoints = ['localhost:%d' % (int(config.get('firehose.localslaveport')),)]
        _logger.debug("worker endpoints are %r", self.__worker_endpoints)
        for bind in self.__worker_endpoints:
            (host,port) = bind.split(':')
            if host == 'localhost':
                _logger.debug("found localhost worker, starting it")
                self.__local_handler = TaskPoller.get()
                self.__local_handler.run_async()
        
        path = self.__path = config.get('firehose.taskdbpath')
        _logger.debug("connecting to %r", path)
        conn = sqlite3.connect(path, isolation_level=None)
        cursor = conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS Tasks (key TEXT UNIQUE,
                                                            prev_hash TEXT,
                                                            prev_time DATETIME)''')
        cursor.execute('''CREATE INDEX IF NOT EXISTS TasksIdx on Tasks (key)''')
        
        curtime = time.time()
        for key,prev_hash,prev_time in cursor.execute('''SELECT key,prev_hash,prev_time from Tasks'''):
            task = QueuedTask(curtime, TaskEntry(key, prev_hash, prev_time))
            heapq.heappush(self.__tasks_queue, task)
            self.__tasks_map[key] = task
        conn.close()
        _logger.debug("%d queued tasks", len(self.__tasks_queue))
    
    def __add_task_keys_unlocked(self, keys):
        for key in keys:
            if key in self.__tasks_map:
                continue 
            task = TaskEntry(key, None, None)
            qtask = QueuedTask(time.time(), task)
            self.__tasks_queue.append(qtask)
            self.__tasks_map[key] = task
            
    def __add_task_keys(self, keys):
        try:
            self.__task_lock.acquire()
            self.__add_task_keys_unlocked(keys)
        finally:
            self.__task_lock.release()
    
    def __add_task_for_key(self, key):
        try:
            self.__task_lock.acquire()
            self.__add_task_keys_unlocked([key])
        finally:
            self.__task_lock.release()
            
    def add_feed(self, feedurl):
        taskkey = 'feed/' + urllib.quote(feedurl)
        self.add_task(taskkey)
    
    def add_task(self, taskkey):
        self.add_tasks([taskkey])
        
    def set_tasks(self, taskkeys):
        # For now we don't support resetting the list; just append
        self.add_tasks(taskkeys)
    
    def add_tasks(self, taskkeys):
        _logger.debug("adding %d task keys", len(taskkeys))
        # Append them to the in-memory state
        self.__add_task_keys(taskkeys)
        # Persist them
        try:
            conn = sqlite3.connect(self.__path, isolation_level=None)        
            cursor = conn.cursor()
            for taskkey in taskkeys:
                cursor.execute('''INSERT OR IGNORE INTO Tasks VALUES (?, ?, ?)''',
                       (taskkey, None, None))
        finally:
            conn.close()
        
    def __set_task_status(self, cursor, taskkey, hashcode, timestamp):
        _logger.debug("updating task %r values (%r %r)", taskkey, hashcode, timestamp)
        cursor.execute('''INSERT OR REPLACE INTO Tasks VALUES (?, ?, ?)''',
                       (taskkey, hashcode, timestamp))
    
    @log_except(_logger)
    def __push_changed(self):
        extkey = config.get('firehose.externalServiceKey')
        try:
            self.__task_lock.acquire()
            self.__changed_thread_queued = False
            changed = self.__changed_buffer
            self.__changed_buffer = []
        finally:
            self.__task_lock.release()
        jsonstr = simplejson.dumps(changed)
        parsed = urlparse.urlparse(self.__client_url)
        conn = httplib.HTTPConnection(parsed.hostname, parsed.port)
        path = parsed.path or '/'
        path += '?esk=' + extkey
        conn.request('POST', path, jsonstr)
        conn.close()        

    def __append_changed(self, changed):
        try:
            self.__task_lock.acquire()
            self.__changed_buffer.extend(changed)
            if not self.__changed_thread_queued:
                thr = threading.Thread(target=self.__push_changed)
                thr.setDaemon(True)
                thr.start()
                self.__changed_thread_queued = True
        finally:
            self.__task_lock.release()

    def taskset_status(self, results):
        _logger.info("got %d results", len(results))
        changed = []
        try:
            self.__task_lock.acquire()
            for (taskkey, hashcode, timestamp) in results:
                try:
                    curtask = self.__tasks_map[taskkey]
                except KeyError, e:
                    _logger.exception("failed to find task key %r", taskkey)
                    continue
                if curtask.task.prev_hash != hashcode:
                    _logger.debug("task %r: new hash for %r differs from prev %r", 
                                  taskkey, hashcode, curtask.task.prev_hash)
                    changed.append(taskkey)
        finally:
            self.__task_lock.release()
        self.__append_changed(changed)            
        try:
            conn = sqlite3.connect(self.__path, isolation_level=None)
            cursor = conn.cursor()
            cursor.execute('''BEGIN''')   
            for (taskkey,hashcode,timestamp) in results:
                self.__set_task_status(cursor, taskkey, hashcode, timestamp)
            cursor.execute('''COMMIT''')
        finally:
            conn.close()
            
    def start(self):
        self.__requeue_poll(immediate=True)

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
    
    @log_except(_logger)
    def __enqueue_taskset(self, worker, taskset):
        jsonstr = simplejson.dumps(taskset)
        conn = httplib.HTTPConnection(worker)
        conn.request('POST', '/', jsonstr)
        conn.close()
            
    def __do_poll(self):
        _logger.debug("in poll")
        
        tasksets = []
        curtime = time.time()
        taskset_limit = curtime + TASKSET_TIMEOUT_SECS
        try:
            self.__task_lock.acquire()
            taskset = []
            i = 0 
            while True:          
                try:
                    task = heapq.heappop(self.__tasks_queue)
                except IndexError, e:
                    break
                if i >= MAX_TASKSET_SIZE:
                    if len(tasksets) >= MAX_TASKSET_WORKERS:
                        break
                    tasksets.append(taskset)
                    taskset = []
                    i = 0                    
                else:
                    i += 1
                eligible = task.eligibility < taskset_limit
                task.eligibility = curtime + DEFAULT_POLL_TIME_SECS
                heapq.heappush(self.__tasks_queue, task)                 
                if eligible:
                    taskset.append((str(task.task), task.task.prev_hash, task.task.prev_timestamp))
                else:
                    break
            if len(taskset) > 0:
                tasksets.append(taskset)
            taskset = None
        finally:
            self.__task_lock.release()
        taskset_count = len(tasksets)
        curworker_count = len(self.__worker_endpoints)
        if taskset_count > curworker_count:
            _logger.info("Need worker activation, current=%d, required=%d", curworker_count, taskset_count)
            self.__activate_workers()
        _logger.info("have %d active tasksets", taskset_count)
        if taskset_count > 0:
            for worker,taskset in zip(self.__worker_endpoints,tasksets):
                self.__enqueue_taskset(worker, taskset)
            self.__requeue_poll()

    def requeue(self):
        self.__requeue_poll()

    def __requeue_poll(self, immediate=False):
        _logger.debug("doing poll requeue")
        self.__unset_poll()
        try:
            self.__task_lock.acquire()
                    
            assert self.__poll_task is None
            if len(self.__tasks_queue) == 0:
                _logger.debug("no tasks")
                return
            curtime = time.time()
            next_timeout = self.__tasks_queue[0].eligibility - curtime
            if immediate:
                next_timeout = 1
            elif (next_timeout < MIN_POLL_TIME_SECS):
                next_timeout = MIN_POLL_TIME_SECS
            _logger.debug("requeuing check for %r secs (%r mins)", next_timeout, next_timeout/60.0)
            self.__poll_task = turbogears.scheduler.add_interval_task(action=self.__do_poll, taskname='FirehoseMasterPoll', 
                                                                      initialdelay=next_timeout, interval=TASKSET_POLL_CHECK_SECS)
        finally:
            self.__task_lock.release()
