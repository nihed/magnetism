<%--
  -	$Revision: 2925 $
  -	$Date: 2005-10-07 12:26:47 -0400 (Fri, 07 Oct 2005) $
  -
  - Copyright (C) 2004-2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.ParamUtils,
                 java.util.*"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
   // Handle a cancel
    if (request.getParameter("cancel") != null) {
      response.sendRedirect("muc-server-props-edit-form.jsp");
      return;
    }
%>

<%-- Define Administration Bean --%>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    String name = ParamUtils.getParameter(request,"servername");
    String muc = ParamUtils.getParameter(request,"mucname");

    // Handle a save
    Map errors = new HashMap();
    if (save) {
        // Make sure that the MUC Service is lower cased.
        muc = muc.toLowerCase();

        // do validation
        if (muc == null  || muc.indexOf('.') >= 0) {
            errors.put("mucname","mucname");
        }
        if (errors.size() == 0) {
            webManager.getMultiUserChatServer().setServiceName(muc);
            response.sendRedirect("muc-server-props-edit-form.jsp?success=true&mucname="+muc);
            return;
        }
    }
    else if(muc == null) {
        name = webManager.getServerInfo().getName() == null ? "" : webManager.getServerInfo().getName();
        muc = webManager.getMultiUserChatServer().getServiceName() == null  ? "" : webManager.getMultiUserChatServer().getServiceName();
    }

    name = webManager.getServerInfo().getName();
    if (errors.size() == 0 && muc == null) {
        muc = webManager.getMultiUserChatServer().getServiceName();
    }
%>

<html>
    <head>
        <title><fmt:message key="groupchat.service.properties.title"/></title>
        <meta name="pageID" content="muc-server-props"/>
        <meta name="helpPage" content="edit_group_chat_service_properties.html"/>
    </head>
    <body>

<p>
<fmt:message key="groupchat.service.properties.introduction" />
</p>

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
            <fmt:message key="groupchat.service.properties.saved_successfully" /> <b><fmt:message key="global.restart" /></b> <fmt:message key="groupchat.service.properties.saved_successfully2" /> <a href="index.jsp"><fmt:message key="global.server_status" /></a>).
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="groupchat.service.properties.error_service_name" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="muc-server-props-edit-form.jsp" method="post">
<input type="hidden" name="save" value="true">

<fieldset>
    <legend><fmt:message key="groupchat.service.properties.legend" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0">

    <tr>
        <td class="c1">
           <fmt:message key="groupchat.service.properties.label_service_name" />
        </td>
        <td>
        <input type="text" size="30" maxlength="150" name="mucname"  value="<%= (muc != null ? muc : "") %>">

        <%  if (errors.get("mucname") != null) { %>

            <span class="jive-error-text">
            <br><fmt:message key="groupchat.service.properties.error_service_name" />
            </span>

        <%  } %>
        </td>
    </tr>
    </table>
    </div>
</fieldset>

<br><br>

<input type="submit" value="<fmt:message key="groupchat.service.properties.save" />">

</form>

    </body>
</html>