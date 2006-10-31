package com.dumbhippo.web.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.HumanVisibleException;

/**
 * 
 * This is a "bug buddy"/"crash" servlet that handles errors we weren't expecting to get.
 * 
 * @author hp
 *
 */
public class AbnormalErrorServlet extends AbstractServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = GlobalSetup.getLogger(AbnormalErrorServlet.class);

    private static final String[] errorVars = {
    	"javax.servlet.error.request_uri",
        "javax.servlet.error.status_code",
        "javax.servlet.error.exception_type",
        "javax.servlet.error.message",
        "javax.servlet.error.exception"        
    };

	@Override
	public void init() {
	}
    
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws HttpException,
		HumanVisibleException, IOException, ServletException {
    	try {
    		logger.error("Abnormal error occurred");
    		for (String var : errorVars) {
    			logger.error("{} = {}", var, request.getAttribute(var));
    		}
    		Throwable t = (Throwable) request.getAttribute("javax.servlet.error.exception");
    		if (t != null) {
    			logger.error("Backtrace:", t);
    			Throwable root = ExceptionUtils.getRootCause(t);
    			if (root != t) {
    				logger.error("Root cause is {} message: {}", 
    						root.getClass().getName(), root.getMessage());
    			}
    		}
    	} catch (Throwable t) {
    		// not sure what happens if the error servlet throws an error, but it can't be good, so 
    		// we unconditionally eat it here
    		logger.error("Error servlet broke! ", t);
    	}
    	// now redirect to error page
    	throw new HumanVisibleException("Woah! How'd you see this?  We have millions of monkeys checking for errors like this every day."
    			+ "  Our crew has been notified of the problem and will be unloading more monkeys to address this area.");
    }
    
    @Override
    protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws HttpException,
    	HumanVisibleException, IOException, ServletException {
    	handleRequest(request, response);
    	return null;
    }
    
    @Override
    protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws HttpException,
    	HumanVisibleException, IOException, ServletException {
    	handleRequest(request, response);
    	return null;
    }

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return false;
	}
}
