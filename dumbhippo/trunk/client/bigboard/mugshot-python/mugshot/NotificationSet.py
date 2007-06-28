class NotificationSet:
    """
    Internal object used to keep track of notifications to be sent out.

    When we get a set of notifications from the server, we first make all updates
    to the internal data, then we call notification handlers, so that the notification
    handlers see the fully updated data model.
    """
    
    def __init__(self, model):
        self.__model = model
        self.__resources = {}

    def add(self, resource, property_id):
        try:
            properties = self.__resources[resource]
        except KeyError:
            properties = self.__resources[resource] = {}

        properties[property_id] = 1
            
    def send(self):
        for resource in self.__resources:
            resource._on_resource_change()
            for property_id in self.__resources[resource]:
                resource._on_property_change(property_id)
                
        
