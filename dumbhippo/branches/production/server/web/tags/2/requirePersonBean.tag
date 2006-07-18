<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="false" type="java.lang.String" %>
<%@ attribute name="asOthersWouldSee" required="false" type="java.lang.Boolean" %>
<%@ attribute name="needExternalAccounts" required="false" type="java.lang.Boolean" %>

<c:if test="${empty person}">
	<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="request"/>
	<%-- Important to set needExternalAccounts before the user id so we get the right
    	 PersonView extras --%>
	<jsp:setProperty name="person" property="needExternalAccounts" value="${needExternalAccounts}"/>	
	<jsp:setProperty name="person" property="asOthersWouldSee" value="${asOthersWouldSee}"/>
	<%-- this setter is not idempotent so don't call it if someone's going to call it again later --%>
	<c:if test="${!empty who}">
		<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>
	</c:if>
</c:if>
