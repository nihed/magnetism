<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${showSidebar}"> <%-- set in twoColumnPage tag --%>	
	<dht:sidebarColumn>
		<dht:sidebarBoxProfileGroup/>
		<dht:sidebarBoxesGroupMembers/>
	</dht:sidebarColumn>
</c:if>
