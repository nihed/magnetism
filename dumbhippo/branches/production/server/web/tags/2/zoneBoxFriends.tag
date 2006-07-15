<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="back" required="false" type="java.lang.Boolean" %>

<dht:zoneBox zone="friend" topImage="/images2/${buildStamp}/header_friends500.gif" bottomImage="/images2/${buildStamp}/bottom_gray500.gif" back="${back}">
	<jsp:doBody/>
</dht:zoneBox>
