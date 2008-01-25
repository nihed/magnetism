from turbogears.database import PackageHub
from sqlobject import *

hub = PackageHub('firehose')
__connection__ = hub

# class YourDataClass(SQLObject):
#     pass
 
