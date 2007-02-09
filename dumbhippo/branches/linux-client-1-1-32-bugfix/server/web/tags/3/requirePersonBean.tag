<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="false" type="java.lang.String" %>
<%@ attribute name="beanClass" required="true" type="java.lang.String" %>
<%@ attribute name="pickRandomIfAnonymous" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${empty person}">
		<dh:bean id="person" class="${beanClass}" scope="request"/>
		<%-- this setter is not idempotent so don't call it if someone's going to call it again later --%>
		<c:choose>
			<c:when test="${!empty param['who']}">
				<jsp:setProperty name="person" property="viewedUserId" param="who"/>
			</c:when>
			<c:when test="${!empty who}">
				<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test="${signin.valid}">
						<jsp:setProperty name="person" property="viewedUserId" value="${signin.userId}"/>
					</c:when>
					<c:when test="${pickRandomIfAnonymous}">
						<jsp:setProperty name="person" property="randomActiveUser" value="true"/>
					</c:when>
				</c:choose>
			</c:otherwise>
		</c:choose>
		
		<c:if test="${!person.valid}">
			<dht:errorPage>Person not found (log in?)</dht:errorPage>
		</c:if>
	</c:when>
	<c:when test="${!dh:myInstanceOf(person, beanClass)}">
		<dht:errorPage>This page is broken! Person bean ${person.class.name} already loaded but ${beanClass} required</dht:errorPage>
	</c:when>
</c:choose>
