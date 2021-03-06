<%--
  -	$Revision: 3195 $
  -	$Date: 2005-12-13 13:07:30 -0500 (Tue, 13 Dec 2005) $
  -
  - Copyright (C) 2004 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.util.*,
                 java.util.*,
                 org.jivesoftware.wildfire.net.SSLConfig,
                 java.security.KeyStore,
                 java.security.cert.CertificateFactory,
                 java.security.cert.Certificate,
                 java.io.ByteArrayInputStream"
    errorPage="error.jsp"
%>
<%@ page import="org.jivesoftware.wildfire.ClientSession"%>
<%@ page import="org.jivesoftware.wildfire.net.SocketConnection"%>
<%@ page import="org.jivesoftware.wildfire.XMPPServer"%>
<%@ page import="org.jivesoftware.wildfire.ConnectionManager"%>
<%@ page import="org.jivesoftware.wildfire.Connection"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%  try { %>

<%  // Get parameters:
    String type = ParamUtils.getParameter(request, "type");
    String cert = ParamUtils.getParameter(request, "cert");
    String alias = ParamUtils.getParameter(request, "alias");
    boolean install = request.getParameter("install") != null;
    boolean uninstall = ParamUtils.getBooleanParameter(request,"uninstall");

    boolean update = request.getParameter("update") != null;
    boolean success = ParamUtils.getBooleanParameter(request, "success");
    String clientSecurityRequired = ParamUtils.getParameter(request,"clientSecurityRequired");
    String ssl = ParamUtils.getParameter(request, "ssl");
    String tls = ParamUtils.getParameter(request, "tls");

    KeyStore keyStore = SSLConfig.getKeyStore();
    KeyStore trustStore = SSLConfig.getTrustStore();

    Map<String, Object> errors = new HashMap<String, Object>();
    if (update) {
        if ("req".equals(clientSecurityRequired)) {
            // User selected that security is required

            // Enable 5222 port and make TLS required
            XMPPServer.getInstance().getConnectionManager().enableClientListener(true);
            ClientSession.setTLSPolicy(Connection.TLSPolicy.required);
            // Enable 5223 port (old SSL port)
            XMPPServer.getInstance().getConnectionManager().enableClientSSLListener(true);
        }
        else if ("notreq".equals(clientSecurityRequired)) {
            // User selected that security is NOT required

            // Enable 5222 port and make TLS optional
            XMPPServer.getInstance().getConnectionManager().enableClientListener(true);
            ClientSession.setTLSPolicy(Connection.TLSPolicy.optional);
            // Enable 5223 port (old SSL port)
            XMPPServer.getInstance().getConnectionManager().enableClientSSLListener(true);
        }
        else if ("custom".equals(clientSecurityRequired)) {
            // User selected custom client authentication

            // Enable or disable 5223 port (old SSL port)
            XMPPServer.getInstance().getConnectionManager().enableClientSSLListener("available".equals(ssl));

            // Enable port 5222 and configure TLS policy
            XMPPServer.getInstance().getConnectionManager().enableClientListener(true);
            if ("notavailable".equals(tls)) {
                ClientSession.setTLSPolicy(Connection.TLSPolicy.disabled);
            }
            else if ("optional".equals(tls)) {
                ClientSession.setTLSPolicy(Connection.TLSPolicy.optional);
            }
            else {
                ClientSession.setTLSPolicy(Connection.TLSPolicy.required);
            }
        }
        success = true;
    }

    // Set page vars
    ConnectionManager connectionManager = XMPPServer.getInstance().getConnectionManager();
    if (connectionManager.isClientListenerEnabled() && connectionManager.isClientSSLListenerEnabled()) {
        if (Connection.TLSPolicy.required.equals(ClientSession.getTLSPolicy())) {
            clientSecurityRequired = "req";
            ssl = "available";
            tls = "required";
        }
        else if (Connection.TLSPolicy.optional.equals(ClientSession.getTLSPolicy())) {
            clientSecurityRequired = "notreq";
            ssl = "available";
            tls = "optional";
        }
        else {
            clientSecurityRequired = "custom";
            ssl = "available";
            tls = "notavailable";
        }
    }
    else {
        clientSecurityRequired = "custom";
        ssl = connectionManager.isClientSSLListenerEnabled() ? "available" : "notavailable";
        tls = Connection.TLSPolicy.disabled.equals(ClientSession.getTLSPolicy()) ? "notavailable" : ClientSession.getTLSPolicy().toString();
    }

    if (install) {
        if (cert == null){
            errors.put("cert","");
        }
        if (alias == null) {
            errors.put("alias","");
        }
        if (errors.size() == 0) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(cert.getBytes()));
                if ("client".equals(type)){
                    trustStore.setCertificateEntry(alias,certificate);
                }
                else {
                    keyStore.setCertificateEntry(alias,certificate);
                }
                SSLConfig.saveStores();
                response.sendRedirect("ssl-settings.jsp?success=true");
                return;
            }
            catch (Exception e) {
                errors.put("general","");
            }
        }
    }
    if (uninstall) {
        if (type != null && alias != null) {
            try {
                if ("client".equals(type)){
                    SSLConfig.getTrustStore().deleteEntry(alias);
                }
                else if ("server".equals(type)) {
                    SSLConfig.getKeyStore().deleteEntry(alias);
                }
                SSLConfig.saveStores();
                response.sendRedirect("ssl-settings.jsp?deletesuccess=true");
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
                errors.put("delete", e);
            }
        }
    }
