from Resource import Resource

ERROR_FAILED = -1
ERROR_NO_CONNECTION = -2
ERROR_BAD_REPLY = -3
ERROR_BAD_REQUEST = 400
ERROR_FORBIDDEN = 403
ERROR_ITEM_NOT_FOUND = 404
ERROR_INTERNAL_SERVER_ERROR = 500

class Query(object):
    """
    A query or update sent to the server

    Queries are created by the query() and update() methods of the data model.
    Once a query is created, you will normally add handlers for successful
    completion and/or errors and then call execute()
    """
    
    def __init__(self, params, single_result=False):
        self.__single_result = single_result
        self.__handlers = []
        self.__error_handlers = []

        self._params = {}
        for k,v in params.iteritems():
            if not isinstance(k,basestring):
                raise Exception("Parameter names must be strings")
            
            if isinstance(v,basestring):
                self._params[k] = v
            elif isinstance(v,Resource):
                self._params[k] = v.resource_id
            else:
                self._params[k] = str(v)
    
    def add_handler(self, handler):
        """Add a handler to be called in case of success

        A query created for a query method will call the handler with
        a single argument, which is either a single object (if
        single_result=True was passed to query()) or a list of
        objects.

        A query created for an update method will call the handler
        without any arguments.
        
        """
        self.__handlers.append(handler)

    def add_error_handler(self, handler):
        """Add a handler to be called in case of error.

        The handler will be called with two arguments: an error code
        indicating the type of error, and a human-readable string
        describing the error.
        
        """
        
        self.__error_handlers.append(handler)

    def execute():
        """Send the query or update to the server"""
        raise NotImplementedException()
    
    def _on_success(self, result=None):
        if result == None: # Update
            for handler in self.__handlers:
                handler()
        else:              # Query
            if self.__single_result:
                if len(result) == 0:
                    self._on_error(ERROR_ITEM_NOT_FOUND, "No result items for a request that should have one result item")
                    return
                elif len(result) > 1:
                    self._on_error(ERROR_BAD_REPLY, "Multiple result items for a request that should have one result item")
                    return
                result = result[0]

            for handler in self.__handlers:
                handler(result)

    def _on_error(self, code, message):
        for handler in self.__error_handlers:
            handler(code, message)
