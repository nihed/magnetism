<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="alwaysShowSidebar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="neverShowSidebar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>
<%@ attribute name="logoOnly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="topMessageHtml" required="false" type="java.lang.String" %>
<%@ attribute name="disableFooter" required="false" type="java.lang.Boolean" %>
<%@ attribute name="currentPageLink" required="false" type="java.lang.String" %>

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

<c:choose>
	<c:when test="${showSidebar}">
		<c:set var="bodyClass" value="dh-body-with-sidebar" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="bodyClass" value="dh-body-without-sidebar" scope="page"/>
	</c:otherwise>
</c:choose>

<%-- not using dht:body because we need to get the header outside the dhPage to work with the new tags/3/header --%>
<body class="${bodyClass} dh-gray-background-page">
	<c:choose>
		<c:when test="${logoOnly}">
			<center><div id="dhPageHeader"><dht:logo/></div></center>
		</c:when>
		<c:otherwise>
			<dht:header currentPageLink="${currentPageLink}" searchText="${searchText}"/>
		</c:otherwise>
	</c:choose>
	<div id="dhPage">
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
		<c:choose>
			<c:when test="${disableFooter}">
			</c:when>
			<c:otherwise>
				<dht:footer/>
			</c:otherwise>
	    </c:choose>
	</div>
	<dht3:analytics/>
</body>
