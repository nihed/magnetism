from Resource import *

class AbstractModel(object):
    """
    Base class defining the operation of the Mugshot data model.

    AbstractModel defines to core operation of the Mugshot data model.
    Normal usage is to use the singleton instance of the DataModel
    subclass, which connects the DataModel to the user's Mugshot
    session.  Other subclasses are used for testing purposes.
    
    """

    def __init__(self):
        self.__ready_handlers = []
        self.__added_handlers = []
        self.__removed_handlers = []
        self.__resources = {}
        self.ready = False
        self.global_resource = None
        self.self_resource = None

    def add_ready_handler(self, handler):
        """Add a handler that will be called a) when we become ready b) on reconnection"""
        self.__ready_handlers.append(handler)

    def remove_ready_handler(self, handler):
        """Remove a handler added with add_ready_handler"""
        self.__ready_handlers.remove(handler)

    def add_added_handler(self, handler):
        """Add a handler that will be called when a resource is added.

        This should never be used by a normal appplication, it's for ddm-viewer.
        """
        self.__added_handlers.append(handler)

    def remove_added_handler(self, handler):
        """Remove a handler added with add_added_handler """
        self.__added_handlers.remove(handler)

    ## of course, currently we never remove resources...
    def add_removed_handler(self, handler):
        """Add a handler that will be called when a resource is removed

        This should never be used by a normal appplication, it's for ddm-viewer.
        """
        self.__removed_handlers.append(handler)

    def remove_removed_handler(self, handler):
        """Remove a handler added with add_removed_handler"""
        self.__removed_handlers.remove(handler)

    def get_resources(self):
        """Get currently-known resources; keep in mind, it's totally undefined what we might currently know, it just depends on what queries people have done"""
        return self.__resources.values()

    def query(self, method, fetch=None, single_result=False, **kwargs):
        """Create a query object.

        Arguments:
        method -- the method to call. A tuple of (namespace_uri, name)
        fetch -- the fetch string to use for retrieving data. (default '+')
        single_result -- if True, the method is expected to return exactly one object, and an
           error will result if it returns 0 or more than 1 object. If False,
           the method returns a possibly-empty list of objects. (default False)

        Additional keyword arguments are passed to the query
        method. The query will not actually be sent until you call
        execute() on it.
        
        """
        raise NotImplementedException()

    def query_resource(self, resource, fetch):
        """Create a query object for the standard system method 'getResource'.

        Arguments:
        resource -- a resource object or resource ID
        fetch -- the fetch string to use for retrieving data. (default '+')

        The query will return the resource object if found, otherwise
        an error will result. The query will not actually be sent
        until you call execute() on it.
        
        """

        if isinstance(resource, Resource):
            resource_id = resource.resource_id
        else:
            resource_id = resource
        
        return self.query(("http://mugshot.org/p/system", "getResource"),
                          fetch,
                          single_result=True,
                          resourceId=resource_id)
                       
    def update(self, method,  **kwargs):
        """Create a query object for an update method

        Arguments:
        method -- the method to call. A tuple of (namespace_uri, name)

        Additional keyword arguments are passed to the query
        method. The query will not actually be sent until you call
        execute() on it. Update methods don't have a result, but you can
        still add handlers to be called on success or error.
        
        """
        
        raise NotImplementedException()

    def _reset(self):
        self.__resources = {}
        self.global_resource = self._ensure_resource("online-desktop:/o/global", "online-desktop:/p/o/global")
        self.self_resource = None
        self.global_resource.connect(self.__on_self_changed, "self")
    
    def _get_resource(self, resource_id):
        return self.__resources[resource_id]

    def _ensure_resource(self, resource_id, class_id):
        try:
            return self.__resources[resource_id]
        except KeyError:
            resource = self.__resources[resource_id] = Resource(self, resource_id, class_id)

            for handler in self.__added_handlers:
                handler(resource)
            
            return resource

    def _on_ready(self):
        self.ready = True
        for handler in self.__ready_handlers:
            handler()    

    def __on_self_changed(self, global_resource):
        self.self_resource = global_resource.self;