%>

<html>
    <head>
        <title><fmt:message key="ssl.settings.title"/></title>
        <meta name="pageID" content="server-ssl"/>
        <meta name="helpPage" content="manage_security_certificates.html"/>
        <script language="JavaScript" type="text/javascript">
            <!-- // code for window popups
            function showOrHide(whichLayer, mode)
            {

                if (mode == "show") {
                    mode = "";
                }
                else {
                    mode = "none";
                }

                if (document.getElementById)
                {
                    // this is the way the standards work
                    var style2 = document.getElementById(whichLayer).style;
                    style2.display = mode;
                }
                else if (document.all)
                {
                    // this is the way old msie versions work
                    var style2 = document.all[whichLayer].style;
                    style2.display = mode;
                }
                else if (document.layers)
                {
                    // this is the way nn4 works
                    var style2 = document.layers[whichLayer].style;
                    style2.display = mode;
                }
            }

            function togglePublicKey(pkLayer, indexLayer)
            {
                if (document.getElementById)
                {
                    // this is the way the standards work
                    var style2 = document.getElementById(pkLayer).style;
                    var certs = document.getElementById(indexLayer);
                    certs.rowSpan = style2.display? 2:1;
                    style2.display = style2.display? "":"none";
                }
                else if (document.all)
                {
                    // this is the way old msie versions work
                    var style2 = document.all[pkLayer].style;
                    var certs = document.all[indexLayer];
                    certs.rowSpan = style2.display? 2:1;
                    style2.display = style2.display? "":"none";
                }
                else if (document.layers)
                {
                    // this is the way nn4 works
                    var style2 = document.layers[pkLayer].style;
                    var certs = document.layers[indexLayer];
                    certs.rowSpan = style2.display? 2:1;
                    style2.display = style2.display? "":"none";
                }
            }
            //-->
        </script>
    </head>
    <body>

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="ssl.settings.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (ParamUtils.getBooleanParameter(request,"deletesuccess")) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="ssl.settings.uninstalled" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (errors.containsKey("delete")) {
        Exception e = (Exception)errors.get("delete");
