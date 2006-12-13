package com.dumbhippo.web.tags;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;
import com.dumbhippo.web.FromJspContext;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.Scope;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;

public class BeanTag extends SimpleTagSupport {
	private static final Logger logger = GlobalSetup.getLogger(BeanTag.class);
	
	String id;
	Scope scope;
	Class clazz;

	private Object findInScope(Scope s, String key) {
		PageContext context = (PageContext)getJspContext();
		
		switch (s) {
		case PAGE:
			return context.getAttribute(key);
		case REQUEST:
			return context.getRequest().getAttribute(key);
		case SESSION:
			return context.getSession().getAttribute(key);
		case APPLICATION:
			return context.getServletContext().getAttribute(key);
		}
		
		throw new IllegalArgumentException("bad scope value");
	}
	
	private Object findObject() {
		return findInScope(scope, id);
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
	
	private SigninBean getSigninBean(boolean mustBeSignedIn) {
		PageContext context = (PageContext)getJspContext();
		
		SigninBean signin = SigninBean.getForRequest((HttpServletRequest)context.getRequest());
		
		if (mustBeSignedIn && !signin.isValid())
			throw new RuntimeException("Sign-in only page " + ((HttpServletRequest)context.getRequest()).getRequestURI() + " accessed when not signed in");
		
		return signin;
	}
	
	private BrowserBean getBrowserBean() {
		PageContext context = (PageContext)getJspContext();
		
		return BrowserBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private PagePositionsBean getPagePositionsBean() {
		PageContext context = (PageContext)getJspContext();
		
		return PagePositionsBean.getForRequest((HttpServletRequest)context.getRequest());
	}
	
	private void setField(Object o, Field f, Object value) {
		/*
		if (logger.isDebugEnabled())
			logger.debug("injecting value of type " + (value != null ? value.getClass().getName() : "null")
					+ " into field " + f.getName() + " of object " + o.getClass().getName());
		*/
		
		try {
			// Like EJB3, we support private-field injection
			f.setAccessible(true);
			f.set(o, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	private Object instantiateObject() {
		//logger.debug("Instantiating " + clazz.getName());
		
		// We special-case the SigninBean and BrowserBean
		if (clazz == SigninBean.class) 
			return getSigninBean(false);
		if (clazz == BrowserBean.class)
			return getBrowserBean();
		
		Object o;
		try {
			o = clazz.newInstance();
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		}
		
		for (Class<?> c = clazz; c.getPackage() == clazz.getPackage(); c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.isAnnotationPresent(Signin.class)) {
					// if the field is UserSigninBean or subclass, then
					// we require valid signin; if the field is SigninBean or 
					// superclass, then we allow AnonymousSigninBean also
					if (UserSigninBean.class.isAssignableFrom(f.getType())) {
						setField(o, f, getSigninBean(true));
					} else if (f.getType().isAssignableFrom(SigninBean.class)) {
						setField(o, f, getSigninBean(false));
					}
				} else if (f.isAnnotationPresent(Browser.class) &&
					f.getType().isAssignableFrom(BrowserBean.class)) {
					setField(o, f, getBrowserBean());
				} else if (f.isAnnotationPresent(PagePositions.class) &&
						f.getType().isAssignableFrom(PagePositionsBean.class)) {
						setField(o, f, getPagePositionsBean());
				} else if (f.isAnnotationPresent(FromJspContext.class)) {
					FromJspContext a = f.getAnnotation(FromJspContext.class);
					String key = a.value();
					Scope s = a.scope();
					if (s == null)
						s = scope; // default to scope of the page
					
					Object toInject = findInScope(s, key);
					if (toInject != null)
						setField(o, f, toInject);
					else
						logger.warn("no value {} found in scope {} not injecting into " + o.getClass().getName(),
								key, s);
				}
			}
		}
		
		return o;
	}
	
	@Override
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
