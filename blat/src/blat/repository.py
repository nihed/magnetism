import os, errno
import urllib,shutil,tempfile
import sha
import xml.dom.minidom
import logging

class Repository:
    def __init__(self, cachedir=None, proxyurl=None):
        self.logger = logging.getLogger()
        if cachedir is None:
            self.cachedir = os.path.expanduser('~/.blat')
        else:
            self.cachedir = cachedir
        if not os.access(self.cachedir, os.X_OK):
            self.logger.info('Creating directory %s' % (self.cachedir,))
            os.makedirs(self.cachedir)
        self.proxyurl = proxyurl
    
    def _pathfragment(self, id, version):
        return urllib.quote(id, '') + '/' + urllib.quote(version, '')
    
    def install(self, id, version, artifact, rename=False, artifact_name=None):
        dir = os.path.join(self.cachedir, self._pathfragment(id, version))
        try:
            os.makedirs(dir)
        except OSError, e:
            if e.errno != errno.EEXIST:
                raise e
        if artifact_name is None:
            baseartifact = os.path.basename(artifact)
        else:
            baseartifact = artifact_name
        meta = open(os.path.join(dir, 'meta.xml'), 'w')
        f = open(artifact)
        if not rename:
            o = open(os.path.join(dir, baseartifact), 'w')
        b = 'x'
        sum = sha.new()
        while b != '':
            b = f.read(8192)
            sum.update(b)
            if not rename:
                o.write(b)
        f.close()
        if not rename:
            o.close()
        else:
            os.rename(artifact, os.path.join(dir, baseartifact))
        metadoc = xml.dom.minidom.getDOMImplementation().createDocument(None, "blat-metadata", None)
        elt = metadoc.createElement('sum')
        elt.setAttribute('type', 'sha1')
        elt.appendChild(metadoc.createTextNode(sum.hexdigest()))
        metadoc.documentElement.appendChild(elt)
        elt = metadoc.createElement('path')
        elt.appendChild(metadoc.createTextNode(baseartifact))
        metadoc.documentElement.appendChild(elt)
        metadoc.writexml(meta)
        meta.write('\n')
        meta.close()
    
    def _parse_metafile(self, f):
        sha = None
        filename = None
        doc = xml.dom.minidom.parse(f)
        for node in doc.documentElement.childNodes:
            if node.nodeType == 1:
                if node.nodeName == 'sum':
                    sha = node.childNodes[0].nodeValue
                elif node.nodeName == 'path':
                    filename = node.childNodes[0].nodeValue
        return (sha, filename)
    
    def _sha_digest_file(self, f):
        sum = sha.new()
        b = 'x'
        while b != '':
            b = f.read(8192)
            sum.update(b)
        return sum.hexdigest()
    
    def _geturl(self, url):
        src = urllib.urlopen(url)
        (tempfd, tempfilename) = tempfile.mkstemp(dir=self.cachedir,prefix='blat-download')
        tempf = os.fdopen(tempfd, 'w')
        shutil.copyfileobj(src, tempf)
        src.close()
        tempf.close()
        return tempfilename
    
    def get(self, id, version, url):
        pathfragment = self._pathfragment(id, version)
        cachepath = os.path.join(self.cachedir, pathfragment)
        url_base = os.path.basename(url)
        url_expected_path = os.path.join(cachepath, url_base)
        if os.access(cachepath, os.R_OK):
            self.logger.debug('Found artifact %s %s in cache' % (id, version))
            (sha, filename) = self._parse_metafile(open(os.path.join(cachepath, 'meta.xml')))
            if url_expected_path != os.path.join(cachepath, filename):
                logger.info('URL for artifact %s changed')
            return url_expected_path
        retrieved = False
        temppath = None

        if self.proxyurl:
            metaurl = self.proxyurl + '/' + urllib.quote(pathfragment) + '/meta.xml'  
            self.logger.info('Looking in proxy for artifact %s,%s; URL: %s' % (id,version,metaurl))
            try:                
                metaf = self._geturl(metaurl)
            except IOError, e:
                self.logger.info('Not found in proxy, retrieving canonical artifact %s,%s; URL: %s' % (id,version,url))
                temppath = self._geturl(url)
            else:
                self.logger.debug('Parsing metafile from proxy')
                (known_sum, meta_filename) = self._parse_metafile(open(metaf))
                os.unlink(metaf)
                # for safety
                meta_filename = os.path.basename(meta_filename)
                artifact_url = self.proxyurl + '/' + urllib.quote(pathfragment) + '/' + meta_filename
                self.logger.info('Downloading from proxy: %s,%s; URL: %s' % (id,version,url))    
                temppath = self._geturl(artifact_url)
                digest = self._sha_digest_file(open(temppath))
                if digest != known_sum:
                    raise Exception("Invalid sha1 %s for proxy-retrieved artifact, expected %s; URL: %s" % (digest, known_sum, artifact_url))
        else:
            self.logger.info('Not found and no proxy specified, retrieving canonical artifact %s,%s; URL: %s' % (id,version,url))
            temppath = self._geturl(url)
        self.logger.info('Succesfully retrieved artifact %s,%s from %s' % (id,version,url)) 
        self.install(id, version, temppath, rename=True, artifact_name=url_base)
        return url_expected_path
            
    
repository = None
def set_repository(repo):
    repository = repo

def get_repository():
    return repository