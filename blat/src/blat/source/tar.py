from blat.source.source import AbstractSource
import os
import tarfile

class TarSource(AbstractSource):
    def __init__(self, xml):
        AbstractSource.__init__(self, xml)
    
    def build(self):
        pass
    
    def install(self, resource, deployment):
        cwd = os.getcwd()
        try:
            os.chdir(deployment.dir)
            tar = tarfile.open(resource, "r")
            for tarinfo in tar:
                tar.extract(tarinfo)
        finally:
            os.chdir(cwd)

def factory(*args, **kwargs):
    return apply(TarSource, args, kwargs)
    