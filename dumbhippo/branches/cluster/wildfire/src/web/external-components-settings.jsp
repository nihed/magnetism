<%--
  -	$RCSfile$
  -	$Revision: 3195 $
  -	$Date: 2005-12-13 13:07:30 -0500 (Tue, 13 Dec 2005) $
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
                 org.jivesoftware.wildfire.*,
                 java.util.*,
                 org.jivesoftware.wildfire.component.ExternalComponentManager,
                 org.jivesoftware.wildfire.component.ExternalComponentConfiguration"
    errorPage="error.jsp"
%>

<html>
    <head>
        <title><fmt:message key="component.settings.title"/></title>
        <meta name="pageID" content="external-components-settings"/>
    </head>
    <body>

<%  // Get parameters
    boolean update = request.getParameter("update") != null;
    boolean permissionUpdate = request.getParameter("permissionUpdate") != null;
    boolean componentEnabled = ParamUtils.getBooleanParameter(request,"componentEnabled");
    int port = ParamUtils.getIntParameter(request,"port", 0);
    String defaultSecret = ParamUtils.getParameter(request,"defaultSecret");
    String permissionFilter = ParamUtils.getParameter(request,"permissionFilter");
    String configToDelete = ParamUtils.getParameter(request,"deleteConf");
    boolean componentAllowed = request.getParameter("componentAllowed") != null;
    boolean componentBlocked = request.getParameter("componentBlocked") != null;
    String subdomain = ParamUtils.getParameter(request,"subdomain");
    String secret = ParamUtils.getParameter(request,"secret");
    boolean updateSucess = false;
    boolean allowSuccess = false;
    boolean blockSuccess = false;
    boolean deleteSuccess = false;

    String serverName = XMPPServer.getInstance().getServerInfo().getName();
    ConnectionManager connectionManager = XMPPServer.getInstance().getConnectionManager();


    // Update the session kick policy if requested
    Map<String, String> errors = new HashMap<String, String>();
    if (update) {
        // Validate params
        if (componentEnabled) {
            if (defaultSecret == null || defaultSecret.trim().length() == 0) {
                errors.put("defaultSecret","");
            }
            if (port <= 0) {
                errors.put("port","");
            }
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            if (!componentEnabled) {
                connectionManager.enableComponentListener(false);
            }
            else {
                connectionManager.enableComponentListener(true);
                connectionManager.setComponentListenerPort(port);
                ExternalComponentManager.setDefaultSecret(defaultSecret);
            }
            updateSucess = true;
        }
    }

    if (permissionUpdate) {
        ExternalComponentManager.setPermissionPolicy(permissionFilter);
        updateSucess = true;
    }

    if (configToDelete != null && configToDelete.trim().length() != 0) {
        ExternalComponentManager.deleteConfiguration(configToDelete);
        deleteSuccess = true;
    }

    if (componentAllowed) {
        // Validate params
        if (subdomain == null || subdomain.trim().length() == 0) {
            errors.put("subdomain","");
        }
        if (secret == null || secret.trim().length() == 0) {
            errors.put("secret","");
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            // Remove the hostname if the user is not sending just the subdomain
            subdomain = subdomain.replace("." + serverName, "");
            ExternalComponentConfiguration configuration = new ExternalComponentConfiguration(subdomain);
            configuration.setSecret(secret);
            configuration.setPermission(ExternalComponentConfiguration.Permission.allowed);
            ExternalComponentManager.allowAccess(configuration);
            allowSuccess = true;
        }
    }

    if (componentBlocked) {
        // Validate params
        if (subdomain == null || subdomain.trim().length() == 0) {
            errors.put("subdomain","");
        }
        // If no errors, continue:
        if (errors.isEmpty()) {
            // Remove the hostname if the user is not sending just the subdomain
            subdomain = subdomain.replace("." + serverName, "");
            ExternalComponentManager.blockAccess(subdomain);
            blockSuccess = true;
        }
    }

    // Set page vars
    if (errors.size() == 0) {
        componentEnabled = connectionManager.isComponentListenerEnabled();
        port = connectionManager.getComponentListenerPort();
        defaultSecret = ExternalComponentManager.getDefaultSecret();
        permissionFilter = ExternalComponentManager.getPermissionPolicy().toString();
        subdomain = "";
        secret = "";
    }
    else {
        if (port == 0) {
            port = connectionManager.getComponentListenerPort();
        }
        if (defaultSecret == null) {
            defaultSecret = ExternalComponentManager.getDefaultSecret();
        }
        if (permissionFilter == null) {
            permissionFilter = ExternalComponentManager.getPermissionPolicy().toString();
        }
        if (subdomain == null) {
            subdomain = "";
        }
        if (secret == null) {
            secret = "";
        }
    }
