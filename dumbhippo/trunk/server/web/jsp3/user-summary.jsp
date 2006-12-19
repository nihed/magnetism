<%-- This page is used for iframe badges, it's not a regular page. Don't use any tags or stylesheets that assume a normal page. --%>
<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${empty person}">
	<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.UserSummaryPage"/>
	<jsp:setProperty name="person" property="needExternalAccounts" value="true"/>
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Summary - Mugshot</title>
	<%-- Don't include the stylesheets for "normal" pages, makes things too confusing. Keep this standalone.
	     Also it's good to load in one http get since we can't assume someone browsing to a random blog 
	     has any of our css/js already cached. --%>
	<dht:faviconIncludes/>
</head>

<body>

	<div>
		<dht:headshot person="${person.viewedPerson}" size="60"/>
	</div>

	<div>
		<c:out value="${person.viewedPerson.name}"/>
	</div>

	<div>
		<c:out value="${person.viewedPerson.onlineIcon}"/>
	</div>

	<c:forEach items="${person.pageableMugshot.results}" var="block">
		<div>
			<c:out value="${block.summaryHeading}"/>
		</div>
		<div>
			<jsp:element name="a">
				<jsp:attribute name="href"><c:out value="${block.summaryLink}"/></jsp:attribute>
				<jsp:body>
					<c:out value="${block.summaryLinkText}"/>
				</jsp:body>
			</jsp:element>
		</div>
	</c:forEach>

	<c:forEach var="account" items="${person.viewedPerson.lovedAccounts.list}">
		<div>
			<c:out value="${account.externalAccount.siteName}"/>
		</div>
		<div>
			<c:out value="${account.externalAccount.linkText}"/>
		</div>
		<div>
			<c:out value="${account.link}"/>
		</div>
		<div>
			<c:out value="/images3/${buildStamp}/${account.externalAccount.iconName}"/>
		</div>
	</c:forEach>

</body>

</html>
