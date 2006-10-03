<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/home.css"/>
	<dht:scriptIncludes/>
	<script src="/javascript/${buildStamp}/dh/stacker.js" type="text/javascript"></script>	
	<dht:faviconIncludes/>
</head>


<dht3:requirePersonBean/>
<jsp:setProperty name="person" property="viewedUserId" value="${signin.user.id}"/>

<body class="dh-gray-background-page dh-home-page">
	<div id="dhPage">
		<dht3:header/>
		<div class="dh-page-title-container">
		<dht3:pageTitle><c:out value="${person.viewedPerson.name}"/>'s Overview</dht3:pageTitle>
		<dht3:standardPageOptions selected="Overview"/>
		</div>
		<dht3:shinyBox color="grey">
			<dht3:personHeader who="${person.viewedPerson}" isSelf="true"><a href="/account">Edit my Mugshot account</a></dht3:personHeader>
			<dht3:stacker stack="${person.stack}"/>
		</dht3:shinyBox>
		
		<c:choose>
			<c:when test="${person.contacts.size > 0}">
				<c:forEach items="${person.contactStacks.list}" end="2" var="personStack">
					<dht3:shinyBox color="grey">				
						<dht3:personHeader who="${personStack.contact}" isSelf="false">
					        <c:choose>
			    		        <c:when test="${personStack.contact.contact != null}">
			    			        <dht:actionLink oneLine="true" href="javascript:dh.actions.removeContact('${personStack.contact.viewPersonPageId}')" title="Remove this person from your friends list">Remove from friends</dht:actionLink>
				    	        </c:when>
						        <c:otherwise>
									<dht:actionLink oneLine="true" href="javascript:dh.actions.addContact('${personStack.contact.viewPersonPageId}')" title="Add this person to your friends list">Add to friends</dht:actionLink>
								</c:otherwise>
							</c:choose>	| <a href="/">Invite to a group</a>
						</dht3:personHeader>
						<dht3:stacker stack="${personStack.stack}"/>							
					</dht3:shinyBox>
				</c:forEach>
				<c:if test="${person.contacts.size > 3}">
			       <dht:moreLink moreName="More (${person.contacts.size})" more="${url}"/>
				</c:if>  
			</c:when>
			<c:otherwise>
			    <c:choose>
				    <c:when test="${person.signin.user.account.invitations > 0}">
					    <p>Email <a href="/invitation">invites</a> to some friends</p>
				    </c:when>
				    <c:otherwise>
					    <p>No Mugshot friends?  Try using the search box to find people you may know by email address,
					       or <a href="/groups">browse public groups</a>.</p>
				    </c:otherwise>
			    </c:choose>
			</c:otherwise>
		</c:choose>		
	</div>
</body>
</html>
