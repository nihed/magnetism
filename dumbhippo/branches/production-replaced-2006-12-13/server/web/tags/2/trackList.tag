<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="tracks" required="true" type="java.util.List" %>
<%@ attribute name="albumArt" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>
<%@ attribute name="displaySinglePersonMusicPlay" required="false" type="java.lang.Boolean"%>
<%@ attribute name="playItLink" required="false" type="java.lang.Boolean"%>

<c:if test="${empty id}">
	<c:set var="id" value=""/>
</c:if>

<c:if test="${empty albumArt}">
        <c:set var="albumArt" value="false"/>
</c:if>

<c:if test="${empty oneLine}">
        <c:set var="oneLine" value="false"/>
</c:if>

<c:if test="${empty displaySinglePersonMusicPlay}">
	<c:set var="displaySinglePersonMusicPlay" value="false"/>
</c:if>

<c:if test="${empty playItLink}">
	<c:set var="playItLink" value="true"/>
</c:if>

<%--
  Our general rule of thumb for using this section is that if the tracks rely on circumstances not fulfilled 
  like the person having friends or having listened to something then we hide it by default.  The other
  sections don't matter as much because they are global, while they can adhere to this guideline it just
  isn't important since they should _never_ be empty or we lose.
--%>

<c:if test="${!empty tracks}">
	<dht:zoneBoxTitle a="${id}"><c:out value="${name}"/></dht:zoneBoxTitle>
	<c:forEach items="${tracks}" var="track">
		<dht:track track="${track}" albumArt="${albumArt}" oneLine="${oneLine}" displaySinglePersonMusicPlay="${displaySinglePersonMusicPlay}" playItLink="${playItLink}"/>
	</c:forEach>
	<c:if test="${!empty pageable}">
		<dht:expandablePager pageable="${pageable}" anchor="${id}"/>
	</c:if>
	<c:if test="${!empty separator && separator}">
		<dht:zoneBoxSeparator/>
	</c:if>
</c:if>
