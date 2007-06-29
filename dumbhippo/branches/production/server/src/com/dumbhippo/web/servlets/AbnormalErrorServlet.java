package com.dumbhippo.web.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
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

	private static final String[] DEFAULT_MESSAGES = {
		"Bet you didn't think you'd end up here, did you? Neither did we.",
		"Don't think of this as an error page. Think of it as a 5 second vacation.",
		"Error 500. Collect them all!",
		"Error pages need love too. No hard feelings?",
		"Every time an error page appears, an angel gets its wings. Isn't that how the phrase goes?",
		"Happy Error Page Day! Now go have some cake.",
		"If seeing an error page was on your list of things to do today, you're	in luck.",
		"If you had a penny for every error page you saw, you'd be one penny richer.",
		"Now you can tell your friends you saw an error page. Hopefully they forgot about last week's unicorn sighting.",
		"Our server temporarily ran out of coffee. (Didn't you know servers run on coffee?)",
		"Remember when error pages used to spill boiling water into your lap? Glad those days are over.",
		"Some day your grandkids will ask what it was like to live in a world with error pages. Sadly, we still live in that world.",
		"Someone must be testing our circuit breakers again.",
		"Sometimes we need an error page to appreciate the good things in life.",
		"Sorry for the error. One of our employees isn't getting dessert tonight.",
		"Sorry, the page you wanted is taking another call.",
		"So this is what happens when we pull out that wire. Interesting.",
		"Take this error page as an opportunity to get up and stretch. You deserve it.",
		"That's one slippery page, huh?",
		"The hamster running our server fell off his little wheel.",
		"This error page is just helping to build suspense for what you really wanted to see.",
		"We must have crossed our thingy with our whatzit again. Or something.",
		"Woah! How'd you see this? We have millions of monkeys checking for errors like this every day. Our crew has been notified of the problem and will be unloading more monkeys to address this area.",
		"Would you rather have an error page on your screen or bears in your house? We thought so.",
		"Yes, it's an error page. But at least it's not raining.",
		"You found our secret error page! Now try to find our prize closet.",
		"You will find true happiness! But we feel bad for the guy who got an error message in his fortune cookie.",
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
    	throw new HumanVisibleException(StringUtils.getRandomString(DEFAULT_MESSAGES));
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
