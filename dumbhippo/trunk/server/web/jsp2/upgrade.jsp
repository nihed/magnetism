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
			<p>Version 1.1.5</p>
			<ul>
                                <li>Add Join Chat and Ignore buttons to notification bubbles</li>
				<li>Fix a memory leak (thanks to Luke Macken)</li>
				<li>Keep notification bubble sizes more consistent and reasonable</li>
				<li>Always place notification bubbles in a corner of the screen</li>
				<li>Reduced startup time</li>
				<li>Fix a memory corruption issue</li>
				<li>Close button closes all bubbles at once instead of only one</li>
				<li>Include connection status in icon tooltip</li>
				<li>Display "The World" in bubbles for world shares</li>
			</ul>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<div class="dh-message">
			   Note: this upgrade might leave your copy of Mugshot in a non-working state. Please check out the
<a href="http://developer.mugshot.org/wiki/How_To_Repair_Your_Windows_Client">Instructions to Fix</a> first.
			</div>
			<p>Version 1.1.53</p>
			<ul>
				<li>Don't lose files on future upgrades</li>
				<li>Add a link to join the chat directly from the bubble.</li>
				<li>Allow the user to ignore chatter on uninteresting posts.</li>
				<li>Bug fix: don't pop up already seen bubbles when reconnecting to the server.</li>
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
