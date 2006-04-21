<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="more" required="false" type="java.lang.Boolean" %>

<c:if test="${more}">
	<c:set var="moreLink" value="/tv" scope="page"/>
</c:if>

<dht:zoneBox zone="tv" topImage="/images2/header_tvparty500.gif" bottomImage="/images2/bottom_tvparty500.gif" more="${moreLink}">
	<jsp:doBody/>
</dht:zoneBox>
