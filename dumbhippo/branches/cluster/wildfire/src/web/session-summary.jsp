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
                 java.util.*,
                 org.jivesoftware.wildfire.*,
                 java.util.Date,
                 org.xmpp.packet.JID"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%!
    static final String NONE = LocaleUtils.getLocalizedString("global.none");

    final int DEFAULT_RANGE = 15;
    final int[] RANGE_PRESETS = {15, 25, 50, 75, 100};

    static final int[] REFRESHES = {0, 10, 30, 60, 90};
    static final String[] REFRESHES_LABELS = {NONE,"10","30","60","90"};
%>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("session-summary", DEFAULT_RANGE));
    int refresh = ParamUtils.getIntParameter(request,"refresh",webManager.getRefreshValue("session-summary", 0));
    boolean close = ParamUtils.getBooleanParameter(request,"close");
    String jid = ParamUtils.getParameter(request,"jid");

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("session-summary", range);
    }

    if (request.getParameter("refresh") != null) {
        webManager.setRefreshValue("session-summary", refresh);
    }

    // Get the user manager
    SessionManager sessionManager = webManager.getSessionManager();

    // Get the session count
    int sessionCount = sessionManager.getSessionCount();

    // Close a connection if requested
    if (close) {
        JID address = new JID(jid);
        try {
            Session sess = sessionManager.getSession(address);
            sess.getConnection().close();
            // wait one second
            Thread.sleep(1000L);
        }
        catch (Exception ignored) {
            // Session might have disappeared on its own
        }
        // Redirect back to this page
        response.sendRedirect("session-summary.jsp?close=success");
        return;
    }

    // paginator vars
    int numPages = (int)Math.ceil((double)sessionCount/(double)range);
    int curPage = (start/range) + 1;
%>

<html>
    <head>
        <title><fmt:message key="session.summary.title"/></title>
        <meta name="pageID" content="session-summary"/>
        <meta name="helpPage" content="view_active_client_sessions.html"/>
    </head>
    <body>

<%  if ("success".equals(request.getParameter("close"))) { %>

    <p class="jive-success-text">
    <fmt:message key="session.summary.close" />
    </p>

<%  } %>

<%  if (refresh > 0) { %>
    <meta http-equiv="refresh" content="<%= refresh %>">
<%  } %>

<table cellpadding="0" cellspacing="0" border="0" width="100%">
<tbody>
<form action="session-summary.jsp" method="get">
    <tr valign="top">
        <td width="99%">
            <fmt:message key="session.summary.active" />: <b><%= sessionCount %></b>

            <%  if (numPages > 1) { %>

                - <fmt:message key="global.showing" /> <%= (start+1) %>-<%= (start+range) %>

            <%  } %>

            <%  if (numPages > 1) { %>

                <p>
                <fmt:message key="global.pages" />:
                [
                <%  for (int i=0; i<numPages; i++) {
                        String sep = ((i+1)<numPages) ? " " : "";
                        boolean isCurrent = (i+1) == curPage;
                %>
                    <a href="session-summary.jsp?start=<%= (i*range) %>"
                     class="<%= ((isCurrent) ? "jive-current" : "") %>"
                     ><%= (i+1) %></a><%= sep %>

                <%  } %>
                ]

            <%  } %>
            - <fmt:message key="session.summary.sessions_per_page" />:
            <select size="1" name="range" onchange="this.form.submit();">

                <%  for (int i=0; i<RANGE_PRESETS.length; i++) { %>

                    <option value="<%= RANGE_PRESETS[i] %>"<%= (RANGE_PRESETS[i] == range ? "selected" : "") %>><%= RANGE_PRESETS[i] %></option>

                <%  } %>

            </select>
        </td>
        <td width="1%" nowrap>
            <fmt:message key="global.refresh" />:
            <select size="1" name="refresh" onchange="this.form.submit();">
            <%  for (int j=0; j<REFRESHES.length; j++) {
                    String selected = REFRESHES[j] == refresh ? " selected" : "";
            %>
                <option value="<%= REFRESHES[j] %>"<%= selected %>><%= REFRESHES_LABELS[j] %>

            <%  } %>
            </select>
            (<fmt:message key="global.seconds" />)

        </td>
    </tr>
</form>
</tbody>
</table>
<br>
<p>
<fmt:message key="session.summary.info" />
</p>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="session.details.name" /></th>
        <th nowrap><fmt:message key="session.details.resource" /></th>
        <th nowrap colspan="2"><fmt:message key="session.details.status" /></th>
        <th nowrap colspan="2"><fmt:message key="session.details.presence" /></th>
        <th nowrap><fmt:message key="session.details.clientip" /></th>
        <th nowrap><fmt:message key="session.details.close_connect" /></th>
    </tr>
</thead>
<tbody>
    <%  // Get the iterator of sessions, print out session info if any exist.
        SessionResultFilter filter = new SessionResultFilter();
        filter.setStartIndex(start);
        filter.setNumResults(range);
        Collection<ClientSession> sessions = sessionManager.getSessions(filter);
        if (sessions.isEmpty()) {
    %>
        <tr>
            <td colspan="9">

                <fmt:message key="session.summary.not_session" />

            </td>
        </tr>

    <%  } %>

    <%  int count = start;
        boolean current = false; // needed in session-row.jspf
        String linkURL = "session-details.jsp";
        for (ClientSession sess : sessions) {
            count++;
    %>
        <%@ include file="session-row.jspf" %>
    <%  } %>

</tbody>
</table>
</div>

<%  if (numPages > 1) { %>

    <p>
    <fmt:message key="global.pages" />:
    [
    <%  for (int i=0; i<numPages; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="session-summary.jsp?start=<%= (i*range) %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>
    ]
    </p>

<%  } %>

<br>
<p>
<fmt:message key="session.summary.last_update" />: <%= JiveGlobals.formatDateTime(new Date()) %>
</p>

    </body>
</html>