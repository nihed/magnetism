<%--
  - Copyright (C) 2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="java.util.*,
				 org.jivesoftware.util.*,
                 org.jivesoftware.admin.*"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%-- Define Administration Bean --%>
<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<% admin.init(request, response, session, application, out ); %>

<%	
    // get parameters
    String host = ParamUtils.getParameter(request,"host");
    int port = ParamUtils.getIntParameter(request,"port",0);
    String username = ParamUtils.getParameter(request,"server_username");
    String password = ParamUtils.getParameter(request,"server_password");
    boolean ssl = ParamUtils.getBooleanParameter(request,"ssl");
    boolean save = request.getParameter("save") != null;
    boolean test = request.getParameter("test") != null;
    boolean success = ParamUtils.getBooleanParameter(request,"success");
    boolean debug = ParamUtils.getBooleanParameter(request, "debug");

    // Handle a test request
    if (test) {
        response.sendRedirect("system-emailtest.jsp");
        return;
    }

    EmailService service = EmailService.getInstance();
    // Save the email settings if requested
    Map errors = new HashMap();
    if (save) {
        if (host != null) {
            service.setHost(host);
        }
        else {
            errors.put("host","");
        }
        if (port > 0) {
            service.setPort(port);
        }
        else {
            // Default to port 25.
            service.setPort(25);
        }
        service.setUsername(username);
        service.setPassword(password);
        service.setDebugEnabled(debug);
        service.setSSLEnabled(ssl);

        if (errors.size() == 0) {
            // Set property to specify email is configured
            JiveGlobals.setProperty("mail.configured", "true");
            response.sendRedirect("system-email.jsp?success=true");
        }
    }

    host = service.getHost();
    port = service.getPort();
    username = service.getUsername();
    password = service.getPassword();
    ssl = service.isSSLEnabled();
    debug = service.isDebugEnabled();
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("system.email.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb("Main", "main.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title, "system-email.jsp"));
    pageinfo.setPageID("system-email");
%>

<jsp:include page="top.jsp" flush="true" />
<jsp:include page="title.jsp" flush="true" />

<p>
<fmt:message key="system.email.info" />
</p>

<%  if ("true".equals(request.getParameter("success"))) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        	<td class="jive-icon-label"><fmt:message key="system.email.update_success" /></td>
        </tr>
    </tbody>
    </table>
    </div>

<%  } %>

<%  if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
        	<td class="jive-icon-label"><fmt:message key="system.email.update_failure" /></td>
        </tr>
    </tbody>
    </table>
    </div>

<%	} %>

<p>

<form action="system-email.jsp" name="f" method="post">

<fieldset>
    <legend><fmt:message key="system.email.name" /></legend>
    <div>

    <table cellpadding="3" cellspacing="0" border="0">
    <tr>
        <td width="1%" nowrap>
            <fmt:message key="system.email.mail_host" />:
        </td>
        <td width="1%" nowrap>
            <input type="text" name="host" value="<%= (host != null)?host:"" %>" size="40" maxlength="150">
        </td>
    </tr>

    <%  if (errors.containsKey("host")) { %>

        <tr>
            <td width="1%" nowrap>
                &nbsp;
            </td>
            <td width="1%" nowrap class="jive-error-text">
                <fmt:message key="system.email.valid_host_name" />
            </td>
        </tr>

    <%  } %>

    <tr>
        <td width="1%" nowrap>
        	<fmt:message key="system.email.server_port" />:            
        </td>
        <td width="1%" nowrap>
            <input type="text" name="port" value="<%= (port > 0) ? String.valueOf(port) : "" %>" size="10" maxlength="15">
        </td>
    </tr>
    <tr>
        <td width="1%" nowrap>
        	<fmt:message key="system.email.mail_debugging" />:            
        </td>
        <td width="1%" nowrap>
            <input type="radio" name="debug" value="true"<%= (debug ? " checked" : "") %> id="rb01"> <label for="rb01">On</label>
            &nbsp;
            <input type="radio" name="debug" value="false"<%= (debug ? "" : " checked") %> id="rb02"> <label for="rb02">Off</label>
            &nbsp; (<fmt:message key="system.email.restart_possible" />)
        </td>
    </tr>

    <%-- spacer --%>
    <tr><td colspan="2">&nbsp;</td></tr>

    <tr>
        <td width="1%" nowrap>
        	<fmt:message key="system.email.server_username" />:            
        </td>
        <td width="1%" nowrap>
            <input type="text" name="server_username" value="<%= (username != null) ? username : "" %>" size="40" maxlength="150">
        </td>
    </tr>
    <tr>
        <td width="1%" nowrap>
        	<fmt:message key="system.email.server_password" />:            
        </td>
        <td width="1%" nowrap>
            <input type="password" name="server_password" value="<%= (password != null) ? password : "" %>" size="40" maxlength="150">
        </td>
    </tr>

    <tr>
        <td width="1%" nowrap>
        	<fmt:message key="system.email.ssl" />: 
        </td>
        <td width="1%" nowrap>
            <input type="checkbox" name="ssl"<%= (ssl) ? " checked" : "" %>>
        </td>
    </tr>

    </table>
    </div>
</fieldset>

<br>

<input type="submit" name="save" value="<fmt:message key="system.email.save" />">
<input type="submit" name="test" value="<fmt:message key="system.email.send_test" />">

</form>

<jsp:include page="bottom.jsp" flush="true" />