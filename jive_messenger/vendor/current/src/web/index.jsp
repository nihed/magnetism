<%--
  -	$Revision: 7742 $
  -	$Date: 2007-03-27 19:44:27 -0500 (Tue, 27 Mar 2007) $
  -
  - Copyright (C) 2004-2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.admin.AdminConsole,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.StringUtils"
%>
<%@ page import="org.jivesoftware.openfire.XMPPServer"%>
<%@ page import="org.jivesoftware.openfire.update.Update"%>
<%@ page import="org.jivesoftware.openfire.update.UpdateManager"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="org.jivesoftware.openfire.spi.ConnectionManagerImpl" %>
<%@ page import="org.apache.mina.transport.socket.nio.SocketAcceptor" %>
<%@ page import="java.net.SocketAddress" %>
<%@ page import="java.net.InetSocketAddress" %>
<%@ page import="org.jivesoftware.openfire.ServerPort" %>
<%@ page import="org.jivesoftware.openfire.filetransfer.proxy.FileTransferProxy" %>
<%@ page import="org.jivesoftware.openfire.http.HttpBindManager" %>
<%@ page import="org.jivesoftware.openfire.mediaproxy.MediaProxyService" %>
<%@ page import="org.jivesoftware.openfire.stun.STUNService" %>
<%@ page import="org.jivesoftware.openfire.session.ConnectionMultiplexerSession" %>
<%@ page import="org.jivesoftware.openfire.Connection" %>
<%@ page import="org.jivesoftware.openfire.session.ClientSession" %>
<%@ page import="org.jivesoftware.util.LocaleUtils" %>
<%@ page import="org.jivesoftware.openfire.container.AdminConsolePlugin" %>
<%@ page import="org.jivesoftware.util.CertificateManager" %>
<%@ page import="org.jivesoftware.openfire.net.SSLConfig" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%-- Define page bean for header and sidebar --%>
<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />

<%  // Simple logout code
    if ("true".equals(request.getParameter("logout"))) {
        session.removeAttribute("jive.admin.authToken");
        response.sendRedirect("index.jsp");
        return;
    }
%>

<%-- Define Administration Bean --%>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out); %>

<% // Get parameters //
    boolean serverOn = (webManager.getXMPPServer() != null);

    String interfaceName = JiveGlobals.getXMLProperty("network.interface");


    ConnectionManagerImpl connectionManager = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager());
    SocketAcceptor socketAcceptor = connectionManager.getSocketAcceptor();
    SocketAcceptor sslSocketAcceptor = connectionManager.getSSLSocketAcceptor();
    SocketAcceptor multiplexerSocketAcceptor = connectionManager.getMultiplexerSocketAcceptor();
    ServerPort serverPort = null;
    ServerPort componentPort = null;
    AdminConsolePlugin adminConsolePlugin =
            (AdminConsolePlugin) XMPPServer.getInstance().getPluginManager().getPlugin("admin");

    FileTransferProxy fileTransferProxy = XMPPServer.getInstance().getFileTransferProxy();
    HttpBindManager httpBindManager = HttpBindManager.getInstance();
    MediaProxyService mediaProxyService = XMPPServer.getInstance().getMediaProxyService();
    STUNService stunService = XMPPServer.getInstance().getSTUNService();

    // Search for s2s and external component ports info
    for (ServerPort port : XMPPServer.getInstance().getServerInfo().getServerPorts()) {
        if (port.getType() == ServerPort.Type.server) {
            serverPort = port;
        } else if (port.getType() == ServerPort.Type.component) {
            componentPort = port;
        }
    }

%>

<html>
    <head>
        <title><fmt:message key="index.title"/></title>
        <meta name="pageID" content="server-settings"/>
        <meta name="helpPage" content="about_the_server.html"/>
    </head>
    <body>

