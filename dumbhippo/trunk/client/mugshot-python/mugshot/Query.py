ERROR_NO_CONNECTION = -1
ERROR_BAD_REPLY = -2
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
    
    def __init__(self):
        self.__handlers = []
        self.__error_handlers = []
    
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
    
    def _on_success(self, result):
        for handler in self.__handlers:
            handler(result)

    def _on_error(self, code, message):
        for handler in self.__error_handlers:
            handler(code, message)
