<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- This is only used in the Linux case, but currently dh:bean as a side effect sets the 
     "browser" variable in request scope, so don't try moving this into the linux-only cases below --%>
<dh:bean id="download" class="com.dumbhippo.web.DownloadPage" scope="page"/>

<head>
	<title>Upgrade</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/upgrade.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:systemPage topText="A new version of Mugshot is available" disableJumpTo="true">
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td><dh:png id="dhUpgradeLogo" src="/images2/${buildStamp}/mugicon90x80.png" style="width: 90; height: 80;"/></td>
	<td>
	<c:choose>
		<c:when test="${browser.linuxRequested}">
			<%-- LINUX RELEASE NOTES GO HERE --%>
			<p>Version 1.1.1</p>
			<ul>
				<li>Initial release</li>
			</ul>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<p>Version 1.1.46</p>
			<ul>
				<li>We've redone the site completely - it's now called Mugshot</li>
				<li>Numerous improvements along the way... let us know what you think</li>
			</ul>
		</c:otherwise>
	</c:choose>
	</td>
	</tr>		
	</table>
	<hr align="center" noshade="true" height="1px" width="80%" class="dh-gray-hr"/><br/>
	<div>
		<center>
			<c:choose>
				<c:when test="${browser.linuxRequested}">
					<a href="${download.downloadUrlLinux}">Fedora Core 5 RPM</a>
					<br/>
					Source code: <a href="${download.downloadUrlLinuxTar}">tar.gz</a> | <a href="${download.downloadUrlLinuxSrpm}">SRPM</a>
				</c:when>
				<c:otherwise>
					<input type="button" value="Install now" onclick="window.external.application.DoUpgrade(); window.close();"/> 
					<input type="button" value="Install later" onclick="window.close();"/>		
				</c:otherwise>		
			</c:choose>
		</center>
	</div>
</dht:systemPage>	
</html>
