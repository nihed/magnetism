<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="admin" class="com.dumbhippo.web.AdminPage" scope="request"/>

<head>
	<title>Admin Console</title>
	<dht:scriptIncludes/>
	<script type="text/javascript">
        dojo.require("dh.util");
	</script>
<body>

<div id="dhContainer">
<h2>Current live users: </h2>
  <c:forEach items="${admin.liveUsers}" var="user">
    <c:out value="${user.liveUser.userId}"/> (<c:out value="${user.name}"/>)  hotness: <c:out value="${user.liveUser.hotness}"/><br/>
  </c:forEach>
<h2>Current live posts: </h2>
  <c:forEach items="${admin.livePosts}" var="post">
    <c:out value="${post.postId}"/>  score: <c:out value="${post.score}"/><br/>
  </c:forEach>
</div>
</body>
</html>
