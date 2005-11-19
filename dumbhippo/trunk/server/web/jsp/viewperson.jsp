<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewperson" class="com.dumbhippo.web.ViewPersonPage" scope="request"/>
<jsp:setProperty name="viewperson" property="viewedPersonId" param="personId"/>

<c:if test="${!viewperson.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:set var="personName" value="${viewperson.person.name}" scope="page"/>
<c:set var="personId" value="${viewperson.person.viewPersonPageId}" scope="page"/>

<head>
	<title><c:out value="${personName}"/></title>
	<dht:stylesheets href="/css/person.css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header><c:out value="${personName}"/></dht:header>
    
    <dht:toolbar>
    	<c:choose>
    		<c:when test="${viewperson.self}">
    		</c:when>
    		<c:when test="${viewperson.contact}">
    			&#151;
    			<a href='javascript:dh.actions.removeContact("${personId}")'>Remove <c:out value="${personName}"/> from my contact list</a>
	    	</c:when>
    		<c:otherwise>
	    		&#151;
				<a href='javascript:dh.actions.addContact("${personId}")'>I know <c:out value="${personName}"/></a>
			</c:otherwise>
		</c:choose>
	</dht:toolbar>

	<div class="person">
		<dht:png klass="cool-person" src="/files/headshots/${personId}" />
		<c:out value="${personName}"/>
	</div>

	<div id="dhMain">
		<table>
		<tr>
		<td>
			<div class="shared-links">	
				<strong>Cool Shared Links</strong>
				<dht:postList posts="${viewperson.posts}" maxPosts="${viewperson.maxPostsShown}" posterId="${personId}" posterName="${personName}"/>
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
