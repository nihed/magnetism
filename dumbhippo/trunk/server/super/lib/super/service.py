class Service:
    def __init__(self, name, config):
        self.name = name
        self.config = config
        self.params = {}
        self.merges = []

    def get_name(self):
        return self.name

    def add_merge(self, merge):
        self.merges.append(merge)

    def set_parameter(self, name, value):
        self.params[name] = value

    def has_parameter(self, name):
        if self.params.has_key(name):
            return True
        else:       
            return self.config.has_parameter(name)

    def get_parameter(self, name):
        if self.params.has_key(name):
            return self.params[name]
        else:       
            return self.config.get_parameter(name)

    def expand_parameter(self, name):
        return self.config.expand_parameter(name, self)

    def expand(self, str):
        return self.config.expand(str, self)


    def init(self):
        pass
    def build(self):
        for merge in self.merges:
            merge.run()
    def start(self):
        pass
    def stop(self):
        pass
    def status(self):
        pass
