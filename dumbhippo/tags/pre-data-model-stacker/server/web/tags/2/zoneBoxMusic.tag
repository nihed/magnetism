<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="more" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableJumpTo" required="false" type="java.lang.Boolean" %>

<%-- more attribute handling deleted in case something is using this tag still - /music doesn't exist --%>

<dht:zoneBox zone="music" topImage="/images2/${buildStamp}/header_music500.gif" bottomImage="/images2/${buildStamp}/bottom_music500.gif" more="${moreLink}" disableJumpTo="${disableJumpTo}">
	<jsp:doBody/>
</dht:zoneBox>
