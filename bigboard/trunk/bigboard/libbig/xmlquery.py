import xml.dom.minidom

def _traverse_nodes(node, matches, index): 
    gather = index+1 == len(matches)
    gather_result = []
    if node.nodeType == xml.dom.Node.ELEMENT_NODE:
        for subnode in node.childNodes:
            if subnode.nodeType == xml.dom.Node.ELEMENT_NODE and subnode.tagName == matches[index]:
                if gather:
                    gather_result.append(subnode)
                else:
                    _traverse_nodes(subnode, matches, index+1)
    if not gather:
        raise KeyError("Couldn't find path %s from node %s" % ('/'.join(matches), node))
    return gather_result

def query(node, *queries):
    """A braindead-simple xpath-like minilanguage for querying an xml node.
    Examples:
    
    foo/bar/baz*    Returns all baz subelements of foo/bar
    foo/bar/baz     Returns the first baz subelement of foo/bar
    \@date          Returns the date attribute of the current node
    bar#            Returns the text content the first node named bar
    baz\@moo        Returns the moo attribute of the first node named baz
    
    A query may also be a tuple of (querystr, conversion_func).  So for example:
    
    (timestamp, link, contents) = xml_query(node, ("\@timestamp", int), "\@link", "content#")"""
    
    def gather_node_value(node):
        result = ""
        for subnode in node.childNodes:
            if subnode.nodeType == xml.dom.Node.TEXT_NODE:
                result += subnode.nodeValue
        return result
    
    def first_or_lose(query, elt_name, nodeset):
        if len(nodeset) == 0:
            raise KeyError("Couldn't find element %s from query %s" % (elt_name, query))   
        return nodeset[0]
    
    results = []
    for query in queries:
        conversion_func = lambda x: x
        if type(query) == tuple:
            (query_str, conversion_func) = query
        else:
            query_str = query
            
        query_components = query_str.split('/')
        last_query = query_components[-1]
        
        get_first_element_func = lambda results: first_or_lose(query_str, last_query, results)
        
        attr_pos = last_query.find('@')
        hash_pos = last_query.find('#')
        star_pos = last_query.find('*')
        if attr_pos > 0:
            query_func = lambda results: get_first_element_func(results).getAttribute(last_query[attr_pos+1:])
            last_query = last_query[:attr_pos]            
        elif hash_pos > 0:
            query_func = lambda results: gather_node_value(get_first_element_func(results))
            last_query = last_query[:hash_pos]
        elif star_pos > 0:
            query_func = lambda results: results
            last_query = last_query[:star_pos]
        else:
            query_func = get_first_element_func
        node_matches = query_components[:-1]
        node_matches.append(last_query)
        resultset = _traverse_nodes(node, node_matches, 0)
        result = conversion_func(query_func(resultset))      
        results.append(result)
    if len(results) == 1:
        return results[0]
    else:
        return results

def get_attrs(node, attrlist):
    attrs = {}
    for attr in attrlist:
        if isinstance(attr, str):
            (attrname, optional) = (attr, False)
        else:
            (attrname, optional) = attr
        attrs[attrname] = node.getAttribute(attrname)
        if (not attrs[attrname]) and (not optional):
            raise KeyError("Failed to find attribute %s of node %s" %(attrname, node))
    return attrs    
        
def get_element(node, path):
    """Traverse a path like foo/bar/baz from a DOM node, using the first matching
    element."""
    return query(node, path)

def get_element_value(node, path):
    return query(node, path+"#")

