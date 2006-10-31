<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="topImage" required="false" type="java.lang.String" %>
<%@ attribute name="bottomImage" required="false" type="java.lang.String" %>
<%@ attribute name="topText" required="false" type="java.lang.String" %>
<%@ attribute name="disableJumpTo" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableSignupLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="fullHeader" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableFooter" required="false" type="java.lang.Boolean" %>

<c:if test="${empty bottomImage}">
	<c:set var="bottomImage" value="/images2/${buildStamp}/bottom_gray500.gif" scope="page"/>
</c:if>

<dht:twoColumnPage neverShowSidebar="true" logoOnly="${!fullHeader}" disableSignupLink="${disableSignupLink}" disableFooter="${disableFooter}">
<dht:contentColumn>
	<dht:zoneBox zone="system" topImage="${topImage}" topText="${topText}" bottomImage="${bottomImage}" disableJumpTo="${disableJumpTo}">
		<jsp:doBody/>
	</dht:zoneBox>
</dht:contentColumn>
</dht:twoColumnPage>
