<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:zoneBox zone="artists" topImage="/images2/${buildStamp}/header_artists500.gif" bottomImage="/images2/${buildStamp}/bottom_gray500.gif">
	<jsp:doBody/>
</dht:zoneBox>