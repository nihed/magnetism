<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="onAccountPage" required="false" type="java.lang.Boolean" %>

<c:if test="${showSidebar}"> <%-- set in twoColumnPage tag --%>	
	<dht:sidebarColumn>
		<dht:sidebarBoxProfileGroup onAccountPage="${onAccountPage}"/>
		<dht:sidebarBoxesGroupMembers/>
	</dht:sidebarColumn>
</c:if>
