package com.dumbhippo.dav;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.NotFoundException;

public class DavHandler {
	
	private static final Logger logger = GlobalSetup.getLogger(DavHandler.class);
	
	private URL rootUrl;
	private DavNode rootNode;
	
	public DavHandler(DavNode rootNode, URL rootUrl) {
		this.rootNode = rootNode;
		this.rootUrl = rootUrl;
	}
		
	public DavNode findNodeByPath(String path) throws NotFoundException {
		return findNodeByPath(path, false);
	}
	
	public DavNode findParentByPath(String path) throws NotFoundException {
		return findNodeByPath(path, true);
	}
	
	private String[] splitElements(String path) throws NotFoundException {
		if (!path.startsWith("/"))
			throw new NotFoundException("path doesn't start with /");		
		
		if (path.equals("/"))
			return new String[0];
		
		// drop leading and trailing /
		path = path.substring(1,
				path.endsWith("/") ? path.length()-1 : path.length());
	
		String[] elements = path.split("/");
		
		if (elements.length == 0)
			throw new NotFoundException("empty path?");

		return elements;
	}
	
	private String splitBasename(String path) {
		String[] elements;
		try {
			elements = splitElements(path);
		} catch (NotFoundException e) {
			throw new RuntimeException("invalid path");
		}
		if (elements.length == 0)
			throw new RuntimeException("root node has no basename");
		else
			return elements[elements.length - 1];
	}
	
	private DavNode findNodeByPath(String path, boolean parent) throws NotFoundException {
		
		//logger.debug("looking for node by path '{}'", path);
		
		String[] elements = splitElements(path);
		
		if (parent) {
			if (elements.length == 0)
				throw new NotFoundException("Folder has no parent"); // root has no parent
			else
				elements[elements.length - 1] = null;
		}
		
		//logger.debug("split into {} from {}", Arrays.toString(elements), path);
		
		DavNode node = rootNode; // empty elements will just return this root
		for (String name : elements) {
			if (name == null)
				break; // happens when looking for the parent
			
			name = StringUtils.urlDecode(name);
			logger.debug("looking for '{}' underneath '{}'", name, node.getName());
			if (name.length() == 0)
				throw new NotFoundException("invalid path contains empty component");
			node = node.getChild(name);
		}
		return node;
	}
	
	private void buildPathWorker(DavNode node, StringBuilder sb) {
		String name = node.getName();
		if (name == null) {
			if (node != rootNode)
				throw new RuntimeException("null name on non-root node");
			if (sb.length() == 0)
				sb.insert(0, "/");
		} else {
			sb.insert(0, StringUtils.urlEncode(name));
			sb.insert(0, "/");
			buildPathWorker(node.getParent(), sb);
		}
	}
	
	private String buildPath(DavNode node) {
		StringBuilder sb = new StringBuilder();
		buildPathWorker(node, sb);
		return sb.toString();
	}
	
	public URL getNodeUrl(DavNode node) {
		try {
			return new URL(rootUrl.getProtocol(),
					rootUrl.getHost(),
					rootUrl.getPort(),
					rootUrl.getPath() + buildPath(node));
		} catch (MalformedURLException e) {
			throw new RuntimeException("getNodeUrl() resulted in bad url", e);
		}
	}
	
	public String getNodeUrlString(DavNode node) {
		return getNodeUrl(node).toExternalForm();
	}
	 
	public String getNodeRelativeUrlString(DavNode node) {
		return buildPath(node);
	}
	
