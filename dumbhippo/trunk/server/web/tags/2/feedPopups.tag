<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:feedPopup id="dhFeedLoadingPopup" title="LOCATING FEED..." icon="/images2/${buildstamp}/feedspinner.gif" url="http://example.com">
	<input type="button" value="OK"/>
	<input type="button" value="Cancel"/>
</dht:feedPopup>
<dht:feedPopup id="dhFeedPreviewPopup" title="FOUND A FEED!" icon="/images2/${buildstamp}/check21x20.png" url="http://example.com">
	<input type="button" value="OK"/>
	<input type="button" value="Cancel"/>
</dht:feedPopup>
<dht:feedPopup id="dhFeedFailedPopup" title="FEED NOT FOUND" icon="/images2/${buildstamp}/alert21x20.png" url="http://example.com">
	<input type="button" value="Try Again"/>
	<input type="button" value="Cancel"/>
</dht:feedPopup>