%>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="ssl.settings.error" />
        <%  if (e != null && e.getMessage() != null) { %>
            <fmt:message key="ssl.settings.error_messenge" />: <%= e.getMessage() %>
        <%  } %>
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } else if (errors.size() > 0) { %>

    <div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="ssl.settings.error_certificate" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="ssl.settings.client.info" />
</p>

<form action="ssl-settings.jsp" method="post">

<fieldset>
    <legend><fmt:message key="ssl.settings.client.legend" /></legend>
    <div>
    <table id="certificates" cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <tr valign="middle">
            <tr valign="middle">
                <td width="1%" nowrap>
                    <input type="radio" name="clientSecurityRequired" value="notreq" id="rb02" onclick="showOrHide('custom', 'hide')"
                     <%= ("notreq".equals(clientSecurityRequired) ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb02">
                    <b><fmt:message key="ssl.settings.client.label_notrequired" /></b> - <fmt:message key="ssl.settings.client.label_notrequired_info" />
                    </label>
                </td>
            </tr>
            <tr valign="middle">
                <td width="1%" nowrap>
                    <input type="radio" name="clientSecurityRequired" value="req" id="rb01" onclick="showOrHide('custom', 'hide')"
                 <%= ("req".equals(clientSecurityRequired) ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb01">
                    <b><fmt:message key="ssl.settings.client.label_required" /></b> - <fmt:message key="ssl.settings.client.label_required_info" />
                    </label>
                </td>
            </tr>
            <tr valign="middle">
                <td width="1%" nowrap>
                    <input type="radio" name="clientSecurityRequired" value="custom" id="rb03" onclick="showOrHide('custom', 'show')"
                     <%= ("custom".equals(clientSecurityRequired) ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb03">
                    <b><fmt:message key="ssl.settings.client.label_custom" /></b> - <fmt:message key="ssl.settings.client.label_custom_info" />
                    </label>
                </td>
            </tr>
            <tr valign="top" id="custom" <% if (!"custom".equals(clientSecurityRequired)) out.write("style=\"display:none\""); %>>
                <td width="1%" nowrap>
                    &nbsp;
                </td>
                <td width="99%">
                    <table cellpadding="3" cellspacing="0" border="0" width="100%">
                    <tr valign="top">
                        <td width="1%" nowrap>
                            <fmt:message key="ssl.settings.client.customSSL" />
                        </td>
                        <td width="99%">
                            <input type="radio" name="ssl" value="notavailable" id="rb04" <%= ("notavailable".equals(ssl) ? "checked" : "") %>
                                   onclick="this.form.clientSecurityRequired[2].checked=true;">&nbsp;<label for="rb04"><fmt:message key="ssl.settings.notavailable" /></label>&nbsp;&nbsp;
                            <input type="radio" name="ssl" value="available" id="rb05" <%= ("available".equals(ssl) ? "checked" : "") %>
                                   onclick="this.form.clientSecurityRequired[2].checked=true;">&nbsp;<label for="rb05"><fmt:message key="ssl.settings.available" /></label>
                        </td>
                    </tr>
                    <tr valign="top">
                        <td width="1%" nowrap>
                            <fmt:message key="ssl.settings.client.customTLS" />
                        </td>
                        <td width="99%">
                            <input type="radio" name="tls" value="notavailable" id="rb06" <%= ("notavailable".equals(tls) ? "checked" : "") %>
                                   onclick="this.form.clientSecurityRequired[2].checked=true;">&nbsp;<label for="rb06"><fmt:message key="ssl.settings.notavailable" /></label>&nbsp;&nbsp;
                            <input type="radio" name="tls" value="optional" id="rb07" <%= ("optional".equals(tls) ? "checked" : "") %>
                                   onclick="this.form.clientSecurityRequired[2].checked=true;">&nbsp;<label for="rb07"><fmt:message key="ssl.settings.optional" /></label>&nbsp;&nbsp;
                            <input type="radio" name="tls" value="required" id="rb08" <%= ("required".equals(tls) ? "checked" : "") %>
                                   onclick="this.form.clientSecurityRequired[2].checked=true;">&nbsp;<label for="rb08"><fmt:message key="ssl.settings.required" /></label>
                        </td>
                    </tr>
                    </table>
                </td>
            </tr>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>
<br>

<input type="submit" name="update" value="<fmt:message key="global.save_settings" />">

</form>

<br><br>

<p><b><fmt:message key="ssl.settings.certificate" /></b></p>

<p>
<fmt:message key="ssl.settings.info" />
</p>

<table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th width="1%">&nbsp;</th>
        <th>
            <fmt:message key="ssl.settings.alias" />
        </th>
        <th>
            <fmt:message key="ssl.settings.type" />
        </th>
        <th>
            <fmt:message key="ssl.settings.publickey" />
        </th>
        <th width="1%">
            <fmt:message key="ssl.settings.uninstall" />
        </th>
    </tr>
</thead>
<tbody>

<%  int i=0;
    for (Enumeration aliases=keyStore.aliases(); aliases.hasMoreElements();) {
        i++;
        String a = (String)aliases.nextElement();
        Certificate c = keyStore.getCertificate(a);
%>
    <tr valign="top">
        <td id="rs<%=i%>" width="1" rowspan="1"><%= (i) %>.</td>
        <td width="29%">
            <%= a %>
        </td>
        <td width="67%">
            <%= c.getType() %>
        </td>
        <td width="2%">
            <a href="javascript:togglePublicKey('pk<%=i%>', 'rs<%=i%>');" title="<fmt:message key="ssl.settings.publickey.title" />"><fmt:message key="ssl.settings.publickey.label" /></a>
        </td>
        <td width="1" align="center">
            <a href="ssl-settings.jsp?alias=<%= a %>&type=server&uninstall=true"
             title="<fmt:message key="ssl.settings.click_uninstall" />"
             onclick="return confirm('<fmt:message key="ssl.settings.confirm_uninstall" />');"
             ><img src="images/delete-16x16.gif" width="16" height="16" border="0" alt=""></a>
        </td>
    </tr>
    <tr id="pk<%=i%>" style="display:none">
        <td colspan="3">
            <span class="jive-description">
            <fmt:message key="ssl.settings.key" />
            </span>
<textarea cols="40" rows="3" style="width:100%;font-size:8pt;" wrap="virtual">
<%= c.getPublicKey() %></textarea>
        </td>
    </tr>

<%  } %>

<%  if (i==0) { %>

    <tr>
        <td colspan="4">
            <p>
            <fmt:message key="ssl.settings.no_installed" />
            </p>
        </td>
    </tr>

<%  } %>

</tbody>
</table>

<br><br>

<form action="ssl-settings.jsp" method="post">

<fieldset>
    <legend><fmt:message key="ssl.settings.install_certificate" /></legend>
    <div>
    <p>
      <fmt:message key="ssl.settings.install_certificate_info" />
    </p>
    <table cellpadding="3" cellspacing="0" border="0" width="100%">
    <tbody>
        <%  if (errors.containsKey("alias")) { %>
            <tr><td>&nbsp;</td>
                <td>
                    <span class="jive-error-text">
                    <fmt:message key="ssl.settings.enter_alias" />
                    </span>
                </td>
            </tr>
        <%  } else if (errors.containsKey("cert")) { %>
            <tr><td>&nbsp;</td>
                <td>
                    <span class="jive-error-text">
                    <fmt:message key="ssl.settings.enter_certificate" />
                    </span>
                </td>
            </tr>
        <%  } else if (errors.containsKey("general")) {
                String error = (String)errors.get("general");
        %>
            <tr><td>&nbsp;</td>
                <td>
                    <span class="jive-error-text">
                    <fmt:message key="ssl.settings.error_installing" />
                    <%  if (error != null && !"".equals(error.trim())) { %>
                        <fmt:message key="ssl.settings.error_reported" />: <%= error %>.
                    <%  } %>
                    </span>
                </td>
            </tr>
        <%  } %>
        <tr>
            <td nowrap><fmt:message key="ssl.settings.type" />:</td>
            <td>
                <select name="type" size="1">
                    <option value="server"><fmt:message key="ssl.settings.server" /></option>
                    <option value="client"><fmt:message key="ssl.settings.client" /></option>
                </select>
            </td>
        </tr>
        <tr>
            <td nowrap><fmt:message key="ssl.settings.alias" />:</td>
            <td>
                <input name="alias" type="text" size="50" maxlength="255" value="<%= (alias != null ? alias : "") %>">
            </td>
        </tr>
        <tr valign="top">
            <td nowrap><fmt:message key="ssl.settings.a_certificate" />:</td>
            <td>
                <span class="jive-description">
                <fmt:message key="ssl.settings.paste_certificate" /><br>
                </span>
                <textarea name="cert" cols="55" rows="7" wrap="virtual" style="font-size:8pt;"></textarea>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <br>
                <input type="submit" name="install" value="<fmt:message key="ssl.settings.add_certificate" />">
            </td>
        </tr>
    </tbody>
    </table>
    </div>
</fieldset>

</form>

    </body>
</html>

<%  } catch (Throwable t) { t.printStackTrace(); } %>