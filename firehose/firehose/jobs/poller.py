#!/usr/bin/python

import os,sys,re,heapq,time,Queue,sha
import BaseHTTPServer,httplib,urlparse
from email.Utils import formatdate,parsedate

import boto

from boto.sqs.connection import SQSConnection
from boto.s3.connection import S3Connection
from boto.s3.key import Key
from boto.sqs.connection import SQSConnection

import simplejson
from turbogears import config

_logger = logging.getLogger('firehose.Poller')

aws_config_path = os.path.expanduser('~/.aws')
execfile(aws_config_path)

class TaskHandler(object):
    FAMILY = None
    
    def run(self, id, prev_hash, prev_timestamp):
        """Receives a task id, result SHA1, and result timestamp integer.
Should compute a new result (newhash, newtimestamp)"""
        raise NotImplementedError() 

class FeedTaskHandler(object):
    FAMILY = 'http-feed'
    
    def run(self, id, prev_hash, prev_timestamp):
        targeturl = urllib.unquote(id)
        parsedurl = urlparse.urlparse(targeturl)
        try:
            connection = httplib.HTTPConnection(parsedurl.host, parsedurl.port)
            connection.request('GET', parsedurl.path,
                               headers={'If-Modified-Since':
                                        formatdate(prev_timestamp)})
            response = connection.getresponse()
            if response.status == 304:
                return (prev_hash, prev_timestamp) 
            data = response.read()
            new_timestamp = resp_headers
            hash = sha.new()
            hash.update(data)
            hash_hex = hash.hexdigest()
            timestamp_str = response.getheader('Last-Modified')
            if timestamp_str is not None:
                timestamp = parsedate(timestamp_str)
            else:
                _logger.debug("no last-modified for %r", targeturl)
                timestamp = time.time()
            if prev_hash != hash_hex:
                return (hash_hex, timestamp)
            return (prev_hash, prev_timestamp)
        finally:
            try:
                connection.close()
            except:
                pass

class TaskRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        _logger.debug("handling POST")
        data = rfile.read()
        taskids = simplejson.load(data)
        poller = TaskPoller.get()
        poller.poll_tasks(taskids)
        
_instance = None
class TaskPoller(SimpleHTTPServer.BaseHTTPServer):
    
    @staticmethod
    def get():
        global _instance
        if _instance is None:
            _instance = MasterPoller()
        return _instance
        
    def __init__(self):
        bindport = int(config.get('firehose.slaveport'))
        self.__server = BaseHTTPServer.HTTPServer(('', bindport), TaskRequestHandler)
        self.__active_collectors = set()
        
    def run(self):
        self.__server.serve_forever()
        
    def __send_results(self, ):
        
    def __run_collect_tasks(self, taskqueue, resultqueue):
        _logger.debug("doing join on taskqueue")
        taskqueue.join()
        _logger.debug("all tasks complete")
        results = []
        while True:
            try:
                result = resultqueue.get(False)
                results.append(result)
            except Queue.Empty:
                break 
        self.__send_results(results)
        
    def poll_tasks(self, taskids):
        taskqueue = Queue.Queue()
        resultqueue = Queue.Queue()
        for task in taskids:
            taskqueue.put(task)
            thread = threading.Thread(target=self.__run_task, args=(task,resultqueue))
            thread.start()
        collector = threading.Thread(target=self.__run_collect_tasks, args=(taskqueue,resultqueue))
        collector.start()
    
    def run(self):
        pass
