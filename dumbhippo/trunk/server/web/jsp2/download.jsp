<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.pages.DownloadPage" scope="page"/>

<head>
	<title>Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/download.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.download");
		dh.download.needTermsOfUse = ${signin.needsTermsOfUse}
		dojo.event.connect(dojo, "loaded", function () { dh.download.init() })
	</script>
</head>

<dht:body>
	<img src="/images2/${buildStamp}/mugshot_tagline.gif"/>
	<div class="dh-special-subtitle"><c:if test="${browser.linuxRequested}">A Linux user! </c:if>Thanks for trying us out!  Here's how to start using our tools...</div>
	<table cellspacing="15px" cellpadding="0" align="center">
	<c:if test="${signin.needsTermsOfUse}">
		<tr>
		<td align="center" colspan="5">
			<div class="dh-accept-terms-box-normal" id="dhAcceptTermsBox">
				<div class="dh-accept-terms-warning">
					You must agree to the Terms of Use before continuing.
				</div>
				<input type="checkbox" id="dhAcceptTerms" onclick="dh.download.updateDownload();">
					I accept the Mugshot <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>.
				</input>
			</div>
		</td>
		</tr>
	</c:if>
	<tr valign="top">
	<td>
		<table cellspacing="0" cellpadding="0">
			<tr><td><img id="dhDownloadImg" src="/images2/${buildStamp}/buzzer63x58.gif"/></td>
			<td class="dh-download-instructions">
				<c:if test="${browser.linuxRequested}">
					<c:set var="forFedora" value=" for Fedora Core 5" scope="page"/>
				</c:if>	
				1. <a id="dhDownloadProduct" class="dh-download-product" href="javascript:dh.download.doDownload('${welcome.downloadUrl}')">Click here to download</a><c:out value="${forFedora}"/>.<br/>
			    <c:choose>
			        <c:when test="${browser.linuxRequested}">
			        Install the package, log out of your desktop, and log back in.
			        </c:when>
			        <c:otherwise>
			        The software will install automatically.
			        </c:otherwise>
			    </c:choose>
			</td>
			</tr>
		</table>
	</td>
	<td class="dh-download-separator"><div></div></td>
	<td class="dh-download-instructions">
	2. Click the link on the bubble that appears.<br/>
	<img src="/images2/${buildStamp}/minibubble.gif"/>
	</td>
	</tr>
	</table>
	<div class="dh-special-subtitle dh-download-bottom-title">
	  <div>
	      That will activate Web Swarm and get you started with Mugshot.
	  </div>
	  <div>
	      Songs you play in 
	      <c:choose>
		      <c:when test="${browser.linuxRequested}">
	      		  Rhythmbox		
		      </c:when>
		      <c:otherwise>
			      iTunes or Yahoo! Music
		      </c:otherwise>
	      </c:choose>			
	      will show up in Music Radar.  Easy!
	  </div>
	</div>
	<dht:notevil/>
	<div class="dh-disclaimer">Download for <a href="/download?platform=windows">Windows</a> | <a href="/download?platform=linux">Linux</a></div>
	<div class="dh-disclaimer"><a id="dhSkipDownload" href="javascript:dh.download.doDownload()">I can't install on this computer, skip download.</a> (Not recommended.)</div>
</dht:body>

</html>
