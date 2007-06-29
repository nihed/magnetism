<%--
  -	$Revision: 3195 $
  -	$Date: 2005-12-13 13:07:30 -0500 (Tue, 13 Dec 2005) $
  -
  - Copyright (C) 2004-2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.*,
                 org.jivesoftware.wildfire.user.*,
                 org.jivesoftware.admin.*,
                 org.jivesoftware.wildfire.*,
                 org.xmpp.packet.Presence,
                 java.net.URLEncoder"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    boolean email = request.getParameter("email") != null;
    boolean password = request.getParameter("password") != null;
    String username = ParamUtils.getParameter(request,"username");

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("user-summary.jsp");
        return;
    }

    // Handle a delete
    if (delete) {
        response.sendRedirect("user-delete.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Handle password change
    if (password) {
        response.sendRedirect("user-password.jsp?username=" + URLEncoder.encode(username, "UTF-8"));
        return;
    }

    // Load the user object
    User user = null;
    try {
        user = webManager.getUserManager().getUser(username);
    }
    catch (UserNotFoundException unfe) {
        user = webManager.getUserManager().getUser(username);
    }

    PresenceManager presenceManager = webManager.getPresenceManager();
%>

<html>
    <head>
        <title><fmt:message key="user.properties.title"/></title>
        <meta name="subPageID" content="user-properties"/>
        <meta name="extraParams" content="<%= "username="+URLEncoder.encode(username, "UTF-8") %>"/>
        <meta name="helpPage" content="edit_user_properties.html"/>
    </head>
    <body>

<p>
<fmt:message key="user.properties.info" />
</p>

<%  if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="user.properties.created" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (request.getParameter("editsuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="user.properties.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2">
            <fmt:message key="user.properties.title" />
        </th>
    </tr>
</thead>
<tbody>
    <tr>
        <td class="c1">
            <fmt:message key="user.create.username" />:
        </td>
        <td>
            <%= user.getUsername() %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="session.details.status" />:
        </td>
        <td>
            <%  if (presenceManager.isAvailable(user)) {
                    Presence presence = presenceManager.getPresence(user);
            %>
                <% if (presence.getShow() == null) { %>
                    <img src="images/user-green-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="user.properties.available" />">
                    <fmt:message key="user.properties.available" />
                <% } %>
                <% if (presence.getShow() == Presence.Show.chat) { %>
                    <img src="images/user-green-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="session.details.chat_available" />">
                    <fmt:message key="session.details.chat_available" />
                <% } %>
                <% if (presence.getShow() == Presence.Show.away) { %>
                    <img src="images/user-yellow-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="session.details.away" />">
                    <fmt:message key="session.details.away" />
                <% } %>
                <% if (presence.getShow() == Presence.Show.xa) { %>
                    <img src="images/user-yellow-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="session.details.extended" />">
                    <fmt:message key="session.details.extended" />
                <% } %>
                <% if (presence.getShow() == Presence.Show.dnd) { %>
                    <img src="images/user-red-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="session.details.not_disturb" />">
                    <fmt:message key="session.details.not_disturb" />
                <% } %>

            <%  } else { %>

                <img src="images/user-clear-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="user.properties.offline" />">
                (<fmt:message key="user.properties.offline" />)

            <%  } %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="user.create.name" />:
        </td>
        <td>
            <%  if ("".equals(user.getName())) { %>
                <span style="color:#999">
                <i><fmt:message key="user.properties.not_set" /></i>
                </span>

            <%  } else { %>
                <%= user.getName() %>

            <%  } %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="user.create.email" />:
        </td>
        <td>
            <%  if (user.getEmail() == null) { %>
                <span style="color:#999">
                <i><fmt:message key="user.properties.not_set" /></i>
                </span>

            <%  } else { %>
                <a href="mailto:<%= user.getEmail() %>"><%= user.getEmail() %></a>

            <%  } %>
            &nbsp;
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="user.properties.registered" />:
        </td>
        <td>
            <%= JiveGlobals.formatDate(user.getCreationDate()) %>
        </td>
    </tr>
</tbody>
</table>
</div>

<br><br>

<form action="user-edit-form.jsp">
<input type="hidden" name="username" value="<%= user.getUsername() %>">
<input type="submit" value="<fmt:message key="global.edit_properties" />">
</form>

</body>
</html>