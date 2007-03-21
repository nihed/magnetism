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
			<p>Version 1.1.37</p>
			<ul>
				<li>Ability to filter which blocks are displayed.</li>
				<li>Optional application usage statistics tracking.</li>
			</ul>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<p>Version 1.1.88</p>
			<ul>
				<li>Includes a Firefox extension with a toolbar icon and live chat preview.</li>
				<li>Links opened in the user's default browser.</li>

				<li>New "Quips and Comments" window replacing the old chat window.</li>
				<li>Notifications for Netflix.</li>
				<li>Optional crash reporting.</li>
				<li>Bug fixes.</li>
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
					<c:if test="${download.haveDownload}">
						<a href="${download.downloadUrl}"><c:out value="${download.downloadFor}"/> RPM</a>
						<br/>
					</c:if>
					Source code: <a href="${download.downloadUrlLinuxTar}">tar.gz</a> | <a href="${download.downloadUrlSrpm}">SRPM</a>
					<br/>
					<a href="http://developer.mugshot.org/wiki/Downloads" target="_new">Packages for other distributions</a>
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
