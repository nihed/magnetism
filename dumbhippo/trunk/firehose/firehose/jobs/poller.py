#!/usr/bin/python

import os,sys,re,heapq,time,Queue,sha,threading
import BaseHTTPServer,httplib,urlparse,urllib
from email.Utils import formatdate,parsedate_tz,mktime_tz
import logging

import boto

from boto.sqs.connection import SQSConnection
from boto.s3.connection import S3Connection
from boto.s3.key import Key
from boto.sqs.connection import SQSConnection

import simplejson
from turbogears import config

from firehose.jobs.logutil import log_except

_logger = logging.getLogger('firehose.Poller')

aws_config_path = os.path.expanduser('~/.aws')
execfile(aws_config_path)

# Global hash mapping family to class
_task_families = {}

class TaskHandler(object):
    FAMILY = None
    
    def run(self, id, prev_hash, prev_timestamp):
        """Receives a task id, result SHA1, and result timestamp integer.
Should compute a new result (newhash, newtimestamp)"""
        raise NotImplementedError() 

class FeedTaskHandler(object):
    FAMILY = 'FEED'
    
    def run(self, id, prev_hash, prev_timestamp):
        targeturl = id
        parsedurl = urlparse.urlparse(targeturl)
        try:
            _logger.info('Connecting to %r', targeturl)
            connection = httplib.HTTPConnection(parsedurl.hostname, parsedurl.port)
            headers = {}
            if prev_timestamp is not None:
                headers['If-Modified-Since'] = formatdate(prev_timestamp)            
            connection.request('GET', parsedurl.path, headers=headers)
            response = connection.getresponse()
            if response.status == 304:
                _logger.info("Got 304 Unmodified for %r", targeturl)
                return (prev_hash, prev_timestamp)
            hash = sha.new()            
            buf = response.read(8192)
            while buf:
                hash.update(buf)
                buf = response.read(8192)
            hash_hex = hash.hexdigest()
            timestamp_str = response.getheader('Last-Modified', None)
            if timestamp_str is not None:
                timestamp = mktime_tz(parsedate_tz(timestamp_str))
            else:
                _logger.debug("no last-modified for %r", targeturl)
                timestamp = time.time()
            if prev_hash != hash_hex:
                _logger.info("Got new hash:%r (prev:%r) ts:%r for url %r", hash_hex, prev_hash, timestamp, targeturl)                
                return (hash_hex, timestamp)
            _logger.info("Fetched full unmodified content for %r", targeturl)             
            return (prev_hash, prev_timestamp)
        finally:
            try:
                connection.close()
            except:
                pass
_task_families[FeedTaskHandler.FAMILY] = FeedTaskHandler

class TaskRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        _logger.debug("handling POST")
        def parsequery(s):
            args = s.split('&')
            return dict(map(lambda x: map(urllib.unquote, x.split('=',1)), args))
        masterhost = parsequery(urlparse.urlparse(self.path).query)['masterhost']
        taskids = simplejson.load(self.rfile)
        poller = TaskPoller.get()
        poller.poll_tasks(taskids, masterhost)
        
_instance = None
class TaskPoller(object):   
    @staticmethod
    def get():
        global _instance
        if _instance is None:
            _logger.debug("constructing task poller")
            _instance = TaskPoller()
        return _instance
        
    def __init__(self):
        bindport = int(config.get('firehose.localslaveport'))
        self.__server = BaseHTTPServer.HTTPServer(('', bindport), TaskRequestHandler)
        self.__active_collectors = set()
        
    def run_async(self):
        thr = threading.Thread(target=self.run)
        thr.setDaemon(True)
        thr.start()
        
    @log_except(_logger)        
    def run(self):
        self.__server.serve_forever()
        
    def __send_results(self, results, masterhost):
        dumped_results = simplejson.dumps(results)
        _logger.debug("opening connection to %r" % (masterhost,))
        connection = httplib.HTTPConnection(masterhost)
        connection.request('POST', '/taskset_status', dumped_results,
                           headers={'Content-Type': 'text/javascript'})
        
    @log_except(_logger)        
    def __run_collect_tasks(self, resultcount, resultqueue, masterhost):
        results = []
        _logger.debug("expecting %r results", resultcount)
        while len(results) < resultcount:
            result = resultqueue.get()
            if result is not None:
                results.append(result)     
        _logger.debug("sending %d results", len(results))            
        self.__send_results(results, masterhost)
        
    @log_except(_logger)        
    def __run_task(self, taskid, prev_hash, prev_timestamp, resultqueue):
        (family, tid) = taskid.split('/', 1)
        try:
            fclass = _task_families[family]
        except KeyError, e:
            _logger.exception("Failed to find family for task %r", taskid)
            return
        inst = fclass()
        try:
            (new_hash, new_timestamp) = inst.run(tid, prev_hash, prev_timestamp)            
        except:
            _logger.exception("Failed task id %r", tid)
            (new_hash, new_timestamp) = (None, None)
        if new_hash is not None:
            resultqueue.put((taskid, new_hash, new_timestamp))
        resultqueue.put(None)
        
    def poll_tasks(self, tasks, masterhost):
        taskcount = 0
        resultqueue = Queue.Queue()
        for (taskid, prev_hash, prev_timestamp) in tasks:
            taskcount += 1
            thread = threading.Thread(target=self.__run_task, args=(taskid, prev_hash, prev_timestamp, resultqueue))
            thread.start()
        collector = threading.Thread(target=self.__run_collect_tasks, args=(taskcount,resultqueue,masterhost))
        collector.start()
