import os
import xml.dom.minidom
import urllib, logging

import repository, deploy
from source.source import DeployedSource

def my_import(name):
    mod = __import__(name)
    components = name.split('.')
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod

class Blat:
    def __init__(self, srcdir=None, repodir=None,repoproxy=None, rootdir=None):
        self._sources = {}
        self._deployed = {}
        self.logger = logging.getLogger()
        self.rootdir = rootdir
        if not (srcdir is None):
            self._read_srcdir(srcdir)
        self.repository = repository.Repository(cachedir=repodir,proxyurl=repoproxy)
    
    def _add_source_from_xml(self, xml):
        id = xml.getAttribute('id')
        self.logger.debug('Adding source id %s' % (id,))
        srctype = xml.getAttribute('type')
        srcmod = my_import('blat.source.' + srctype)
        source = srcmod.factory(xml)
        self._sources[id] = source

    def _read_srcdir(self, dir):
        for fn in os.listdir(dir):
            if fn[-4:] != '.xml':
                continue
            dom = xml.dom.minidom.parse(open(os.path.join(dir,fn)))
            node = dom.documentElement
            if node.nodeName == 'source':
                 self._add_source_from_xml(node)

    def _parse_smudge(self, smudgefd):
        smudge = xml.dom.minidom.parse(smudgefd)
        for node in smudge.documentElement.childNodes:
            if node.nodeType == 1 and node.nodeName == 'deploy':
                deployment= deploy.BlatDeployment(node, root=self.rootdir)
                self.logger.debug('Read deployment id %s' % (deployment.id,))
                if not self._sources.has_key(deployment.id):
                    raise Exception('Deployment specified for unknown source id %s' % (deployment.id,))
                self._deployed[deployment.id] = DeployedSource(self._sources[deployment.id], deployment)
                
    def install(self, smudge):
        self.prime_cache(smudge)
        for id in self._deployed:
            deployed = self._deployed[id]
            resource = self.repository.get(id, deployed.get_version(), deployed.get_url())
            self.logger.info('Installing deployment id %s' % (id,))            
            deployed.install(resource)
        
    def prime_cache(self, smudge):
        self._parse_smudge(smudge)
        for id in self._deployed:
            deployed = self._deployed[id]
            self.logger.debug('Initializing deployment id %s' % (id,))            
            resource = self.repository.get(id, deployed.get_version(), deployed.get_url())
    
    def check_updates(self):
        return []
    
    def cache(self, id, version, filename):
        self.repository.install(id, version,filename)
        

