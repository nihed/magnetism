package com.dumbhippo.web;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

public class BeanTag extends SimpleTagSupport {
	private static final Log logger = GlobalSetup.getLog(BeanTag.class);
	
	enum Scope {
		PAGE,
		REQUEST,
		SESSION,
		APPLICATION
	}
	
	String id;
	Scope scope;
	Class clazz;
	
	private Object findObject() {
		PageContext context = (PageContext)getJspContext();
		
		switch (scope) {
		case PAGE:
			return context.getAttribute(id);
		case REQUEST:
			return context.getRequest().getAttribute(id);
		case SESSION:
			return context.getSession().getAttribute(id);
		case APPLICATION:
			return context.getServletContext().getAttribute(id);
		}
		
		throw new IllegalStateException();
	}
	
	public void storeObject(Object o) {
		PageContext context = (PageContext)getJspContext();
		
		switch (scope) {
		case PAGE:
			context.setAttribute(id, o);
			break;
		case REQUEST:
			context.getRequest().setAttribute(id, o);
			break;
		case SESSION:
			context.getSession().setAttribute(id, o);
			break;
		case APPLICATION:
			context.getServletContext().setAttribute(id, o);
			break;
		}		
	}
	
	private SigninBean getSigninBean() {
		PageContext context = (PageContext)getJspContext();
		
		return SigninBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private BrowserBean getBrowserBean() {
		PageContext context = (PageContext)getJspContext();
		
		return BrowserBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private Object instantiateObject() {
		logger.debug("Instantiating " + clazz.getName());
		// We special-case the SigninBean
		if (clazz == SigninBean.class) 
			return getSigninBean();
		
		Object o;
		try {
			o = clazz.newInstance();
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		}
		
		for (Field f : clazz.getDeclaredFields()) {
			if (f.isAnnotationPresent(Signin.class) &&
				f.getType().isAssignableFrom(SigninBean.class)) {
				logger.debug("Injecting SigninBean into " + f.getName());
				try {
					// Like EJB3, we support private-field injection
					f.setAccessible(true);
					f.set(o, getSigninBean());
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Error injecting SigninBean", e);
				}
			} else if (f.isAnnotationPresent(Browser.class) &&
				f.getType().isAssignableFrom(BrowserBean.class)) {
				logger.debug("Injecting BrowserBean into " + f.getName());
				try {
					// Like EJB3, we support private-field injection
					f.setAccessible(true);
					f.set(o, getBrowserBean());
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Error injecting BrowserBean", e);
				}
			}
		}
		
		return o;
	}
	
	public void doTag() throws IOException, JspException {
		Object o = findObject();
		if (o == null) {
			o = instantiateObject();			
			storeObject(o);
		}
		
		JspFragment frag = getJspBody();
		if (frag != null)
			frag.invoke(getJspContext().getOut());
	}
	
	public void setId(String i) {
		id = i;
	}
	
	public void setScope(String s) {
		if (s.equals("page"))
			scope = Scope.PAGE;
		else if (s.equals("request"))
			scope = Scope.REQUEST;
		else if (s.equals("session"))
			scope = Scope.SESSION;
		else if (s.equals("application"))
			scope = Scope.APPLICATION;
		else
			throw new IllegalArgumentException("Bad scope value " + s);
	}
	
	public void setClass(Class c) {
		clazz = c;
	}
}
