<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="privatePage" required="false" type="java.lang.Boolean" %>
<%@ attribute name="titleLink" required="false" type="java.lang.String" %>
<%@ attribute name="titleLinkText" required="false" type="java.lang.String" %>

<div class="dh-page-title-container">
	<c:if test="${!empty privatePage && privatePage}">
		<dh:png src="/images3/${buildStamp}/private_icon.png"/>
	</c:if>
	<span class="dh-page-title"><c:out value="${title}"/></span>
	<c:if test="${!empty titleLink}">
		<span class="dh-page-title-link"><a href="${titleLink}"><c:out value="${titleLinkText}"/></a></span>
	</c:if>
	<div class="dh-page-options-container">	
		<div class="dh-page-options"><jsp:doBody/></div>
	</div>
</div>