<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="tracks" required="true" type="java.util.List" %>
<%@ attribute name="albumArt" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>

<c:if test="${empty id}">
	<c:set var="id" value=""/>
</c:if>

<c:if test="${empty albumArt}">
        <c:set var="albumArt" value="false"/>
</c:if>

<c:if test="${empty oneLine}">
        <c:set var="oneLine" value="false"/>
</c:if>

<dht:zoneBoxTitle a="${id}"><c:out value="${name}"/></dht:zoneBoxTitle>
<c:if test="${!empty tracks}">
	<c:forEach items="${tracks}" var="track">
		<dht:track track="${track}" albumArt="${albumArt}" oneLine="${oneLine}"/>
	</c:forEach>
	<c:if test="${!empty pageable}">
		<dht:expandablePager pageable="${pageable}" anchor="${id}"/>
	</c:if>
</c:if>
<c:if test="${!empty separator && separator}">
	<dht:zoneBoxSeparator/>
</c:if>
