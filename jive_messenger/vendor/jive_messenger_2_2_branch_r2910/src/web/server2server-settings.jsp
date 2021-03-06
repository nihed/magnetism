<%--
  -	$RCSfile$
  -	$Revision: 1607 $
  -	$Date: 2005-07-07 14:27:11 -0400 (Thu, 07 Jul 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%@ page import="org.jivesoftware.util.*,
                 java.util.Iterator,
                 org.jivesoftware.messenger.*,
                 java.util.*,
                 java.text.DateFormat,
                 org.jivesoftware.admin.AdminPageBean,
                 org.jivesoftware.messenger.server.RemoteServerManager,
                 org.jivesoftware.messenger.server.RemoteServerConfiguration,
                 org.jivesoftware.messenger.component.ExternalComponentManager"
    errorPage="error.jsp"
%>

<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<% admin.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean update = request.getParameter("update") != null;
    boolean s2sEnabled = ParamUtils.getBooleanParameter(request,"s2sEnabled");
    int port = ParamUtils.getIntParameter(request,"port", 0);
    boolean closeEnabled = ParamUtils.getBooleanParameter(request,"closeEnabled");
    String idletime = ParamUtils.getParameter(request,"idletime");
    boolean closeSettings = request.getParameter("closeSettings") != null;
    boolean closeSettingsSuccess = request.getParameter("closeSettingsSuccess") != null;
    boolean permissionUpdate = request.getParameter("permissionUpdate") != null;
    String permissionFilter = ParamUtils.getParameter(request,"permissionFilter");
    String configToDelete = ParamUtils.getParameter(request,"deleteConf");
    boolean serverAllowed = request.getParameter("serverAllowed") != null;
    boolean serverBlocked = request.getParameter("serverBlocked") != null;
    String domain = ParamUtils.getParameter(request,"domain");
    String remotePort = ParamUtils.getParameter(request,"remotePort");
    boolean updateSucess = false;
    boolean allowSuccess = false;
    boolean blockSuccess = false;
    boolean deleteSuccess = false;

    // Get muc server
    SessionManager sessionManager = admin.getSessionManager();
    ConnectionManager connectionManager = XMPPServer.getInstance().getConnectionManager();

    Map errors = new HashMap();
    if (update) {
        // Validate params
        if (s2sEnabled) {
            if (port <= 0) {
                errors.put("port","");
            }
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            if (!s2sEnabled) {
                connectionManager.enableServerListener(false);
            }
            else {
                connectionManager.enableServerListener(true);
                connectionManager.setServerListenerPort(port);
            }
            updateSucess = true;
        }
    }

    // Handle an update of the kicking task settings
    if (closeSettings) {
       if (!closeEnabled) {
           // Disable kicking users by setting a value of -1
           sessionManager.setServerSessionIdleTime(-1);
           response.sendRedirect("server2server-settings.jsp?closeSettingsSuccess=true");
           return;
       }
       // do validation
       if (idletime == null) {
           errors.put("idletime","idletime");
       }
       int idle = 0;
       // Try to obtain an int from the provided strings
       if (errors.size() == 0) {
           try {
               idle = Integer.parseInt(idletime) * 1000 * 60;
           }
           catch (NumberFormatException e) {
               errors.put("idletime","idletime");
           }
           if (idle < 0) {
               errors.put("idletime","idletime");
           }
       }

       if (errors.size() == 0) {
           sessionManager.setServerSessionIdleTime(idle);
           response.sendRedirect("server2server-settings.jsp?closeSettingsSuccess=true");
           return;
       }
    }

    if (permissionUpdate) {
        RemoteServerManager.setPermissionPolicy(permissionFilter);
        updateSucess = true;
    }

    if (configToDelete != null && configToDelete.trim().length() != 0) {
        RemoteServerManager.deleteConfiguration(configToDelete);
        deleteSuccess = true;
    }

    if (serverAllowed) {
        int intRemotePort = 0;
        // Validate params
        if (domain == null || domain.trim().length() == 0) {
            errors.put("domain","");
        }
        if (remotePort == null || remotePort.trim().length() == 0 ||  "0".equals(remotePort)) {
            errors.put("remotePort","");
        }
        else {
            try {
                intRemotePort = Integer.parseInt(remotePort);
            }
            catch (NumberFormatException e) {
                errors.put("remotePort","");
            }
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            RemoteServerConfiguration configuration = new RemoteServerConfiguration(domain);
            configuration.setRemotePort(intRemotePort);
            configuration.setPermission(RemoteServerConfiguration.Permission.allowed);
            RemoteServerManager.allowAccess(configuration);
            allowSuccess = true;
        }
    }

    if (serverBlocked) {
        // Validate params
        if (domain == null || domain.trim().length() == 0) {
            errors.put("domain","");
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            RemoteServerManager.blockAccess(domain);
            blockSuccess = true;
        }
    }

    // Set page vars
    if (errors.size() == 0) {
        s2sEnabled = connectionManager.isServerListenerEnabled();
        port = connectionManager.getServerListenerPort();
        permissionFilter = RemoteServerManager.getPermissionPolicy().toString();
        domain = "";
        remotePort = "0";
    }
    else {
        if (port == 0) {
            port = connectionManager.getServerListenerPort();
        }
        if (permissionFilter == null) {
            permissionFilter = RemoteServerManager.getPermissionPolicy().toString();
        }
        if (domain == null) {
            domain = "";
        }
        if (remotePort == null) {
            remotePort = "0";
        }
    }
%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%  // Title of this page and breadcrumbs
    String title = LocaleUtils.getLocalizedString("server2server.settings.title");
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(LocaleUtils.getLocalizedString("global.main"), "index.jsp"));
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title, "server2server-settings.jsp"));
    pageinfo.setPageID("server2server-settings");
