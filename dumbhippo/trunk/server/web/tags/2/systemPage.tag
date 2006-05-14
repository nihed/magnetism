<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="topImage" required="false" type="java.lang.String" %>
<%@ attribute name="topText" required="false" type="java.lang.String" %>
<%@ attribute name="disableJumpTo" required="false" type="java.lang.Boolean" %>

<dht:twoColumnPage neverShowSidebar="true">
	<dht:zoneBox zone="group" topImage="${topImage}" topText="${topText}" bottomImage="/images2/bottom_gray500.gif" disableJumpTo="${disableJumpTo}">
		<jsp:doBody/>
	</dht:zoneBox>
</dht:twoColumnPage>
