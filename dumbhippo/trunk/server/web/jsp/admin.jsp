<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="admin" class="com.dumbhippo.web.AdminPage" scope="request"/>

<c:if test="${!admin.valid}">
	<dht:errorPage>Permission Denied</dht:errorPage>
</c:if>

<head>
	<title>Admin Console</title>
	<dht:scriptIncludes/>
	<script type="text/javascript">
        dojo.require("dh.admin");
	</script>
</head>
<dht:body>

<h2>Site Stats</h2>
<p>
<b><c:out value="${admin.numberOfAccounts}"/> active accounts</b>
</p>
<p><c:out value="${admin.systemInvitations}"/> public invitations, 
<c:out value="${admin.userInvitations}"/> invitations allocated to users, 
<c:out value="${admin.totalInvitations}"/> total invitations.
</p>

<h2>Available live users: </h2>
<p><c:out value="${admin.availableLiveUsersCount}"/> available users</p>
<p>
  <c:forEach items="${admin.availableLiveUsers}" var="user">
	<dht:liveUserDebug user="${user}"/>
  </c:forEach>
</p>
<h2>Cached live users: </h2>
<p><c:out value="${admin.cachedLiveUsersCount}"/> cached users</p>
<p>
  <c:forEach items="${admin.cachedLiveUsers}" var="user">
	<dht:liveUserDebug user="${user}"/>
  </c:forEach>  
</p>
<h2>Cached live posts: </h2>
<p><c:out value="${admin.livePostsCount}"/> live posts</p>
<p>
  <c:forEach items="${admin.livePosts}" var="post">
  	<dht:livePostDebug post="${post}"/>
  </c:forEach>
</p>
<h2>All users: </h2>
<table>
    <tr>
      	<th></th>
    	<th>email</th>
    	<th>status</th>
     	<th>id</th>
	</tr>
	<c:forEach items="${admin.allUsers}" var="user">
  		<tr>
			<th align="left"><c:out value="${user.name}"/></th>
		    <td><c:out value="${user.email.email}"/></td>
			<td>
  				<c:choose>
  					<c:when test="${user.account.disabled}">
  						disabled
	  				</c:when>
	  				<c:when test="${!user.account.wasSentShareLinkTutorial}">
  						web-only
  					</c:when>
	  				<c:otherwise>
  						active
	  				</c:otherwise>
		  		</c:choose>
			</td>
			<td><c:out value="${user.user.id}"/></td>  		
			<td><a href="javascript:dh.admin.sendRepairEmail('${user.user.id}')">Send repair email</a></td>
		</tr>
  	</c:forEach>
</table>
</p>
</dht:body>
</html>
