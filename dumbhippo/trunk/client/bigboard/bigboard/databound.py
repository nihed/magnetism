class DataBound:
    def __init__(self, resource):
        self.resource = resource
        self.__connections = []

    def connect_resource(self, callback, property=None):
        self.__connections.append(callback)
        self.resource.connect(callback, property=property)

    def disconnect_resources(self):
        for callback in self.__connections:
            self.resource.disconnect(callback)
        self.__connections = []


class DataBoundItem(DataBound):
    def __init__(self, resource):
        DataBound.__init__(self, resource)
        self.connect("destroy", self.__disconnect)
        
    def __disconnect(self, object):
        self.disconnect_resources()
