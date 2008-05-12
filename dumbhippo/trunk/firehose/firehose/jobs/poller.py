#!/usr/bin/python

import os,sys,re,heapq,time,Queue,sha,threading
import BaseHTTPServer,httplib,urlparse,urllib
import tempfile
from email.Utils import formatdate,parsedate_tz,mktime_tz
from BeautifulSoup import BeautifulSoup,Comment
import logging
from StringIO import StringIO

import boto

from boto.sqs.connection import SQSConnection
from boto.s3.connection import S3Connection
from boto.s3.key import Key
from boto.sqs.connection import SQSConnection

import simplejson
from turbogears import config

from firehose.jobs.logutil import log_except

# This gross hack is the only easy way right now for us to set
# a timeout on socket connections
import socket
socket.setdefaulttimeout(30)

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
    
class FeedPostProcessor(object):
    def process(self, data):
        return data

class XmlElementEater(FeedPostProcessor):
    def __init__(self, deletepaths=[]):
        super(XmlElementEater, self).__init__()
        self.__deletepaths = deletepaths
        
    def process(self, data):
        import lxml.etree
        tree = lxml.etree.parse(StringIO(data))
        root = tree.getroot()
        for path in self.__deletepaths:
            node = root.xpath(path)
            if not node:
                continue
            node = node[0]
            parent = node.getparent()
            parent.remove(node)
        return lxml.etree.tostring(tree, pretty_print=True)
    
class RegexpEater(FeedPostProcessor):
    def __init__(self, regexps):
        super(RegexpEater, self).__init__()
        self.__regexps = map(re.compile, regexps)
        
    def process(self, data):
        value = StringIO(data)
        outvalue = StringIO()
        for line in value:
            for regexp in self.__regexps:
                if regexp.search(line):
                    continue
                outvalue.write(line)
        return outvalue.getvalue()

class HtmlCommentEater(FeedPostProcessor):
    def __init__(self):
        super(HtmlCommentEater, self).__init__()
        
    def process(self, data):
        is_xml = False
        is_html = False
        for i,line in enumerate(StringIO(data)):
            if i > 20:
                break
            # This is low tech, but should be reliable enough; remember
            # it's just an optimization here.
            # We could investiate MIME sniffers though.
            if line.find('<?.*xml.*version') >= 0:
                is_xml = True
                break
            if line.find('<html>') >= 0:
                is_html = True
                break
        if is_html and not is_xml:
            soup = BeautifulSoup(data)
            comments = soup.findAll(text=lambda text:isinstance(text, Comment))
            for comment in comments:
                comment.extract()
            return soup.prettify()
        return data
                
class ChainedProcessors(object):
    def __init__(self, processors):
        super(ChainedProcessors, self).__init__()
        self.__is_identity = len(processors) == 0        
        if self.__is_identity:
            processors = [FeedPostProcessor()]
        self._processors = processors
        
    def is_identity(self):
        return self.__is_identity
        
    def process(self, data):
        for processor in self._processors:
            data = processor.process(data)
        return data

# Define shared eaters for these feed types
rss_eater = XmlElementEater(['/rss/channel/lastBuildDate', '/rss/channel/pubDate'])
atom_eater = XmlElementEater(['/feed/updated'])
# This maps from a regular expression matching a URL to a list of processors
feed_transformations = [
  (r'digg.com/users/.*/history/diggs.rss', [rss_eater]),
  (r'picasaweb.google.com.*feed.*base.*album', [rss_eater, atom_eater]),
  (r'google.com/reader/public', [XmlElementEater(['/feed/updated'])]),
  (r'blogs.gnome.org', [RegexpEater(['<!--.*page served in.*seconds.*-->'])]),
  # We try to consume all HTML
  (r'.*', [HtmlCommentEater()]),
]
feed_transformations = [(re.compile(r'^https?://([A-Z0-9]+\.)*' + x[0]), x[1]) for x in feed_transformations]

def get_transformations(url):
    transformers = []
    for (matcher, decepticons) in feed_transformations:
         if matcher.search(url):
             transformers.extend(decepticons)
    return transformers

