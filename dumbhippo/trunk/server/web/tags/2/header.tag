<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="kind" required="false" type="java.lang.String" %>
<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<c:if test="${empty kind}">
	<c:choose>
		<c:when test="${showSidebar}">
			<c:set var="kind" scope="page" value="withSidebar"/>
		</c:when>
		<c:otherwise>
			<c:set var="kind" scope="page" value="withoutSidebar"/>
		</c:otherwise>
	</c:choose>
</c:if>

<c:choose>
	<c:when test="${kind == 'main'}">
		<c:set var="headerImage" value="/images2/mughdr710x110.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-tall" scope="page"/>
	</c:when>
	<c:when test="${kind == 'withSidebar'}">
		<c:set var="headerImage" value="/images2/mughdr710x65.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-short" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="headerImage" value="/images2/mughdr500x65.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-short" scope="page"/>
	</c:otherwise>
</c:choose>

<div id="dhPageHeader" class="${headerHeightClass}">
	<img id="dhPageHeaderImage" src="${headerImage}" />
	<div id="dhHeaderControls">
		<div id="dhHeaderLinks">
			<c:choose>
				<c:when test="${signin.valid}">
					<c:if test="${!disableHomeLink}"><a href="/">HOME</a> | </c:if><dht:actionLinkLogout/>
				</c:when>
				<c:otherwise>
					<a href="/signup">Sign up</a> | <a href="/who-are-you">Log in</a>
				</c:otherwise>
			</c:choose>
		</div>
		<div id="dhSearchBox">
			<form action="/search" method="get">
				Search: 
				<jsp:element name="input">
					<jsp:attribute name="type">text</jsp:attribute>
					<jsp:attribute name="class">dh-text-input</jsp:attribute>
					<jsp:attribute name="name">q</jsp:attribute>
					<jsp:attribute name="value">${searchText}</jsp:attribute>
				</jsp:element>
				<input type="submit" value="Go"/>
			</form>
		</div>
	</div>		
</div>
