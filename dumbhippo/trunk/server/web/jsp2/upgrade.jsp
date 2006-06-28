<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- This is only used in the Linux case, but currently dh:bean as a side effect sets the 
     "browser" variable in request scope, so don't try moving this into the linux-only cases below --%>
<dh:bean id="download" class="com.dumbhippo.web.pages.DownloadPage" scope="page"/>

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
			<p>Version 1.1.6</p>
			<ul>
				<li>Fix a bug where sometimes the first chat message did not show up</li>
				<li>Add support for RSS feeds in groups</li>
				<li>Other small fixes</li>
			</ul>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<p>Version 1.1.54</p>
			<ul>
				<li>Fix a bug where sometimes the first chat message did not show up</li>
				<li>Add support for RSS feeds in groups</li>
				<li>Other small fixes</li>
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