class FeedTaskHandler(object):
    FAMILY = 'FEED'
    
    def __init__(self, feedcache_bucket=None):
        self.__feedcache_bucket = feedcache_bucket

    def run(self, id, prev_hash, prev_timestamp):
        targeturl = id
        transformlist = get_transformations(targeturl)
        parsedurl = urlparse.urlparse(targeturl)
        try:
            _logger.info('Connecting to %r', targeturl)
            hostport = parsedurl[1].split(':', 1)
            if len(hostport) == 1:
                (host,port) = (hostport[0], 80)
            else:
                (host,port) = hostport
            connection = httplib.HTTPConnection(host, port)
            headers = {}
            if prev_timestamp is not None:
                headers['If-Modified-Since'] = formatdate(prev_timestamp)
            headers['User-Agent'] = 'Mugshot/Firehose (http://developer.mugshot.org/wiki/FirehosePolling)'            
            connection.request('GET', parsedurl[2], headers=headers)
            response = connection.getresponse()
            if response.status == 304:
                _logger.info("Got 304 Unmodified for %r", targeturl)
                return (prev_hash, prev_timestamp)
            if self.__feedcache_bucket is not None:
                (tempfd, temppath) = tempfile.mkstemp()
                outfile = os.fdopen(tempfd, 'w')
            else:
                (tempfd, temppath) = (None, None)
                outfile = None
            rawhash = sha.new()
            data = StringIO()
            buf = response.read(8192)
            while buf:
                if outfile is not None:
                    outfile.write(buf)
                data.write(buf)
                rawhash.update(buf)
                buf = response.read(8192)
            rawhash_hex = rawhash.hexdigest()
            datavalue = data.getvalue()
            processor = ChainedProcessors(transformlist)            
            processed = processor.process(datavalue)
            hash = sha.new()
            hash.update(processed)
            hash_hex = hash.hexdigest()
            if outfile is not None:
                outfile.close()
                if prev_hash != hash_hex:
                    k = Key(self.__feedcache_bucket)
                    ts = int(time.time())                    
                    k.key = targeturl + ('.%d' % (ts,))
                    _logger.debug("storing to bucket %s key %s", self.__feedcache_bucket.name, k.key)      
                    k.set_contents_from_filename(temppath)
                else:
                    os.unlink(temppath)
            timestamp_str = response.getheader('Last-Modified', None)
            if timestamp_str is not None:
                timestamp = mktime_tz(parsedate_tz(timestamp_str))
            else:
                _logger.debug("no last-modified for %r", targeturl)
                timestamp = time.time()
            filters_applied = (not processor.is_identity()) and "(filters applied)" or ""  
            if prev_hash != hash_hex:
                _logger.info("Got new hash:%r (prev:%r) ts:%r %s for url %r", hash_hex, prev_hash, timestamp, filters_applied, targeturl)                
                return (hash_hex, timestamp)
            _logger.info("Fetched full unmodified content %s for %r", filters_applied, targeturl)             
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
        args = parsequery(urlparse.urlparse(self.path)[4])
        masterhost = args['masterhost']
        serial = args['serial']
        taskids = simplejson.load(self.rfile)
        poller = TaskPoller.get()
        poller.poll_tasks(taskids, masterhost, serial)
        
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
        self.__savefetches = config.get('firehose.savefetches') == "true"
        self.__server = BaseHTTPServer.HTTPServer(('', bindport), TaskRequestHandler)
        self.__active_collectors = set()
        aws_accessid = config.get('firehose.awsAccessKeyId')
        aws_secretkey = config.get('firehose.awsSecretAccessKey')       
        self.__s3_conn = S3Connection(aws_accessid, aws_secretkey)
        
        bname = config.get('firehose.awsS3Bucket')
        self.__feedcache_bucket = self.__s3_conn.get_bucket('feedcache.' + bname)              
        
    def run_async(self):
        thr = threading.Thread(target=self.run)
        thr.setDaemon(True)
        thr.start()
        
    @log_except(_logger)        
    def run(self):
        self.__server.serve_forever()
        
    def __send_results(self, results, masterhost, serial):
        data = {'results': results,
                'serial': serial}
        dumped_results = simplejson.dumps(data)
        _logger.debug("opening connection to %r" % (masterhost,))
        connection = httplib.HTTPConnection(masterhost)
        connection.request('POST', '/taskset_status', dumped_results,
                           headers={'Content-Type': 'text/javascript'})
        
    @log_except(_logger)        
    def __run_collect_tasks(self, resultcount, resultqueue, masterhost, serial):
        results = []
        received_count = 0
        _logger.debug("expecting %r results", resultcount)
        while received_count < resultcount:
            result = resultqueue.get()
            received_count += 1
            if result is not None:
                results.append(result)     
        _logger.debug("sending %d results", len(results)) 
        self.__send_results(results, masterhost, serial)
        
    @log_except(_logger)        
    def __run_task(self, taskid, prev_hash, prev_timestamp, resultqueue):
        (family, tid) = taskid.split('/', 1)
        try:
            fclass = _task_families[family]
        except KeyError, e:
            _logger.exception("Failed to find family for task %r", taskid)
            return
        if self.__savefetches:
            inst_kwargs = {'feedcache_bucket': self.__feedcache_bucket}
        else:
            inst_kwargs = {}
        inst = fclass(**inst_kwargs)
        kwargs = {}     
        try:
            (new_hash, new_timestamp) = inst.run(tid, prev_hash, prev_timestamp, **kwargs)            
        except Exception, e:
            _logger.exception("Failed task id %r: %s", tid, e)
            (new_hash, new_timestamp) = (None, None)
        if new_hash is not None:
            resultqueue.put((taskid, new_hash, new_timestamp))
        else:
            resultqueue.put(None)
        
    def poll_tasks(self, tasks, masterhost, serial):
        taskcount = 0
        resultqueue = Queue.Queue()
        for (taskid, prev_hash, prev_timestamp) in tasks:
            taskcount += 1
            thread = threading.Thread(target=self.__run_task, args=(taskid, prev_hash, prev_timestamp, resultqueue))
            thread.start()
        collector = threading.Thread(target=self.__run_collect_tasks, args=(taskcount,resultqueue,masterhost,serial))
        collector.start()

