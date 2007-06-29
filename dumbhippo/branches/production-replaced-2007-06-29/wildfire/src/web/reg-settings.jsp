<%--
  -	$Revision: 3195 $
  -	$Date: 2005-12-13 13:07:30 -0500 (Tue, 13 Dec 2005) $
  -
  - Copyright (C) 2004-2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.wildfire.handler.IQRegisterHandler,
                 org.jivesoftware.wildfire.handler.IQAuthHandler,
                 java.util.*,
                 org.jivesoftware.wildfire.ClientSession,
                 org.jivesoftware.util.*"
    errorPage="error.jsp"
%>
<%@ page import="java.util.regex.Pattern"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<html>
    <head>
        <title><fmt:message key="reg.settings.title"/></title>
        <meta name="pageID" content="server-reg-and-login"/>
        <meta name="helpPage" content="manage_registration_and_login_settings.html"/>
    </head>
    <body>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;
    boolean inbandEnabled = ParamUtils.getBooleanParameter(request,"inbandEnabled");
    boolean canChangePassword = ParamUtils.getBooleanParameter(request,"canChangePassword");
    boolean anonLogin = ParamUtils.getBooleanParameter(request,"anonLogin");
    String allowedIPs = request.getParameter("allowedIPs");

    // Get an IQRegisterHandler:
    IQRegisterHandler regHandler = new IQRegisterHandler();
    IQAuthHandler authHandler = new IQAuthHandler();

    if (save) {
        regHandler.setInbandRegEnabled(inbandEnabled);
        regHandler.setCanChangePassword(canChangePassword);
        authHandler.setAllowAnonymous(anonLogin);

        // Build a Map with the allowed IP addresses
        Pattern pattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)" +
                "(?:(?:\\*|25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){2}" +
                "(?:\\*|25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
        Map<String,String> newMap = new HashMap<String,String>();
        StringTokenizer tokens = new StringTokenizer(allowedIPs, ", ");
        while (tokens.hasMoreTokens()) {
            String address = tokens.nextToken().trim();
            if (pattern.matcher(address).matches()) {
                newMap.put(address, "");
            }
        }
        ClientSession.setAllowedIPs(newMap);
    }

    // Reset the value of page vars:
    inbandEnabled = regHandler.isInbandRegEnabled();
    canChangePassword = regHandler.canChangePassword();
    anonLogin = authHandler.isAllowAnonymous();
    // Encode the allowed IP addresses
    StringBuilder buf = new StringBuilder();
    Iterator<String> iter = ClientSession.getAllowedIPs().keySet().iterator();
    if (iter.hasNext()) {
        buf.append(iter.next());
    }
    while (iter.hasNext()) {
        buf.append(", ").append((String)iter.next());
    }
    allowedIPs = buf.toString();
%>

<p>
<fmt:message key="reg.settings.info" />
</p>

<form action="reg-settings.jsp">

<% if (save) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
        <fmt:message key="reg.settings.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<% } %>

<fieldset>
    <legend><fmt:message key="reg.settings.inband_account" /></legend>
    <div>
    <p>
    <fmt:message key="reg.settings.inband_account_info" />
    </p>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td width="1%">
                <input type="radio" name="inbandEnabled" value="true" id="rb01"
                 <%= ((inbandEnabled) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb01"><b><fmt:message key="reg.settings.enable" /></b> -
                <fmt:message key="reg.settings.auto_create_user" /></label>
            </td>
        </tr>
        <tr>
            <td width="1%">
                <input type="radio" name="inbandEnabled" value="false" id="rb02"
                 <%= ((!inbandEnabled) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb02"><b><fmt:message key="reg.settings.disable" /></b> - <fmt:message key="reg.settings.not_auto_create" /></label>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br>

<fieldset>
    <legend><fmt:message key="reg.settings.change_password" /></legend>
    <div>
    <p>
    <fmt:message key="reg.settings.change_password_info" />
    </p>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td width="1%">
            <input type="radio" name="canChangePassword" value="true" id="rb03"
             <%= ((canChangePassword) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb03"><b><fmt:message key="reg.settings.enable" /></b> - <fmt:message key="reg.settings.can_change" /></label>
            </td>
        </tr>
        <tr>
            <td width="1%">
            <input type="radio" name="canChangePassword" value="false" id="rb04"
             <%= ((!canChangePassword) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb04"><b><fmt:message key="reg.settings.disable" /></b> - <fmt:message key="reg.settings.cannot_change" /></label>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br>

<fieldset>
    <legend><fmt:message key="reg.settings.anonymous_login" /></legend>
    <div>
    <p>
    <fmt:message key="reg.settings.anonymous_login_info" />
    </p>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td width="1%">
            <input type="radio" name="anonLogin" value="true" id="rb05"
             <%= ((anonLogin) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb03"><b><fmt:message key="reg.settings.enable" /></b> - <fmt:message key="reg.settings.anyone_login" /></label>
            </td>
        </tr>
        <tr>
            <td width="1%">
            <input type="radio" name="anonLogin" value="false" id="rb06"
             <%= ((!anonLogin) ? "checked" : "") %>>
            </td>
            <td width="99%">
                <label for="rb04"><b><fmt:message key="reg.settings.disable" /></b> - <fmt:message key="reg.settings.only_registered_login" /></label>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br>

<fieldset>
    <legend><fmt:message key="reg.settings.allowed_ips" /></legend>
    <div>
    <p>
    <fmt:message key="reg.settings.allowed_ips_info" />
    </p>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr>
            <td>
                <textarea name="allowedIPs" cols="40" rows="3" wrap="virtual"><%= ((allowedIPs != null) ? allowedIPs : "") %></textarea>
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

<br><br>

<input type="submit" name="save" value="<fmt:message key="global.save_settings" />">

</form>

    </body>
</html>