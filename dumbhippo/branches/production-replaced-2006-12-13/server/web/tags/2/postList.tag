<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="posts" required="true" type="java.util.List" %>
<%@ attribute name="format" required="true" type="java.lang.String" %>
<%@ attribute name="separators" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enumerateStart" required="false" type="java.lang.Integer" %>
<%@ attribute name="favesMode" required="false" type="java.lang.String" %>

<c:forEach items="${posts}" var="post" varStatus="status">
	<c:choose>
		<c:when test="${format == 'simple'}">
			<dht:simplePostLink post="${post}"/>
		</c:when>
		<c:when test="${format == 'enumerated'}">
			<dht:post post="${post}" includeExtra="false" numeral="${status.index + enumerateStart}"/>
		</c:when>
		<c:when test="${format == 'full'}">
			<dht:post post="${post}" favesMode="${favesMode}"/>
		</c:when>
		<c:when test="${format == 'full-with-photos'}">
			<dht:post post="${post}" favesMode="${favesMode}" showPhoto="true"/>
		</c:when>
		<c:otherwise>
			<dht:errorPage>Unknown post list format</dht:errorPage>
		</c:otherwise>
	</c:choose>
	<c:if test="${separators && !status.last}">
		<dht:zoneBoxSeparator/>
	</c:if>
</c:forEach>