	// if !includeValues then requested==null
	private void writePropStats(XmlBuilder xml, DavNode node,
			Set<DavProperty> requested, Set<String> unknownRequested, boolean includeValues) {
		/* There's a separate <propstat> for each status code, 
		 * so e.g. 200 OK vs. 404 not found
		 */
		Set<DavProperty> found = EnumSet.noneOf(DavProperty.class);
		Set<DavProperty> notFound = EnumSet.noneOf(DavProperty.class);
		
		Map<DavProperty,Object> properties = node.getProperties();
		
		if (includeValues) {
			for (DavProperty prop : requested) {
				if (properties.containsKey(prop)) {
					found.add(prop);
				} else {
					notFound.add(prop);
				}
			}
		} else {
			// list names available
			for (DavProperty prop : properties.keySet()) {
				found.add(prop);
			}
		}

		if (!found.isEmpty()) {
			xml.openElement(DavXmlElement.propstat);
			
			xml.openElement(DavXmlElement.prop);
			for (DavProperty prop : found) {
				if (includeValues) {
					Object value = properties.get(prop);
					prop.writeNode(xml, value);
				} else {
					xml.appendEmptyNode(prop.getElement());
				}
			}
			xml.closeElement(); // close prop
			
			xml.appendTextNode(DavXmlElement.status, "HTTP/1.1 200 OK");
			
			xml.closeElement(); // close propstat
		}
		
		if (!notFound.isEmpty() || unknownRequested != null) {
			xml.openElement(DavXmlElement.propstat);
			
			xml.openElement(DavXmlElement.prop);
			for (DavProperty prop : notFound) {
				xml.appendEmptyNode(prop.getElement());
			}
			if (unknownRequested != null) {
				for (String prop : unknownRequested) {
					xml.appendEmptyNode(prop);
				}
			}
			xml.closeElement(); // close prop
			
			xml.appendTextNode(DavXmlElement.status, "HTTP/1.1 404 NOT FOUND");
			
			xml.closeElement(); // close propstat
		}
	}
	
	private void collectDescendants(List<DavNode> children, DavNode node, int depth) {
		if (depth == 0)
			return;
		
		for (DavNode child : node.getChildren()) {
			children.add(child);
			collectDescendants(children, child, depth - 1);
		}
	}

	// if !includeValues then requested==null
	// hadTrailingSlash is because cadaver has an issue where it gets confused if it asks 
	// for "/foo/" and gets back "/foo" in the xml
	private void propFind(XmlBuilder xml, DavNode node, int depth, Set<DavProperty> requested, Set<String> unknownRequested,
			boolean includeValues, boolean hadTrailingSlash) {
		xml.openElement(DavXmlElement.multistatus, "xmlns", "DAV:");
		
		List<DavNode> all = new ArrayList<DavNode>();
		all.add(node);
		if (depth > 0)
			collectDescendants(all, node, depth);
		
		for (DavNode child : all) {
			xml.append("\n  ");
			xml.openElement(DavXmlElement.response);
			
			String nodeHref = getNodeUrlString(child);

			// the root path always ends in "/"
			// while nothing else should, unfortunately
			// this makes a bit of a mess since we don't
			// know what to expect here
			if (hadTrailingSlash) {
				if (!nodeHref.endsWith("/"))
					nodeHref = nodeHref + "/";
				// don't want it for any but the root/requested node which is first in the list
				hadTrailingSlash = false;
			} else {
				if (nodeHref.endsWith("/"))
					nodeHref = nodeHref.substring(0, nodeHref.length() - 1);
			}
			
			xml.append("\n    ");
			xml.appendTextNode(DavXmlElement.href, nodeHref);
			
			xml.append("\n    ");
			writePropStats(xml, child, requested, unknownRequested, includeValues);
			
			xml.append("\n  ");
			xml.closeElement();
			
			xml.append("\n");
		}
		
		xml.closeElement();
	}
	
	public void propFindValues(XmlBuilder xml, DavNode node, int depth, Set<DavProperty> requested, Set<String> unknownRequested,
			boolean hadTrailingSlash) {
		propFind(xml, node, depth, requested, unknownRequested, true, hadTrailingSlash);
	}
	
	public void propFindNames(XmlBuilder xml, DavNode node, int depth, boolean hadTrailingSlash) {
		propFind(xml, node, depth, null, null, false, hadTrailingSlash);
	}
	
