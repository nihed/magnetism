<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<link rel="stylesheet" href="/css/group.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
	<dht:header><c:out value="${viewgroup.name}"/></dht:header>
	
	<dht:toolbar> &#151;
		<c:choose>
			<c:when test="${viewgroup.isMember}">
				<a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>Leave <c:out value="${viewgroup.name}"/></a>
			</c:when>
			<c:otherwise>
				<a href='javascript:dh.actions.joinGroup("${viewgroup.viewedGroupId}")'>Join <c:out value="${viewgroup.name}"/></a>
			</c:otherwise>
		</c:choose>
	</dht:toolbar>

	<div class="main">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool New Links</strong>
				<c:forEach items="${viewgroup.postInfos}" var="info">
				<dht:postBubble info="${info}"/>
			</div>
		</td>
		<td>
			<div class="group-members">
				<strong>Members:</strong><br/>
				<dh:entityList value="${viewgroup.members}"/>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
