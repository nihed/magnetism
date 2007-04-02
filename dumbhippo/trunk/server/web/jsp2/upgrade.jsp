<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="download" class="com.dumbhippo.web.DownloadBean" scope="page"/>

<head>
	<title>Upgrade</title>
	<dht:siteStyle/>
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
		<c:when test="${download.linuxRequested}">
			<%-- LINUX RELEASE NOTES GO HERE --%>
			<p>Version 1.1.40</p>
			<ul>
				<li><a href="/applications" target="_new">Application pages</a> now show "Install" and "Run" links when possible.</li>
				<li>Support for Picasa photos and Google Reader shared items.</li>
				<li>Facebook notifications no longer require frequent login.</li>
				<li>Added a menu item to the desktop main menu and a "Quit" option to the tray icon.</li>
				<li>Builds for Fedora Rawhide and all Fedora architectures (PowerPC and x86_64 in addition to x86).</li>
				<li>Bug fixes and performance enhancements.</li>
			</ul>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<p>Version 1.1.89</p>
			<ul>
				<li>Ability to filter which blocks are displayed in the stacker.</li>
				<li>Support for Picasa photos and Google Reader shared items.</li>
				<li>Facebook notifications no longer require frequent login.</li>
				<li>Bug fixes and performance enhancements.</li>
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
				<c:when test="${download.linuxRequested}">
					<c:if test="${!empty download.download}">
						<a href="${download.download.url}">
							<c:out value="${download.download.distribution.name}"/>
							<c:out value="${download.download.distribution.osVersion}"/>
							(<c:out value="${download.download.architecture}"/>) RPM
						<br/>
					</c:if>
					<a href="http://developer.mugshot.org/wiki/Downloads" target="_new">Source code and contributed binaries</a>
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
