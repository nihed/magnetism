<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewperson" class="com.dumbhippo.web.ViewPersonPage" scope="request"/>
<jsp:setProperty name="viewperson" property="viewedPersonId" param="personId"/>

<c:set var="personName" value="${viewperson.person.humanReadableName}" scope="page"/>
<c:set var="personId" value="${viewperson.person.person.id}" scope="page"/>

<head>
	<title><c:out value="${personName}"/></title>
	<link rel="stylesheet" href="/css/person.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header><c:out value="${personName}"/></dht:header>
    
    <dht:toolbar> &#151;
    	<c:choose>
    		<c:when test="${viewperson.isContact}">
    			<a href='javascript:dh.actions.removeContact("${personId}")'>Remove <c:out value="${personName}"/> from my contact list</a>
	    	</c:when>
    		<c:otherwise>
				<a href='javascript:dh.actions.addContact("${personId}")'>I know <c:out value="${personName}"/></a>
			</c:otherwise>
		</c:choose>
	</dht:toolbar>

	<div class="main">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool Shared Links</strong>
				<c:forEach items="${viewperson.posts}" var="post">
					<dht:postBubble post="${post}"/>
				</c:forEach>
			</div>
		</td>
		<td>
			<div class="groups">
				<strong>Groups:</strong><br/>
				<dh:entityList value="${viewperson.groups}"/>
			</div>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
