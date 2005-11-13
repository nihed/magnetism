<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="groupphoto" class="com.dumbhippo.web.GroupPhotoPage" scope="request"/>
<jsp:setProperty name="groupphoto" property="viewedGroupId" param="groupId"/>

<c:if test="${empty groupphoto.viewedGroupId}">
	<dht:errorPage>No group found; something went wrong</dht:errorPage>
</c:if>

<c:if test="!${groupphoto.canModify}">
	<dht:errorPage>You can't change the photo for this group; maybe you aren't in the group?</dht:errorPage>
</c:if>

<head>
	<title>Changing Photo for <c:out value="${groupphoto.name}"/></title>
	<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
</head>
<body>
	<dht:header>
		Changing Photo for <c:out value="${groupphoto.name}"/>
	</dht:header>
	<dht:toolbar/>

	<div id="dhMain">
		<br/>
		<br/>
		<dht:uploadPhoto location="/groupshots" groupId="${groupphoto.viewedGroupId}"/>
	</div>
</body>
</html>