<%
    UpdateManager updateManager = XMPPServer.getInstance().getUpdateManager();
    Update serverUpdate = updateManager.getServerUpdate();
    if (serverUpdate != null) { %>
    <div class="warning">
    <table cellpadding="0" cellspacing="0" border="0" >
    <tbody>
        <tr>
            <td class="jive-icon-label">
            <b><fmt:message key="index.update.alert" /></b><br/><br/>
            </td>
        </tr>
        <td valign="top" align="left" colspan="2">
        <span><fmt:message key="index.update.info">
                <fmt:param value="<%= serverUpdate.getLatestVersion()%>" />
                <fmt:param value="<%= "<a href='"+serverUpdate.getURL()+"'>" %>" />
                <fmt:param value="<%= "</a>" %>" />
                <fmt:param value="<%= "<a href='"+serverUpdate.getChangelog()+"'>" %>" />
                <fmt:param value="<%= "</a>" %>" />
            </fmt:message></span>
        </td>
    </tbody>
    </table>
    </div>
    <br>

<%
    }
%>
<p>
<fmt:message key="index.title.info" />
</p>

<style type="text/css">
.bar TD {
    padding : 0px;
}
</style>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2"><fmt:message key="index.properties" /></th>
    </tr>
</thead>
<tbody>

    <%  if (serverOn) { %>

         <tr>
            <td class="c1"><fmt:message key="index.uptime" /></td>
            <td>
                <%
                    long now = System.currentTimeMillis();
                    long lastStarted = webManager.getXMPPServer().getServerInfo().getLastStarted().getTime();
                    long uptime = now - lastStarted;
                    String uptimeDisplay = StringUtils.getElapsedTime(uptime);
                %>

                <%  if (uptimeDisplay != null) { %>
                    <%= uptimeDisplay %> -- started
                <%  } %>

                <%= JiveGlobals.formatDateTime(webManager.getXMPPServer().getServerInfo().getLastStarted()) %>
            </td>
        </tr>

    <%  } %>

    <tr>
        <td class="c1"><fmt:message key="index.version" /></td>
        <td class="c2">
            <%= AdminConsole.getAppName() %>
            <%= AdminConsole.getVersionString() %>
        </td>
    </tr>
    <tr>
        <td class="c1"><fmt:message key="index.home" /></td>
        <td class="c2">
            <%= JiveGlobals.getHomeDirectory() %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="index.server_name" />
        </td>
        <td class="c2">
            <% if (!CertificateManager.isRSACertificate(SSLConfig.getKeyStore(), XMPPServer.getInstance().getServerInfo().getName())) {%>
            <img src="images/warning-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="index.certificate-warning" />" text="<fmt:message key="index.certificate-warning" />">&nbsp;
            <% } %>
            ${webManager.serverInfo.name}
        </td>
    </tr>
</tbody>
<thead>
    <tr>
        <th colspan="2"><fmt:message key="index.environment" /></th>
    </tr>
