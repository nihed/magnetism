<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="topImage" required="false" type="java.lang.String" %>
<%@ attribute name="topText" required="false" type="java.lang.String" %>
<%@ attribute name="logoOnly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableJumpTo" required="false" type="java.lang.Boolean" %>

<dht:body extraClass="dh-gray-background-page">
	<c:choose>
		<c:when test="${logoOnly}"><center><dht:logo/></center></c:when>
		<c:otherwise><dht:header disableHomeLink="${disableHomeLink}"/></c:otherwise>
	</c:choose>
	<dht:zoneBox zone="group" topImage="${topImage}" topText="${topText}" bottomImage="/images2/bottom_gray500.gif" disableJumpTo="${disableJumpTo}">
		<jsp:doBody/>
	</dht:zoneBox>
	<dht:footer/>
</dht:body>

