from super.dirtree import DirTree

class Merge:

    """Data model for a <merge/> element"""
    
    def __init__(self, service, src, dest, exclude, expand, symlink, hot):
        self.service = service
        self.src = src
        self.dest = dest
        self.exclude = exclude
        self.expand = expand
        self.symlink = symlink
        self.hot = hot

    def add_to_tree(self, tree):
        """Adds the files represented by the <merge/> element to a DirTree."""
        excludes = tree.standard_excludes()
        if self.exclude is not None:
            excludefile = self.service.expand(self.exclude)
            excludes.extend(tree.compile_excludes(excludefile))

        src = self.service.expand(self.src)
        if self.dest:
            dest = self.service.expand(self.dest)
        else:
            dest = None
        tree.add_tree(dest, src,
                      self.symlink, self.expand, self.hot,
                      excludes)

        
