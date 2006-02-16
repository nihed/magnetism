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
<body>

<div id="dhContainer">
<h2>Available live users: </h2>
  <c:forEach items="${admin.cachedLiveUsers}" var="user">
	<dht:liveUserDebug user="${user}"/>
  </c:forEach>
<h2>Cached live users: </h2>
  <c:forEach items="${admin.availableLiveUsers}" var="user">
	<dht:liveUserDebug user="${user}"/>
  </c:forEach>  
<h2>Cached live posts: </h2>
  <c:forEach items="${admin.livePosts}" var="post">
  	<dht:livePostDebug post="${post}"/>
  </c:forEach>
</div>
<dht:bottom/>
</body>
</html>