if __name__ == '__main__':
    testdata = '''<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
    <channel>
        <title>digg / jdhore1 / history / diggs</title>
        <link>http://digg.com/users/jdhore1/history/diggs</link>
        <description>A history of jdhore1's diggs</description>
        <language>en-us</language>
        <pubDate>Wed, 30 Apr 2008 16:42:42 UTC</pubDate>
        <lastBuildDate>Wed, 30 Apr 2008 16:42:42 UTC</lastBuildDate>
        <generator>Digg.com</generator>
        <item>
            <title>Hans Reiser was convicted Monday of first degree murder in t</title>
            <link>http://digg.com/linux_unix/Hans_Reiser_was_convicted_Monday_of_first_degree_murder_in_t</link>
            <description><![CDATA[
                 A jury has found an Oakland software programmer guilty in the death of his estranged wife.        
    ]]></description>
            <pubDate>Mon, 28 Apr 2008 23:41:43 UTC</pubDate>
            <author>jdhore1</author>
            <guid>http://digg.com/linux_unix/Hans_Reiser_was_convicted_Monday_of_first_degree_murder_in_t</guid>
        </item>
        <item>
            <title>The Democrats Have a Nominee: It's Obama!</title>
            <link>http://digg.com/political_opinion/The_Democrats_Have_a_Nominee_It_s_Obama</link>
            <description><![CDATA[
                Other than ensuring the Greatest Show on Earth will continue, does it matter that Hillary Clinton d
efeated Barack Obama Tuesday in Pennsylvania by nine-plus points? Barack Obama is the nominee.

            ]]></description>
            <pubDate>Fri, 25 Apr 2008 21:23:01 UTC</pubDate>
            <author>jdhore1</author>
            <guid>http://digg.com/political_opinion/The_Democrats_Have_a_Nominee_It_s_Obama</guid>
        </item>
    </channel>
</rss>
    '''
    transformers = get_transformations('http://digg.com/users/jdhore/history/diggs.rss')
    processor = ChainedProcessors(transformers)
    print processor.process(testdata)
    processor = ChainedProcessors([])
    print processor.process(testdata)
    transformers = get_transformations('http://myspace.com/blah')
    processor = ChainedProcessors(transformers)
    print processor.process('''
<html>
  <!-- foo bar
  baz -->
  <head>moo</head>
<body>
  testing<!-- one -->two three
  <b>four</b><!--
    blabla-->
</body>
</html>''')
    