<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>

<span class="dh-presence">
	<c:choose>
		<c:when test="${who.online}">
			<dh:png src="/images3/${buildStamp}/online_icon.png" style="width: 12px; height: 12px;"/>
		</c:when>
		<c:otherwise>
			<dh:png src="/images3/${buildStamp}/offline_icon.png" style="width: 12px; height: 12px;"/>
		</c:otherwise>
	</c:choose>
</span>	