</thead>
<tbody>
    <tr>
        <td class="c1"><fmt:message key="index.jvm" /></td>
        <td class="c2">
            <%
                String vmName = System.getProperty("java.vm.name");
                if (vmName == null) {
                    vmName = "";
                }
                else {
                    vmName = " -- " + vmName;
                }
            %>
            <%= System.getProperty("java.version") %> <%= System.getProperty("java.vendor") %><%= vmName %>
        </td>
    </tr>
    <tr>
        <td class="c1"><fmt:message key="index.app" /></td>
        <td class="c2">
            <%= application.getServerInfo() %>
        </td>
    </tr>
    <tr>
        <td class="c1"><fmt:message key="index.os" /></td>
        <td class="c2">
            <%= System.getProperty("os.name") %> / <%= System.getProperty("os.arch") %>
        </td>
    </tr>
    <tr>
        <td class="c1"><fmt:message key="index.local" /></td>
        <td class="c2">
            <%= JiveGlobals.getLocale() %> / <%= JiveGlobals.getTimeZone().getDisplayName(JiveGlobals.getLocale()) %>
            (<%= (JiveGlobals.getTimeZone().getRawOffset()/1000/60/60) %> GMT)
        </td>
    </tr>
    <tr>
        <td><fmt:message key="index.memory" /></td>
        <td>
        <%    // The java runtime
            Runtime runtime = Runtime.getRuntime();

            double freeMemory = (double)runtime.freeMemory()/(1024*1024);
            double maxMemory = (double)runtime.maxMemory()/(1024*1024);
            double totalMemory = (double)runtime.totalMemory()/(1024*1024);
            double usedMemory = totalMemory - freeMemory;
            double percentFree = ((maxMemory - usedMemory)/maxMemory)*100.0;
            double percentUsed = 100 - percentFree;
            int percent = 100-(int)Math.round(percentFree);

            DecimalFormat mbFormat = new DecimalFormat("#0.00");
            DecimalFormat percentFormat = new DecimalFormat("#0.0");
        %>

        <table cellpadding="0" cellspacing="0" border="0" width="300">
        <tr valign="middle">
            <td width="99%" valign="middle">
                <div class="bar">
                <table cellpadding="0" cellspacing="0" border="0" width="100%" style="border:1px #666 solid;">
                <tr>
                    <%  if (percent == 0) { %>

                        <td width="100%"><img src="images/percent-bar-left.gif" width="100%" height="8" border="0" alt=""></td>

                    <%  } else { %>

                        <%  if (percent >= 90) { %>

                            <td width="<%= percent %>%" background="images/percent-bar-used-high.gif"
                                ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>

                        <%  } else { %>

                            <td width="<%= percent %>%" background="images/percent-bar-used-low.gif"
                                ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>

                        <%  } %>
                        <td width="<%= (100-percent) %>%" background="images/percent-bar-left.gif"
                            ><img src="images/blank.gif" width="1" height="8" border="0" alt=""></td>
                    <%  } %>
                </tr>
                </table>
                </div>
            </td>
            <td width="1%" nowrap>
                <div style="padding-left:6px;">
                <%= mbFormat.format(usedMemory) %> MB of <%= mbFormat.format(maxMemory) %> MB (<%= percentFormat.format(percentUsed) %>%) used
                </div>
            </td>
        </tr>
        </table>
        </td>
    </tr>
</tbody>
</table>
</div>

<br>

<div id="jive-title"><fmt:message key="index.server_port" /></div>
<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th width="80"><fmt:message key="ports.interface" /></th>
        <th width="1"><fmt:message key="ports.port" /></th>
        <th width="1">&nbsp;</th>
        <th width="130"><fmt:message key="ports.type" /></th>
        <th><fmt:message key="ports.description" /></th>
    </tr>
