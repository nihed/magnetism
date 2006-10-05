<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="false" type="java.lang.String" %>
<%@ attribute name="mugshot" required="false" type="java.lang.Boolean" %>

<c:if test="${empty person}">
	<dh:bean id="person" class="com.dumbhippo.web.pages.StackedPersonPage" scope="request"/>
	<jsp:setProperty name="person" property="needExternalAccounts" value="true"/>	
	<%-- this setter is not idempotent so don't call it if someone's going to call it again later --%>
	<c:choose>
		<c:when test="${!empty param['who']}">
			<jsp:setProperty name="person" property="viewedUserId" param="who"/>
		</c:when>
		<c:when test="${!empty who}">
			<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>
		</c:when>
		<c:otherwise>
			<jsp:setProperty name="person" property="viewedUserId" value="${signin.userId}"/>
		</c:otherwise>
	</c:choose>	
</c:if>
