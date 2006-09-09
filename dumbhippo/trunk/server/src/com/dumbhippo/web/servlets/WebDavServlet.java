package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.dav.DavHandler;
import com.dumbhippo.dav.DavHttpStatusException;
import com.dumbhippo.dav.DavNode;
import com.dumbhippo.dav.DavResourceType;
import com.dumbhippo.dav.DavStatusCode;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.SharedFileDavFactory;
import com.dumbhippo.server.SharedFileSystem;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class WebDavServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(WebDavServlet.class);

	// there's probably some way to query this from the servlet context
	private String servletRoot = "/dav";
	private String servletRootWithSlash = "/dav/";
	
	enum DavMethod {
		PROPFIND {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException, DavHttpStatusException {
				servlet.doPropFind(request, response);
			}
		},
		PROPPATCH {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doPropPatch(request, response);
			}
		},
		MKCOL {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doMkCol(request, response);
			}
		},
		COPY {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doCopy(request, response);
			}
		},
		MOVE {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doMove(request, response);
			}
		},
		LOCK {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doLock(request, response);
			}
		},
		UNLOCK {
			@Override
			void doIt(WebDavServlet servlet, HttpServletRequest request,
					HttpServletResponse response) throws ServletException, IOException {
				servlet.doUnlock(request, response);
			}
		};

		abstract void doIt(WebDavServlet servlet, HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException, DavHttpStatusException;
	}

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String m = request.getMethod();

		DavMethod davMethod = null;
		try {
			davMethod = DavMethod.valueOf(m);
		} catch (IllegalArgumentException e) {
			; // leave it null
		}

		if (davMethod != null) {
			logRequest(request, davMethod.name());
			try {
				davMethod.doIt(this, request, response);
			} catch (DavHttpStatusException e) {
				logger.debug("Sending DAV status {}: {}", e.getCode(), e.getMessage());
				response.sendError(e.getCode().getCode(), e.getMessage());
			}
			logger.debug("{} complete", davMethod.name());
		} else {
			super.service(request, response);
		}
	}

	private String getNodePath(HttpServletRequest request) throws DavHttpStatusException {
		String withRoot = request.getRequestURI();
		if (!withRoot.startsWith(servletRootWithSlash)) {
			if (withRoot.equals(servletRoot))
				return "/";
			else
				throw new DavHttpStatusException(DavStatusCode.NOT_FOUND, "DAV uris start with " + servletRoot);
		}
		// note we keep the leading slash here
		return withRoot.substring(servletRoot.length());
	}
	
	protected void doPropFind(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, DavHttpStatusException {
		DavHandler handler = getDavHandler(request);
		
		XmlBuilder xml = new XmlBuilder();
		xml.appendStandaloneFragmentHeader();
		
		handler.propFind(xml, getNodePath(request),
					request.getHeader("Depth"),
					request.getInputStream());
		
		byte[] bytes = xml.getBytes();
		response.setContentLength(bytes.length);
		response.setContentType("text/xml");
		response.setStatus(DavStatusCode.MULTI_STATUS.getCode());
		OutputStream out = response.getOutputStream();
		out.write(bytes);
		out.flush();
		out.close();
		
		logger.debug("Wrote propfind reply {}", xml.toString());
	}

	protected void doPropPatch(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	protected void doMkCol(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	protected void doCopy(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	protected void doMove(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	protected void doLock(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	protected void doUnlock(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}	
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		response.sendError(DavStatusCode.FORBIDDEN.getCode());
	}
	
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		// the DAV header is "compliance classes", if we supported locking it 
		// would be "1,2"
		response.addHeader("DAV", "1");
		response.addHeader("Allow", "OPTIONS, GET, HEAD, POST, DELETE, TRACE, " + 
				"PROPFIND" /* + 
				"PROPPATCH, COPY, MOVE, MKCOL, LOCK, UNLOCK" */);
		response.addHeader("MS-Author-Via", "DAV");
	}
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException, HttpException {
		throw new HttpException(HttpResponseCode.FORBIDDEN, "POST not supported");
	}
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException, HttpException {
		
		DavHandler handler = getDavHandler(request);
		DavNode node;
		try {
			node = handler.findNodeByPath(getNodePath(request));
		} catch (NotFoundException e) {
			logger.debug("DAV did not find URI '{}'", request.getRequestURI());
			throw new HttpException(HttpResponseCode.NOT_FOUND, "No such file");
		} catch (DavHttpStatusException e) {
			logger.debug("Sending DAV status {}: {}", e.getCode(), e.getMessage());
			response.sendError(e.getCode().getCode(), e.getMessage());
			return null;
		}
	
		response.setDateHeader("Last-Modified", node.getLastModified());
		
		if (node.getResourceType() == DavResourceType.FILE) {
			response.setContentLength(node.getContentLength());
			response.setContentType(node.getContentType());
			
			OutputStream out = response.getOutputStream(); 
			node.write(out);
			out.flush();
			out.close();
		} else if (node.getResourceType() == DavResourceType.COLLECTION) {
			XmlBuilder html = new XmlBuilder();
			html.appendHtmlHead("Listing of " + node.getDisplayName());
			
			html.openElement("body");
			html.openElement("div");
			html.openElement("ul");
			
			for (DavNode child : node.getChildren()) {
				html.openElement("li");
				html.appendTextNode("a", child.getDisplayName(),
						"href",
						servletRoot + handler.getNodeRelativeUrlString(child)); 
			}
			
			html.closeElement();
			html.closeElement();
			html.closeElement();
			
			byte[] bytes = html.getBytes();
			response.setContentLength(bytes.length);
			response.setContentType("text/html");
			OutputStream out = response.getOutputStream();
			out.write(bytes);
			out.flush();
			out.close();
		}
		
		return null;
	}
	
	// just always produces a new instance so we can ignore thread-related stuff
	private DavHandler getDavHandler(HttpServletRequest request) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		URL url;
		try {
			url = new URL(config.getBaseUrl(), servletRoot);
		} catch (MalformedURLException e) {
			throw new RuntimeException("malformed dav root url");
		}
		logger.debug("Full URL of dav servlet is '{}'", url.toExternalForm());
		
		/*
		Set<DavTestNode> children = new HashSet<DavTestNode>();
		
		children.add(new DavTestNode("file-c-1"));
		children.add(new DavTestNode("file-d-1"));
		DavTestNode folder = new DavTestNode("folder-a-0", children);
		children.clear();
		children.add(folder);
		children.add(new DavTestNode("file-a-0"));
		children.add(new DavTestNode("file-b-0"));
		DavNode root = new DavTestNode(null, children);
		*/
		DavNode root = SharedFileDavFactory.newRoot(SigninBean.getForRequest(request).getViewpoint(),
				WebEJBUtil.defaultLookup(SharedFileSystem.class));
		
		return new DavHandler(root, url);
	}
}