</thead>
<tbody>
    <% if (socketAcceptor != null) {
        for (SocketAddress socketAddress : socketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <% if (ClientSession.getTLSPolicy() == Connection.TLSPolicy.disabled) { %>
            <td><img src="images/blank.gif" width="1" height="1"></td>
        <% } else { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
        <% } %>
        <td><fmt:message key="ports.client_to_server" /></td>
        <td><fmt:message key="ports.client_to_server.desc">
            <fmt:param value="<a href='ssl-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <% if (sslSocketAcceptor != null) {
        for (SocketAddress socketAddress : sslSocketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
        <td><fmt:message key="ports.client_to_server" /></td>
        <td><fmt:message key="ports.client_to_server.desc_old_ssl">
            <fmt:param value="<a href='ssl-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <%
        if (serverPort != null) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : serverPort.getIPAddress() %></td>
        <td><%= serverPort.getPort() %></td>
        <% if (JiveGlobals.getBooleanProperty("xmpp.server.tls.enabled", true)) { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
        <% } else { %>
            <td><img src="images/blank.gif" width="1" height="1"></td>
        <% } %>
        <td><fmt:message key="ports.server_to_server" /></td>
        <td><fmt:message key="ports.server_to_server.desc">
            <fmt:param value="<a href='server2server-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
        <td>
</td>
    </tr>
    <% } %>
    <% if (multiplexerSocketAcceptor != null) {
        for (SocketAddress socketAddress : multiplexerSocketAcceptor.getManagedServiceAddresses()) {
            InetSocketAddress address = (InetSocketAddress) socketAddress;
    %>
    <tr>
        <td><%= "0.0.0.0".equals(address.getHostName()) ? LocaleUtils.getLocalizedString("ports.all_ports") : address.getHostName() %></td>
        <td><%= address.getPort() %></td>
        <% if (ConnectionMultiplexerSession.getTLSPolicy() == Connection.TLSPolicy.disabled) { %>
            <td><img src="images/blank.gif" width="1" height="1"></td>
        <% } else { %>
            <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
        <% } %>
        <td><fmt:message key="ports.connection_manager" /></td>
        <td><fmt:message key="ports.connection_manager.desc">
            <fmt:param value="<a href='connection-managers-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } } %>
    <%
        if (componentPort != null) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : componentPort.getIPAddress() %></td>
        <td><%= componentPort.getPort() %></td>
        <td><img src="images/blank.gif" width="1" height="1"></td>
        <td><fmt:message key="ports.external_components" /></td>
        <td><fmt:message key="ports.external_components.desc">
            <fmt:param value="<a href='external-components-settings.jsp'>" />
            <fmt:param value="</a>" />
            </fmt:message>
        </td>
    </tr>
    <% } %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
        <td><%= adminConsolePlugin.getAdminUnsecurePort() %></td>
        <td><img src="images/blank.gif" width="1" height="1"></td>
        <td><fmt:message key="ports.admin_console" /></td>
        <td><fmt:message key="ports.admin_console.desc_unsecured" /></td>
    </tr>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
        <td><%= adminConsolePlugin.getAdminSecurePort() %></td>
        <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
        <td><fmt:message key="ports.admin_console" /></td>
        <td><fmt:message key="ports.admin_console.desc_secured" /></td>
    </tr>
    <%
        if (fileTransferProxy.isProxyEnabled()) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
        <td><%= fileTransferProxy.getProxyPort() %></td>
        <td><img src="images/blank.gif" width="1" height="1"></td>
        <td><fmt:message key="ports.file_proxy" /></td>
        <td><fmt:message key="ports.file_proxy.desc" /></td>
    </tr>
    <% } %>
    <%
        if (httpBindManager.isHttpBindEnabled()) {
    %>
        <%
            if (httpBindManager.getHttpBindUnsecurePort() > 0) {
        %>
        <tr>
            <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
            <td><%= httpBindManager.getHttpBindUnsecurePort() %></td>
            <td><img src="images/blank.gif" width="1" height="1"></td>
            <td><fmt:message key="ports.http_bind" /></td>
            <td><fmt:message key="ports.http_bind.desc_unsecured" /></td>
        </tr>
        <% } %>
        <%
            if (httpBindManager.getHttpBindSecurePort() > 0) {
        %>
        <tr>
            <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
            <td><%= httpBindManager.getHttpBindSecurePort() %></td>
            <td><img src="images/lock.gif" width="16" height="16" border="0"/></td>
            <td><fmt:message key="ports.http_bind" /></td>
            <td><fmt:message key="ports.http_bind.desc_secured" /></td>
            <td></td>
        </tr>
        <% } %>
    <% } %>
    <%
        if (mediaProxyService.isEnabled()) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
        <td><%= mediaProxyService.getMinPort() %> - <%= mediaProxyService.getMaxPort() %></td>
        <td><img src="images/blank.gif" width="1" height="1"></td>
        <td><fmt:message key="ports.media_proxy" /></td>
        <td><fmt:message key="ports.media_proxy.desc" /></td>
    </tr>
    <% } %>
    <%
        if (stunService.isEnabled()) {
    %>
    <tr>
        <td><%= interfaceName == null ? LocaleUtils.getLocalizedString("ports.all_ports") : interfaceName %></td>
        <td><%= stunService.getPrimaryPort() %> & <%= stunService.getSecondaryPort() %></td>
        <td><img src="images/blank.gif" width="1" height="1"></td>
        <td><fmt:message key="ports.stun" /></td>
        <td><fmt:message key="ports.stun.desc" /></td>
    </tr>
    <% } %>
</tbody>
</table>
</div>
<br>

<form action="server-props.jsp">
<input type="submit" value="<fmt:message key="global.edit_properties" />">
</form>

    </body>
</html>