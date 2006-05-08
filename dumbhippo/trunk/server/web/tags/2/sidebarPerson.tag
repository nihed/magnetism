<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="java.lang.String" %>

<c:if test="${showSidebar}"> <%-- set in twoColumnPage tag --%>
	<dht:requirePersonBean who="${who}"/>
	
	<dht:sidebarColumn>
		<dht:sidebarBoxProfile/>
		<%-- any extra boxes go here --%>
		<jsp:doBody/>
		<dht:sidebarBoxGroups/>
		<dht:sidebarBoxFriends/>
	</dht:sidebarColumn>
</c:if>
