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
        dojo.require("dh.util");
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
</dht:body>
</html>
