package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.polling.SwarmPollingSystemMBean;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.WebEJBUtil;

public class ExternalServiceServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalServiceServlet.class);
	
	static final long serialVersionUID = 1;
	
	private Configuration config;
	private String accessKey;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
		accessKey = config.getPropertyFatalIfUnset(HippoProperty.EXTERNAL_SERVICE_KEY);
	}	

	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		logger.debug("full request is: {}", request.toString());

        logRequest(request, "POST");
  
        String key = request.getParameter("eak");
        if (key == null || !key.equals(accessKey))
        	throw new HttpException(HttpResponseCode.FORBIDDEN, "Invalid or unspecified access key");
        
        if (request.getPathInfo().equals("/notify-polling-tasks")) {
			MBeanServer server = MBeanServerLocator.locateJBoss();
			SwarmPollingSystemMBean swarm;			
			try {
				swarm = (SwarmPollingSystemMBean) MBeanProxyExt.create(SwarmPollingSystemMBean.class, "dumbhippo.com:service=SwarmPollingSystem", server);
			} catch (MalformedObjectNameException e) {
				throw new RuntimeException(e);
			}
			
        	String json = IOUtils.toString(request.getInputStream());
        	JSONObject obj;
        	JSONArray tasksProp;
        	try {
				obj = new JSONObject(json);
				tasksProp = obj.getJSONArray("tasks");
			} catch (JSONException e) {
				throw new ServletException(e);
			}
        	Collection<String> tasks = new HashSet<String>();
        	int len = tasksProp.length();
        	for (int i = 0; i < len; i++) {
        		try {
					tasks.add(tasksProp.getString(i));
				} catch (JSONException e) {
					throw new ServletException(e);
				}
        	}
        	swarm.runExternalTasks(tasks);
        }     
        
		response.setContentType("text/plain");
		response.getOutputStream().write("".getBytes());
		
		return null;
	}
	
	@Override
	protected boolean isReadWrite(HttpServletRequest request) {
		// The method is GET, since we need links that the user can just click upon,
		// but they have side effects. This is OK since the links are unique, so 
		// caching won't happen.
		
		return true;
	}
	

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return true;
	}
}
