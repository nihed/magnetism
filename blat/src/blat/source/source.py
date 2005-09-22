from blat import deploy

class AbstractSource:
    def __init__(self, xml):
        self.id = xml.getAttribute('id')
        self.url = None
        for node in xml.childNodes:
            if node.nodeType == 1:
                if node.nodeName == 'url':
                    self.url = node.childNodes[0].nodeValue
    
    def retrieve(self):
        if self.url is None:
            raise Exception('No URL known for source id %s' % (self.id,))
    
    def build(self):
        pass
        
    def install(self, resource, deployment):
        pass
        
class DeployedSource:
    def __init__(self, src, deployment):
        self.src = src
        self.deployment = deployment
    
    def get_version(self):
        return self.deployment.version
    
    def get_url(self):
        url = self.src.url
        if url is None:
            url =self.deployment.url
        return url.replace('${VERSION}', self.deployment.version)
        
    def install(self, resource):
        self.src.install(resource, self.deployment)
        self.deployment.install()