<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="java.lang.String" %>
<%@ attribute name="asOthersWouldSee" required="false" type="java.lang.Boolean" %>

<c:if test="${showSidebar}"> <%-- set in twoColumnPage tag --%>
	<dht:requirePersonBean who="${who}" asOthersWouldSee="${asOthersWouldSee}"/>
	
	<dht:sidebarColumn>
		<dht:sidebarBoxProfile/>
		<%-- any extra boxes go here --%>
		<jsp:doBody/>
		<dht:sidebarBoxGroups/>
		<dht:sidebarBoxFriends/>
	</dht:sidebarColumn>
</c:if>
