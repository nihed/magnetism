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
		<c:when test="${browser.linuxRequested}">
			<%-- LINUX RELEASE NOTES GO HERE --%>
			<p>Version 1.1.23</p>
			<ul>
				<li>A better way to expand and collapse blocks.</li>			
				<li>Bug fixes.</li>
			</ul>
			<div class="dh-upgrade-message">
			    Note: the last version of Mugshot didn't shut down properly
			    on some systems, so after upgrade, you may end up with two
			    mugshot icons in your notification area. If you see this, 
			    please open a terminal and  enter the following command:
			    <div class="dh-command">
				   killall mugshot ; sleep 1 ; mugshot &
				</div>
				Or, simply log out and log back in.
			</div>
		</c:when>
		<c:otherwise>
			<%-- WINDOWS RELEASE NOTES GO HERE --%>
			<p>Version 1.1.76</p>
			<ul>
				<li>A better way to expand and collapse blocks.</li>			
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
				<c:when test="${browser.linuxRequested}">
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
