<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="alwaysShowSidebar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="neverShowSidebar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableSignupLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>
<%@ attribute name="logoOnly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="topMessageHtml" required="false" type="java.lang.String" %>

<c:choose>
	<c:when test="${alwaysShowSidebar}">
		<c:set var="showSidebar" value="true" scope="request"/>
	</c:when>
	<c:when test="${neverShowSidebar}">
		<c:set var="showSidebar" value="false" scope="request"/>
	</c:when>
	<c:otherwise>
		<c:set var="showSidebar" value="${signin.valid}" scope="request"/>
	</c:otherwise>
</c:choose>

<dht:body extraClass="dh-gray-background-page">
	<c:choose>
		<c:when test="${logoOnly}">
			<center><div id="dhPageHeader"><dht:logo/></div></center>
		</c:when>
		<c:otherwise>
			<dht:header disableHomeLink="${disableHomeLink}" disableSignupLink="${disableSignupLink}" searchText="${searchText}"/>
		</c:otherwise>
	</c:choose>
	<c:if test="${!empty topMessageHtml}">
		<div id="dhPageTopMessage">
			<div id="dhPageTopMessageContent">
				<c:out value="${topMessageHtml}" escapeXml="false"/>
			</div>
		</div>
	</c:if>
	<div id="dhPageContent">
		<jsp:doBody/>
	</div>
	<dht:footer/>
</dht:body>
