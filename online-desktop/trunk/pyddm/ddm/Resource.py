UPDATE_ADD = 0
UPDATE_REPLACE = 1
UPDATE_DELETE = 2
UPDATE_CLEAR = 3

CARDINALITY_01 = 0
CARDINALITY_1 = 1
CARDINALITY_N = 2

class Resource:
    """A local proxy for a data resource on the remote server

    A Resource object is used for retrieving data and getting notification
    about resources (data objects) on the server. Resource objects are returned
    as the result of calling query() or queryResource() on the data model, or
    from resource-valued properties of other resources.

    Resource implements __getattr__ as a shortcut so you can use resource.name
    rather than resource.get('name').

    """

    def connect(self, handler, property=None):
        """Add a handler to be called when the resource changes

        Arguments:
        handler: the callback function. It will be called with a single argument,
           the resource.
        property: if None, the handler will be called on any property change,
           otherwise it can be the unqualified name of a property or
           a tuple (namespace_uri, unqualified_name), and the handler will be
           called on only that property changing (default=None).

        """
        self.__connections.append((handler, property))

    def disconnect(self, handler):
        """Remove a handler added with connect()"""
        for i in range(0, len(self.__connections)):
            if self.__connections[i][0] == handler:
                del self.__connections[i]
                return
                
    def get(self, property):
        """Retrieve a resource property. 

        Arguments:
        property: The unqualified name of name of a property, or a
           tuple (namespace_uri, unqualified_name).

        raises AttributeError will if the property is not found.
        
        A non-existent property could mean that you didn't query it from the
        server by passing an appropriate fetch string, that the server doesn't
        support that property on this resource, or that the property is an
        optional single-valued property and doesn't have a value. These cases are
        not distinguished. (If a multi-valued property is supported by the server
        then you'll get an empty list if there are no values for it.)
           
        """
        
        if isinstance(property, tuple):
            return self._get_by_id(property)
        else:
            return self.__getattr__(property)

    def get_properties(self):
        return self.__properties.items()
        
    def __init__(self, model, resource_id, class_id):
        """Create a new resource node. Do not call directly."""
        self.model = model
        self.resource_id = resource_id
        self.class_id = class_id
        self.__names = {}
        self.__properties = {}
        self.__connections = []

    def __insert_property_id(self, uri, name):
        property_id = (uri, name)
        try:
            old = self.__names[name]
            if isinstance(old, list):
                if old.count(property_id) == 0:
                    old.append(property_id)
            else:
                if old != property_id:
                    self.__names[name] = [ old, property_id ]
        except KeyError:
            self.__names[name] = property_id

        return property_id
                    
    def _update_property(self, property, update, cardinality, value, notifications=None):
        
        property_id = self.__insert_property_id(property[0], property[1])

        if update == UPDATE_DELETE and not self.__properties.has_key(property_id):
            raise Exception("Remove of a property we don't have")

        if cardinality == CARDINALITY_01:
            if update == UPDATE_REPLACE:
                self.__properties[property_id] = value
            elif update == UPDATE_ADD:
                if self.__properties.has_key(property_id):
                    raise Exception("add update for cardinality 01 with a property already there")
                self.__properties[property_id] = value
            elif update == UPDATE_DELETE:
                if self.__properties[property_id] != value:
                    raise Exception("remove of a property value not there")
                del self.__properties[property_id]
            elif update == UPDATE_CLEAR:
                try:
                    del self.__properties[property_id]
                except KeyError:
                    pass
        elif cardinality == CARDINALITY_1:
            if update == UPDATE_REPLACE:
                self.__properties[property_id] = value
            elif update == UPDATE_ADD:
                if self.__properties.has_key(property_id):
                    raise Exception("add update for cardinality 1 with a property already there")
                self.__properties[property_id] = value
            elif update == UPDATE_DELETE:
                raise Exception("remove update for a property with cardinality 1")
            elif update == UPDATE_CLEAR:
                raise Exception("clear update for a property with cardinality 1")
        elif cardinality == CARDINALITY_N:
            if update == UPDATE_REPLACE:
                self.__properties[property_id] = [value]
            elif update == UPDATE_ADD:
                try:
                    self.__properties[property_id].append(value)
                except KeyError:
                    self.__properties[property_id] = [value]
            elif update == UPDATE_DELETE:
                try:
                    self.__properties[property_id].remove(value)
                except ValueError:
                    raise Exception("remove of a property value not there")
            elif update == UPDATE_CLEAR:
                self.__properties[property_id] = []

        if notifications != None and len(self.__connections) != 0:
            notifications.add(self, property_id)

    def __repr__(self):
        return 'Resource(' + self.resource_id + ')'

    def __cmp__(self, other):
        if not isinstance(other, Resource):
            return -1
        return self.resource_id.__cmp__(other.resource_id)

    def __eq__(self, other):
        if not isinstance(other, Resource):
            return False
        return self.resource_id == other.resource_id

    def __ne__(self, other):
        if not isinstance(other, Resource):
            return True
        return self.resource_id != other.resource_id

    def __hash__(self):
        return self.resource_id.__hash__()

    def _get_by_id(self, property_id):
        try:
            return self.__properties[property_id]
        except KeyError:
            raise AttributeError(property)
        
    def __getattr__(self, name):
        try:
            property_id = self.__names[name]
        except KeyError:
            raise AttributeError(name)
        
        if isinstance(property_id,list):
            raise Exception("%s is ambigious. Possibilities are %s", name, property_id)

        return self._get_by_id(property_id)
        
    def _dump(self):
        print self.resource_id, self.class_id        
        for id in self.__properties:
            if id[0] == self.class_id:
                short = id[1]
            else:
                short = id[0] + "#" + id[1]
            print "   ", short, ":", self.__properties[id]

    def _on_resource_change(self):
        for (handler,property) in self.__connections:
            if property == None:
                handler(self)
                
    def _on_property_change(self, changed_property):
        for (handler,property) in self.__connections:
            if property != None:
                if isinstance(property,tuple):
                    if property == changed_property:
                        handler(self)
                else:
                    if property == changed_property[1]:
                        handler(self)
