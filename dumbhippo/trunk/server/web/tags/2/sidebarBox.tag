<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="boxClass" required="false" type="java.lang.String" %>
<%@ attribute name="more" required="false" type="java.lang.String" %>
<%@ attribute name="lockIcon" required="false" type="java.lang.Boolean" %>

<div class="dh-sidebar-box ${boxClass}">
	<dht:sidebarBoxTitle><c:if test="${lockIcon}"><img src="/images2/lockicon.gif"/> </c:if><c:out value="${title}"/></dht:sidebarBoxTitle>
	<jsp:doBody/>
	<c:if test="${!empty more}">
		<dht:moreLink more="${more}"/>
	</c:if>
</div>
