class DataModel:
    """
    The data model for the Mugshot desktop session.

    There is a singleton DataModel object for each server session, retrieved by
    calling the constructor DataModel(server_name). server_name is optional, and if
    omitted, the session will be for the official official mugshot.org server.
    
    """
    def __new__(cls, *args, **kwargs):
        pass
