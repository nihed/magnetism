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
	<dht:stylesheets href="person.css" />
	<dht:scriptIncludes/>
</head>
<body>
    <dht:header>
    	<c:choose>
    		<c:when test="${viewperson.disabled}">
    			Account not active
    		</c:when>
    		<c:otherwise>
		    	<c:out value="${personName}"/>
		    </c:otherwise>
		</c:choose>
    </dht:header>
    
    <dht:toolbar>
    	<c:if test="${!viewperson.disabled}">
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
		</c:if>
	</dht:toolbar>

	<c:if test="${!viewperson.disabled}">
		<div class="person">
			<dht:headshot person="${viewperson.person}" size="128"/>
			<c:out value="${personName}"/>
		</div>
	</c:if>
	
	<div id="dhMain">
		<c:choose>
			<c:when test="${viewperson.disabled}">
				This page is disabled.
				<c:if test="${viewperson.self}">
					<a href="javascript:dh.actions.setAccountDisabled(false);">Enable it again</a>
				</c:if>
			</c:when>
			<c:otherwise>
				<table class="dh-main-table">
				<tr>
				<td class="dh-post-list-td">
					<c:if test="${viewperson.posts.size > 0}">
						<div class="shared-links">	
							<strong>Cool Shared Links</strong>
							<dht:postList posts="${viewperson.posts.list}" maxPosts="${viewperson.maxPostsShown}" posterId="${personId}" posterName="${personName}"/>
						</div>
					</c:if>
				</td>
				<td>
					<c:if test="${viewperson.groups.size > 0}">
						<div class="groups">
							<strong>Groups:</strong><br/>
							<dh:entityList value="${viewperson.groups.list}" photos="true"/>
						</div>
					</c:if>
				</td>
				</tr>
				</table>
			</c:otherwise>
		</c:choose>
	</div>
</body>
</html>
