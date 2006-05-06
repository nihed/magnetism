<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<div id="dhPageHeader">
	<dht:logo/>
	<div id="dhHeaderLinks">
		<c:choose>
			<c:when test="${signin.valid}">
				<c:if test="${!disableHomeLink}"><a href="/home">HOME</a> | </c:if><dht:actionLinkLogout/>
			</c:when>
			<c:otherwise>
				<a href="">Sign up</a> | <a href="">Log in</a>
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
