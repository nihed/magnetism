/**
 * $RCSfile$
 * $Revision: 750 $
 * $Date: 2004-12-26 21:54:13 -0500 (Sun, 26 Dec 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

public abstract class WebBean {

    public HttpSession session;
    public HttpServletRequest request;
    public HttpServletResponse response;
    public ServletContext application;
    public JspWriter out;

    public void init(HttpServletRequest request, HttpServletResponse response,
            HttpSession session, ServletContext app, JspWriter out)
    {
        this.request = request;
        this.response = response;
        this.session = session;
        this.application = app;
        this.out = out;
    }

    public void init(HttpServletRequest request, HttpServletResponse response,
                     HttpSession session, ServletContext app) {

        this.request = request;
        this.response = response;
        this.session = session;
        this.application = app;
    }

    public void init(PageContext pageContext){
        this.request = (HttpServletRequest)pageContext.getRequest();
        this.response = (HttpServletResponse)pageContext.getResponse();
        this.session = (HttpSession)pageContext.getSession();
        this.application = (ServletContext)pageContext.getServletContext();
        this.out = (JspWriter)pageContext.getOut();
    }
}