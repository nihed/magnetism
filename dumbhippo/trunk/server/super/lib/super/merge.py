from super.dirtree import DirTree

class Merge:
    def __init__(self, service, src, dest, exclude, expand, symlink, hot):
        self.service = service
        self.src = src
        self.dest = dest
        self.exclude = exclude
        self.expand = expand
        self.symlink = symlink
        self.hot = hot

    def add_to_tree(self, tree):
        excludes = tree.standard_excludes()
        if self.exclude != None:
            excludefile = self.service.expand(self.exclude)
            excludes.extend(tree.compile_excludes(excludefile))

        src = self.service.expand(self.src)
        if self.dest:
            dest = self.service.expand(self.dest)
        else:
            dest = None
        tree.add_tree(dest, src,
                      self.symlink, self.expand,
                      excludes)

        
