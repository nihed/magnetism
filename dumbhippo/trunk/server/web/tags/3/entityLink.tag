<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.EntityView" %>
<%@ attribute name="onlineIcon" required="false" type="java.lang.Boolean" %>
<%@ attribute name="imageOnly" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${dh:myInstanceOf(who, 'com.dumbhippo.server.views.PersonView')}">
		<a class="dh-person-link dh-underlined-link" href="/person?who=${who.viewPersonPageId}"><c:choose><c:when test="${imageOnly}"><dh:png src="${who.photoUrl30}" style="width: 30px; height: 30px"/></c:when><c:otherwise><c:out value="${who.name}"/><c:if test="${onlineIcon}"> <dht3:presenceIcon who="${who}"/></c:if></c:otherwise></c:choose></a>
	</c:when>
	<c:when test="${dh:myInstanceOf(who, 'com.dumbhippo.server.views.GroupView')}">
		<a class="dh-group-link dh-underlined-link" href="${who.homeUrl}"><c:choose><c:when test="${imageOnly}"><dh:png src="${who.photoUrl30}" style="width: 30px; height: 30px"/></c:when><c:otherwise><c:out value="${who.name}"/></c:otherwise></c:choose></a>
	</c:when>	
	<c:otherwise>
	</c:otherwise>
</c:choose>