	public void propFind(XmlBuilder xml, String requestPath, String depthHeader, InputStream requestContent)
		throws DavHttpStatusException {
		
		//logger.debug("propfind on {} depth {}", requestPath, depthHeader);
		
		DavNode node;
		try {
			node = findNodeByPath(requestPath);
		} catch (NotFoundException e) {
			throw new DavHttpStatusException(DavStatusCode.NOT_FOUND, e.getMessage(), e);
		}
		
		if (depthHeader != null)
			depthHeader = depthHeader.trim();
		
		int depth;
		// spec says that missing header should mean infinity
		if (depthHeader == null || depthHeader.length() == 0 ||
				depthHeader.equals("infinity"))
			depth = Integer.MAX_VALUE;
		else if (depthHeader.equals("0"))
			depth = 0;
		else if (depthHeader.equals("1"))
			depth = 1;
		else
			throw new DavHttpStatusException(DavStatusCode.BAD_REQUEST, "depth header has invalid value '" + depthHeader + "'");		
		
		//logger.debug("parsed depth to {}", depth);
		
		PropFindParser parser = new PropFindParser();
		try {
			// we have to read the stream into memory just to see if it's empty...
			// the SAX parser normally throws an error on an empty document, instead
			// we just immediately endDocument().
			String content = StreamUtils.readStreamUTF8(requestContent, 1024*640); // 640K ought to be enough for anyone! ;-)
			
			if (content.trim().length() == 0)
				parser.endDocument();
			else {
				parser.parse(content);
			}
		} catch (SAXException e) {
			throw new DavHttpStatusException(DavStatusCode.BAD_REQUEST, "bad XML in request: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new DavHttpStatusException(DavStatusCode.BAD_REQUEST, "failed to read request xml: " + e.getMessage(), e);
		}
		
		//logger.debug("parsed request");
		
		boolean hadTrailingSlash = requestPath.endsWith("/");
		
		if (parser.isNamesRequested()) {
			logger.debug("propfind list names depth {}", depth);
			propFindNames(xml, node, depth, hadTrailingSlash);
		} else if (parser.isAllRequested()) {
			logger.debug("propfind get all props depth {}", depth);
			propFindValues(xml, node, depth, EnumSet.allOf(DavProperty.class), null, hadTrailingSlash);
		} else {
			logger.debug("propfind requested props {} depth {}", parser.getPropertiesRequested(), depth);
			Set<String> requestedProps = parser.getPropertiesRequested();
			Set<String> unknownRequested = null; // demand-create since not often needed
			Set<DavProperty> requested = EnumSet.noneOf(DavProperty.class);
			for (String prop : requestedProps) {
				try {
					requested.add(DavProperty.fromElementName(prop));
				} catch (IllegalArgumentException e) {
					if (unknownRequested == null)
						unknownRequested = new HashSet<String>();
					unknownRequested.add(prop);
				}
			}
			propFindValues(xml, node, depth, requested, unknownRequested, hadTrailingSlash);
		}
	}
	
	public void put(String requestPath, String mimeType, InputStream requestContent)
		throws DavHttpStatusException {
		
		try {
			DavNode node = findNodeByPath(requestPath);
			try {
				// Replace an existing file
				node.replaceContent(mimeType, requestContent);
			} catch (IOException e1) {
				logger.debug("IO error replacing content of existing file {}: {}", requestPath, e1.getMessage());
				throw new DavHttpStatusException(DavStatusCode.FORBIDDEN, e1.getMessage());
			}
		} catch (NotFoundException e) {
			
			logger.debug("node {} does not already exist: {}", requestPath, e.getMessage());
			
			DavNode parent;
			try {
				parent = findParentByPath(requestPath);
			} catch (NotFoundException e1) {
				// the DAV spec requires this error if the folder doesn't exist
				logger.debug("parent folder not found for {}: {}", requestPath, e1.getMessage());
				throw new DavHttpStatusException(DavStatusCode.CONFLICT, "Folder does not exist");
			}
			String childName = splitBasename(requestPath);
			try {
				// Create a new file
				parent.createChild(childName, mimeType, requestContent);
			} catch (IOException e1) {
				logger.debug("error creating {}: {}", requestPath, e1.getMessage());
				throw new DavHttpStatusException(DavStatusCode.FORBIDDEN, e1.getMessage());
			}
		}
	}
	
	public void delete(String requestPath) throws DavHttpStatusException {
		DavNode node;
		try {
			node = findNodeByPath(requestPath);
		} catch (NotFoundException e) {
			logger.debug("did not find path {} to delete: {}", requestPath, e.getMessage());
			throw new DavHttpStatusException(DavStatusCode.NOT_FOUND, "File not found");
		}
		try {
			node.delete();
		} catch (IOException e) {
			logger.debug("failed to delete {}: {}", requestPath, e.getMessage());
			throw new DavHttpStatusException(DavStatusCode.FORBIDDEN, e.getMessage());
		}
	}
}
