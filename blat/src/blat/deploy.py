import os    

import xml.sax
import logging

class BlatDeployment:
    def __init__(self, xml, root=None):
        self.id = xml.getAttribute('id')
        self.version = xml.getAttribute('version')
        self.dir = "/"
        self.url = None
        self.logger = logging.getLogger()
        self.files = {}
        for node in xml.childNodes:
            if node.nodeType == 1:
                if node.nodeName == 'dir':
                    self.dir = node.childNodes[0].nodeValue
                elif node.nodeName == 'url':
                    self.url = node.childNodes[0].nodeValue
                elif node.nodeName == 'file':
                    path = node.getAttribute('path')
                    value = ''
                    for subnode in node.childNodes:
                        subvalue = subnode.nodeValue
                        if not (subvalue is None):
                            value = value + subvalue
                    self.files[path] = value
                else:
                    self.logger.warning('Unknown element name ' + node.nodeName)
        if not (root is None):
            if self.dir[0] == '/':
                self.dir = self.dir[1:]
            self.dir = os.path.join(root, self.dir)
    
    def install(self):
        for path in self.files:
            f = open(os.path.join(self.dir, path), 'w')
            f.write(self.files[path])
            f.close()