%>

<p>
<fmt:message key="component.settings.info">
    <fmt:param value="<%= "<a href='component-session-summary.jsp'>" %>" />
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

            <% if (errors.get("port") != null) { %>
                <fmt:message key="component.settings.valid.port" />
            <% } else if (errors.get("defaultSecret") != null) { %>
                <fmt:message key="component.settings.valid.defaultSecret" />
            <% } else if (errors.get("subdomain") != null) { %>
                <fmt:message key="component.settings.valid.subdomain" />
            <% } else if (errors.get("secret") != null) { %>
                <fmt:message key="component.settings.valid.secret" />
            <% } %>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
    <br>

<%  } else if (updateSucess || allowSuccess || blockSuccess || deleteSuccess) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <% if (updateSucess) { %>
            <fmt:message key="component.settings.confirm.updated" />
        <% } else if (allowSuccess) { %>
            <fmt:message key="component.settings.confirm.allowed" />
        <% } else if (blockSuccess) { %>
            <fmt:message key="component.settings.confirm.blocked" />
        <% } else if (deleteSuccess) { %>
            <fmt:message key="component.settings.confirm.deleted" />
        <%
            }
        %>
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<form action="external-components-settings.jsp" method="post">

<fieldset>
    <legend><fmt:message key="component.settings.enabled.legend" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="componentEnabled" value="false" id="rb01"
                 <%= (!componentEnabled ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb01">
                <b><fmt:message key="component.settings.label_disable" /></b> - <fmt:message key="component.settings.label_disable_info" />
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="componentEnabled" value="true" id="rb02"
                 <%= (componentEnabled ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb02">
                <b><fmt:message key="component.settings.label_enable" /></b> - <fmt:message key="component.settings.label_enable_info" />
                </label>
            </td>
        </tr>
        <tr valign="top">
            <td width="1%" nowrap>
                &nbsp;
            </td>
            <td width="99%">
                <table cellpadding="3" cellspacing="0" border="0" width="100%">
                <tr valign="top">
                    <td width="1%" nowrap class="c1">
                        <fmt:message key="component.settings.port" />
                    </td>
                    <td width="99%">
                        <input type="text" size="10" maxlength="50" name="port"
                         value="<%= port %>">
                    </td>
                </tr>
                <tr valign="top">
                    <td width="1%" nowrap class="c1">
                        <fmt:message key="component.settings.defaultSecret" />
                    </td>
                    <td width="99%">
                        <input type="text" size="15" maxlength="70" name="defaultSecret"
                         value="<%= ((defaultSecret != null) ? defaultSecret : "") %>">
                    </td>
                </tr>
                </table>
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

<fieldset>
    <legend><fmt:message key="component.settings.allowed" /></legend>
    <div>
    <form action="external-components-settings.jsp" method="post">
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>

        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="permissionFilter" value="<%= ExternalComponentManager.PermissionPolicy.blacklist %>" id="rb03"
                 <%= (ExternalComponentManager.PermissionPolicy.blacklist.toString().equals(permissionFilter) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb03">
                <b><fmt:message key="component.settings.anyone" /></b> - <fmt:message key="component.settings.anyone_info" />
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input type="radio" name="permissionFilter" value="<%= ExternalComponentManager.PermissionPolicy.whitelist %>" id="rb04"
                 <%= (ExternalComponentManager.PermissionPolicy.whitelist.toString().equals(permissionFilter) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb04">
                <b><fmt:message key="component.settings.whitelist" /></b> - <fmt:message key="component.settings.whitelist_info" />
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
            <th width="50%" nowrap><fmt:message key="component.settings.subdomain" /></th>
            <th width="49%" nowrap><fmt:message key="component.settings.secret" /></th>
            <th width="10%" nowrap><fmt:message key="global.delete" /></th>
        </tr>
    </thead>
    <tbody>
    <% Collection<ExternalComponentConfiguration> configs = ExternalComponentManager.getAllowedComponents();
       if (configs.isEmpty()) { %>
        <tr>
            <td align="center" colspan="7"><fmt:message key="component.settings.empty_list" /></td>
        </tr>
       <% }
        else {
        int count = 1;
        for (Iterator<ExternalComponentConfiguration> it=configs.iterator(); it.hasNext(); count++) {
            ExternalComponentConfiguration configuration = it.next();
       %>
	    <tr class="jive-<%= (((count%2)==0) ? "even" : "odd") %>">
	        <td>
	            <%= count %>
	        </td>
	        <td>
	            <%= configuration.getSubdomain() %>
	        </td>
	        <td>
	            <%= configuration.getSecret() %>
	        </td>
	        <td align="center" style="border-right:1px #ccc solid;">
	            <a href="#" onclick="if (confirm('<fmt:message key="component.settings.confirm_delete" />')) { location.replace('external-components-settings.jsp?deleteConf=<%= configuration.getSubdomain() %>'); } "
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
    <form action="external-components-settings.jsp" method="post">
    <tr>
        <td nowrap width="1%">
            <fmt:message key="component.settings.subdomain" />
        </td>
        <td>
            <input type="text" size="40" name="subdomain" value="<%= componentAllowed ?  subdomain : "" %>"/>
        </td>
        <td nowrap width="1%">
            <fmt:message key="component.settings.secret" />
        </td>
        <td>
            <input type="text" size="15" name="secret"value="<%= componentAllowed ?  secret : "" %>"/>
        </td>
    </tr>
    <tr align="center">
        <td colspan="4">
            <input type="submit" name="componentAllowed" value="<fmt:message key="component.settings.allow" />">
        </td>
    </tr>
    </form>
    </table>

    </div>
</fieldset>

<br><br>

<fieldset>
    <legend><fmt:message key="component.settings.disallowed" /></legend>
    <div>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr><td><p><fmt:message key="component.settings.disallowed.info" /></p></td></tr>
    </tbody>
    </table>
    <br><br>
    <table class="jive-table" cellpadding="3" cellspacing="0" border="0" width="100%">
    <thead>
        <tr>
            <th width="1%">&nbsp;</th>
            <th width="89%" nowrap><fmt:message key="component.settings.subdomain" /></th>
            <th width="10%" nowrap><fmt:message key="global.delete" /></th>
        </tr>
    </thead>
    <tbody>
    <% Collection<ExternalComponentConfiguration> blockedComponents = ExternalComponentManager.getBlockedComponents();
       if (blockedComponents.isEmpty()) { %>
        <tr>
            <td align="center" colspan="7"><fmt:message key="component.settings.empty_list" /></td>
        </tr>
       <% }
        else {
        int count = 1;
        for (Iterator<ExternalComponentConfiguration> it=blockedComponents.iterator(); it.hasNext(); count++) {
            ExternalComponentConfiguration configuration = it.next();
       %>
	    <tr class="jive-<%= (((count%2)==0) ? "even" : "odd") %>">
	        <td>
	            <%= count %>
	        </td>
	        <td>
	            <%= configuration.getSubdomain() %>
	        </td>
	        <td align="center" style="border-right:1px #ccc solid;">
	            <a href="#" onclick="if (confirm('<fmt:message key="component.settings.confirm_delete" />')) { location.replace('external-components-settings.jsp?deleteConf=<%= configuration.getSubdomain() %>'); } "
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
    <form action="external-components-settings.jsp" method="post">
    <tr>
        <td nowrap width="1%">
            <fmt:message key="component.settings.subdomain" />
        </td>
        <td>
            <input type="text" size="40" name="subdomain" value="<%= componentBlocked ?  subdomain : "" %>"/>&nbsp;
            <input type="submit" name="componentBlocked" value="<fmt:message key="component.settings.block" />">
        </td>
    </tr>
    </form>
    </table>
    </div>
</fieldset>

    </body>
</html>
