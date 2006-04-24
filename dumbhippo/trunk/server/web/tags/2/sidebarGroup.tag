<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="java.lang.String" %>

<c:if test="${showSidebar}"> <%-- set in twoColumnPage tag --%>
	<dh:bean id="sidebar" class="com.dumbhippo.web.SidebarGroupPage" scope="request"/>
	<jsp:setProperty name="sidebar" property="viewedGroupId" value="${who}"/>
	
	<dht:sidebarColumn>
		<%-- FIXME sidebar boxes here --%>
	</dht:sidebarColumn>
</c:if>
