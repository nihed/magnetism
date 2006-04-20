<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:sidebarColumn>
	<dht:sidebarBoxProfile/>
	<%-- any extra boxes go here --%>
	<jsp:doBody/>
	<dht:sidebarBoxGroups/>
	<dht:sidebarBoxFriends/>
</dht:sidebarColumn>
