<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="page" required="false" type="java.lang.String" %>

<c:set var="option" value="use all our features."/>
<c:if test="${page == 'webSwarm'}">
    <c:set var="option" value="use all of Web Swarm's features."/>
</c:if>
<c:if test="${page == 'musicRadar'}">
    <c:set var="option" value="use all of Music Radar's features."/>
</c:if>
<c:if test="${page == 'stacker'}">
    <c:set var="option" value="use all of the Stacker's features."/>
</c:if>
<c:if test="${page == 'webAccounts'}">
    <c:set var="option" value="show your Web account updates."/>
</c:if>

<table id="dhLearnMoreNextStep" cellspacing="0" cellpadding="0">
<tr>
<td>
<div class="dh-learnmore-summary">
	Get the Mugshot download to <c:out value="${option}"/> It's easy and free!
</div>    
<c:choose>
	<c:when test="${signin.valid}">
	    <dh:bean id="download" class="com.dumbhippo.web.DownloadBean" scope="request"/>
        <c:choose>
	        <c:when test="${download.haveDownload}">
					<div class="dh-download-buttons">
						<a id="dhDownloadProduct" class="dh-download-product" href="${download.downloadUrl}"><img src="/images3/${buildStamp}/download_now_button.gif"/></a>
					</div>
					</td>
					<td class="dh-download-details" valign="top">
				        This version is for <c:out value="${download.downloadFor}"/>.
                        <div class="dh-download-more-details">
                        Current version: <c:out value="${download.currentVersion}, ${download.versionDate}."/>
                        </div>
			</c:when>
			<c:otherwise>
			    <c:choose>
			        <c:when test="${download.macRequested}">
			            We're still working on Mac OS X support. However, you can view <a href="/">your Mugshot Stacker</a> on the web.
			        </c:when>
				    <c:when test="${download.linuxRequested}">					
					    Contributed third-party builds <a href="http://developer.mugshot.org/wiki/Downloads">can be found on the Mugshot Wiki</a>.
				    </c:when>
				    <c:otherwise>
				        We don't have a Mugshot Client for your computer yet. However, you can view <a href="/">your Mugshot Stacker</a> on the web.
				    </c:otherwise>
				</c:choose>     					
			</c:otherwise> 
		</c:choose>	   
		<div class="dh-download-more-details">
		Other versions:
		<c:if test="${!download.fedora5Requested}">
			<a href="/download?distribution=fedora5">Fedora Core 5</a>
	    </c:if>
		<c:if test="${!download.fedora6Requested}">
			<c:if test="${!download.fedora5Requested}">| </c:if>
			<a href="/download?distribution=fedora6">Fedora Core 6</a>
	    </c:if>
		<c:if test="${!download.windowsRequested}">
			| <a href="/download?platform=windows">Windows XP</a>
		</c:if>
		</div>
		</td>
	</c:when>
	<c:otherwise>
	    <table id="dhLearnMoreButtons" class="dh-download-buttons" cellspacing="0" cellpadding="0">
	    <tr>
	    <td><span class="dh-button"><a href="/signup"><img src="/images3/${buildStamp}/signup.gif"/></a></span></td>
	    <td valign="middle" align="center"><span class="dh-download-buttons-or">or</span></td>
	    <td><span class="dh-button"><a href="/who-are-you"><img src="/images3/${buildStamp}/login.gif"/></a></span></td>
	    </tr>
	    </table>
	    </td>
	</c:otherwise>
</c:choose>
</tr>
</table>	

