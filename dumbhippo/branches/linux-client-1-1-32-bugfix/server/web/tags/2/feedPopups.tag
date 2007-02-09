<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:set var="loadingHtml" scope="page">
	<jsp:attribute name="value">
		<%-- this is to make the loading popup taller to match the other two popups --%>
		<div style="height: 100px;"></div>
	</jsp:attribute>
</c:set>
<dht:feedPopup id="dhFeedLoadingPopup" title="LOCATING FEED..." icon="/images2/${buildstamp}/feedspinner.gif" bodyHtml="${loadingHtml}">
	<input type="button" value="OK" disabled="true"/> <%--- never gets enabled, we just switch to PreviewPopup --%>
	<input type="button" value="Cancel" onclick="dh.feeds.loadingCancel()"/>
</dht:feedPopup>
<c:set var="previewHtml" scope="page">
	<jsp:attribute name="value">
		<div id="dhFeedPreview"></div>
	</jsp:attribute>
</c:set>
<dht:feedPopup id="dhFeedPreviewPopup" title="FOUND A FEED!" icon="/images2/${buildstamp}/check21x20.png" bodyHtml="${previewHtml}">
	<input type="button" value="OK"  onclick="dh.feeds.previewOK()"/>
	<input type="button" value="Cancel" onclick="dh.feeds.previewCancel()"/>
</dht:feedPopup>
<c:set var="failedHtml" scope="page">
	<jsp:attribute name="value">
		<div id="dhFeedFailedMessage"></div>
	</jsp:attribute>
</c:set>
<dht:feedPopup id="dhFeedFailedPopup" title="FEED NOT FOUND" icon="/images2/${buildstamp}/alert21x20.png" bodyHtml="${failedHtml}">
	<input type="button" value="Try Again" onclick="dh.feeds.failedTryAgain()"/>
	<input type="button" value="Cancel" onclick="dh.feeds.failedCancel()"/>
</dht:feedPopup>
