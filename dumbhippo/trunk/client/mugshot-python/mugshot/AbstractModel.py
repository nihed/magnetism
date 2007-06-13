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
        self.__connected_handlers = []
        self.__disconnected_handlers = []
        self.__resources = {}
        self.connected = False

    def add_connected_handler(self, handler):
        """Add a handler that will be called when we become connected to the server"""
        self.__connected_handlers.append(handler)

    def remove_connected_handler(self, handler):
        """Remove a handler added with add_connected_handler"""
        self.__connected_handlers.remove(handler)

    def add_disconnected_handler(self, handler):
        """Add a handler that will be called when we become disconnected from the server"""
        self.__disconnected_handlers.append(handler)

    def remove_disconnected_handler(self, handler):
        """Remove a handler added with add_disconnected_handler"""
        self.__disconnected_handlers.remove(handler)

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

    def query_resource(self, resource_id, fetch):
        """Create a query object for the standard system method 'getResource'.

        Arguments:
        method -- the ID of the resource to retrieve
        fetch -- the fetch string to use for retrieving data. (default '+')

        The query will return the resource object if found, otherwise
        an error will result. The query will not actually be sent
        until you call execute() on it.
        
        """
        
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

    def _get_resource(self, resource_id):
        return self.__resources[resource_id]

    def _ensure_resource(self, resource_id, class_id):
        try:
            return self.__resources[resource_id]
        except KeyError:
            resource = self.__resources[resource_id] = Resource(self, resource_id, class_id)
            return resource

    def _on_connected(self):
        if self.connected:
            return

        # On reconnection, all previous state is irrelevant
        self.__resources = {}
        
        self.connected = True
        for handler in self.__connected_handlers:
            handler()

    def _on_disconnected(self):
        if not self.connected:
            return
        
        self.connected = False
        for handler in self.__disconnected_handlers:
            handler()

    def _set_self_id(self, self_id):
        self.self_id = self_id