%>

<jsp:include page="top.jsp" flush="true" />
<jsp:include page="title.jsp" flush="true" />

<p>
<fmt:message key="server2server.settings.info">
    <fmt:param value="<%= "<a href='server-session-summary.jsp'>" %>" />
    <fmt:param value="<%= "</a>" %>" />
</fmt:message>
</p>

<%  if (!errors.isEmpty()) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
            <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"/></td>
            <td class="jive-icon-label">

            <% if (errors.get("idletime") != null) { %>
                <fmt:message key="server2server.settings.valid.idle_minutes" />
            <% } else if (errors.get("domain") != null) { %>
                <fmt:message key="server2server.settings.valid.domain" />
            <% } else if (errors.get("remotePort") != null) { %>
                <fmt:message key="server2server.settings.valid.remotePort" />
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (closeSettingsSuccess || updateSucess || allowSuccess || blockSuccess || deleteSuccess) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <% if (updateSucess) { %>
            <fmt:message key="server2server.settings.confirm.updated" />
        <% } else if (allowSuccess) { %>
            <fmt:message key="server2server.settings.confirm.allowed" />
        <% } else if (blockSuccess) { %>
            <fmt:message key="server2server.settings.confirm.blocked" />
        <% } else if (deleteSuccess) { %>
            <fmt:message key="server2server.settings.confirm.deleted" />
        <% } else if (closeSettingsSuccess) { %>
            <fmt:message key="server2server.settings.update" />
        <%
            }
        %>
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="server2server-settings.jsp" method="post">

<fieldset>
    <legend><fmt:message key="server2server.settings.enabled.legend" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="s2sEnabled" value="false" id="rb01"
                 <%= (!s2sEnabled ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb01">
                <b><fmt:message key="server2server.settings.label_disable" /></b> - <fmt:message key="server2server.settings.label_disable_info" />
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="s2sEnabled" value="true" id="rb02"
                 <%= (s2sEnabled ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb02">
                <b><fmt:message key="server2server.settings.label_enable" /></b> - <fmt:message key="server2server.settings.label_enable_info" />
                </label>  <input type="text" size="5" maxlength="10" name="port" value="<%= port %>">
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>
<br>

<input type="submit" name="update" value="<fmt:message key="global.save_settings" />">

</form>

<br>

<form action="server2server-settings.jsp?closeSettings" method="post">

<fieldset>
    <legend><fmt:message key="server2server.settings.close_settings" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="closeEnabled" value="false" id="rb03"
                 <%= ((admin.getSessionManager().getServerSessionIdleTime() < 0) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb03"><fmt:message key="server2server.settings.never_close" /></label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="closeEnabled" value="true" id="rb04"
                 <%= ((admin.getSessionManager().getServerSessionIdleTime() > -1) ? "checked" : "") %>>
            </td>
            <td width="99%">
                    <label for="rb04"><fmt:message key="server2server.settings.close_session" /></label>
                     <input type="text" name="idletime" size="5" maxlength="5"
                         onclick="this.form.closeEnabled[1].checked=true;"
                         value="<%= admin.getSessionManager().getServerSessionIdleTime() == -1 ? 30 : admin.getSessionManager().getServerSessionIdleTime() / 1000 / 60 %>">
                     <fmt:message key="global.minutes" />.
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br>

<input type="submit" value="<fmt:message key="global.save_settings" />">

</form>

<br>

<fieldset>
    <legend><fmt:message key="server2server.settings.allowed" /></legend>
    <div>
    <form action="server2server-settings.jsp" method="post">
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>

        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="permissionFilter" value="<%= RemoteServerManager.PermissionPolicy.blacklist %>" id="rb05"
                 <%= (RemoteServerManager.PermissionPolicy.blacklist.toString().equals(permissionFilter) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb05">
                <b><fmt:message key="server2server.settings.anyone" /></b> - <fmt:message key="server2server.settings.anyone_info" />
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="permissionFilter" value="<%= RemoteServerManager.PermissionPolicy.whitelist %>" id="rb06"
                 <%= (RemoteServerManager.PermissionPolicy.whitelist.toString().equals(permissionFilter) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb06">
                <b><fmt:message key="server2server.settings.whitelist" /></b> - <fmt:message key="server2server.settings.whitelist_info" />
                </label>
            </td>
        </tr>
    </tbody>
    </table>
    <br>
    <input type="submit" name="permissionUpdate" value="<fmt:message key="global.save_settings" />">
    </form>

    <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
    <thead>
        <tr>
            <th width="1%">&nbsp;</th>
            <th width="50%" nowrap><fmt:message key="server2server.settings.domain" /></th>
            <th width="49%" nowrap><fmt:message key="server2server.settings.remotePort" /></th>
            <th width="10%" nowrap><fmt:message key="global.delete" /></th>
        </tr>
    </thead>
    <tbody>
    <% Collection<RemoteServerConfiguration> configs = RemoteServerManager.getAllowedServers();
       if (configs.isEmpty()) { %>
        <tr>
            <td align="center" colspan="7"><fmt:message key="server2server.settings.empty_list" /></td>
        </tr>
       <% }
        else {
        int count = 1;
        for (Iterator<RemoteServerConfiguration> it=configs.iterator(); it.hasNext(); count++) {
            RemoteServerConfiguration configuration = it.next();
       %>
	    <tr class="jive-<%= (((count%2)==0) ? "even" : "odd") %>">
	        <td>
	            <%= count %>
	        </td>
	        <td>
	            <%= configuration.getDomain() %>
	        </td>
	        <td>
	            <%= configuration.getRemotePort() %>
	        </td>
	        <td align="center" style="border-right:1px #ccc solid;">
	            <a href="#" onclick="if (confirm('<fmt:message key="server2server.settings.confirm_delete" />')) { location.replace('server2server-settings.jsp?deleteConf=<%= configuration.getDomain() %>'); } "
	             title="<fmt:message key="global.click_delete" />"
	             ><img src="images/delete-16x16.gif" width="16" height="16" border="0"></a>
	        </td>
	    </tr>
       <% }
       }
    %>
    </tbody>
    </table>
    <br>
    <table cellpadding="3" cellspacing="1" border="0" width="100%">
    <form action="server2server-settings.jsp" method="post">
    <tr>
        <td nowrap>
            <fmt:message key="server2server.settings.domain" />
            <input type="text" size="40" name="domain" value="<%= serverAllowed ?  domain : "" %>"/>
            &nbsp;
            <fmt:message key="server2server.settings.remotePort" />
            <input type="text" size="5" name="remotePort"value="<%= serverAllowed ?  remotePort : "5269" %>"/>
            <input type="submit" name="serverAllowed" value="<fmt:message key="server2server.settings.allow" />">
        </td>
    </tr>
    </form>
    </table>

    </div>
</fieldset>

<br><br>

<fieldset>
    <legend><fmt:message key="server2server.settings.disallowed" /></legend>
    <div>
    <table cellpadding="3" cellspacing="1" border="0" width="100%"><tr><td>
    <fmt:message key="server2server.settings.disallowed.info" />
    </td></tr></table>
    <p>
    <table class="jive-table" cellpadding="3" cellspacing="0" border="0" width="100%">
    <thead>
        <tr>
            <th width="1%">&nbsp;</th>
            <th width="89%" nowrap><fmt:message key="server2server.settings.domain" /></th>
            <th width="10%" nowrap><fmt:message key="global.delete" /></th>
        </tr>
    </thead>
    <tbody>
    <% Collection<RemoteServerConfiguration> blockedComponents = RemoteServerManager.getBlockedServers();
       if (blockedComponents.isEmpty()) { %>
        <tr>
            <td align="center" colspan="7"><fmt:message key="server2server.settings.empty_list" /></td>
        </tr>
       <% }
        else {
        int count = 1;
        for (Iterator<RemoteServerConfiguration> it=blockedComponents.iterator(); it.hasNext(); count++) {
            RemoteServerConfiguration configuration = it.next();
       %>
	    <tr class="jive-<%= (((count%2)==0) ? "even" : "odd") %>">
	        <td>
	            <%= count %>
	        </td>
	        <td>
	            <%= configuration.getDomain() %>
	        </td>
	        <td align="center" style="border-right:1px #ccc solid;">
	            <a href="#" onclick="if (confirm('<fmt:message key="server2server.settings.confirm_delete" />')) { location.replace('server2server-settings.jsp?deleteConf=<%= configuration.getDomain() %>'); } "
	             title="<fmt:message key="global.click_delete" />"
	             ><img src="images/delete-16x16.gif" width="16" height="16" border="0"></a>
	        </td>
	    </tr>
       <% }
       }
    %>
    </tbody>
    </table>
    <br>
    <table cellpadding="3" cellspacing="1" border="0" width="100%">
    <form action="server2server-settings.jsp" method="post">
    <tr>
        <td nowrap width="1%">
            <fmt:message key="server2server.settings.domain" />
        </td>
        <td>
            <input type="text" size="40" name="domain" value="<%= serverBlocked ?  domain : "" %>"/>&nbsp;
            <input type="submit" name="serverBlocked" value="<fmt:message key="server2server.settings.block" />">
        </td>
    </tr>
    </form>
    </table>
    </div>
</fieldset>

<jsp:include page="bottom.jsp" flush="true" />
