<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="admin" class="com.dumbhippo.web.pages.AdminPage" scope="request"/>

<c:if test="${!admin.valid}">
	<dht:errorPage>Permission Denied</dht:errorPage>
</c:if>

<head>
	<title>Admin Console</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/admin.css"/>	
	<dht:scriptIncludes/>
	<script type="text/javascript">
        dojo.require("dh.admin");
	</script>
</head>
<dht:body>

<h2>Shell</h2>
<div id="dhAdminSamples">Sample commands:<br/>
<span class="dh-admin-sample-code">r = 1+1;</span> -> <b>2</b><br/>
<span class="dh-admin-sample-code">r;</span> -> <b>2</b><br/>
<span class="dh-admin-sample-code">me = user("walters@redhat.com");</span> -> <b>User@12345</b><br/>
<span class="dh-admin-sample-code">me.getGuid();</span> -> <b>52fx2341v33</b><br/>
<span class="dh-admin-sample-code">user("52fx2341v33");</span> -> <b>User@12345</b><br/>
<span class="dh-admin-sample-code">me.getAccount().setMySpaceName("foo")</span> -> <b>void</b><br/>
<span class="dh-admin-sample-code">server.getEJB("InvitationSystem").getInvitations(me);</span> -> <b>4</b><br/>
</div>
<div id="dhAdminShellMessage"></div>
<div>
<div><textarea rows="8" cols="70" accesskey="i" id="dhAdminShellInput" onkeypress="dh.admin.shell.queueParseCheck();"></textarea></div>
<div><input id="dhAdminShellEvalButton" accesskey="e" type="button" value="Eval" onclick="dh.admin.shell.doEval();"/></div>
</div>
<div id="dhAdminShellResultArea">
<div id="dhAdminShellResult" class="dh-admin-shell-result"><p style="color: grey;">Result</p></div>
<div id="dhAdminShellResultReflection" class="dh-admin-shell-result"><p style="color: grey;">Reflection</p></div>
</div>
<h3>Output Stream</h3>
<textarea rows="10" cols="70" id="dhAdminShellOutput"></textarea>
<textarea rows="10" cols="70" id="dhAdminShellTrace" style="display: none;"></textarea>

<hr/>

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
			<td><jsp:element name="a">
				<jsp:attribute name="href">
					javascript:dh.admin.shell.insertUser('<c:out value="${user.user.id}"/>');
				</jsp:attribute>
				<jsp:body>
					<c:out value="${user.user.id}"/>
				</jsp:body>
				</jsp:element>
				</td>  		
			<td><a href="javascript:dh.admin.sendRepairEmail('${user.user.id}')">Send repair email</a></td>
			<td>
				<c:choose>
					<c:when test="${user.account.adminDisabled}">
						<a href="javascript:dh.admin.setAdminDisabled('${user.user.id}', false)">Enable</a>
					</c:when>
					<c:otherwise>
						<a href="javascript:dh.admin.setAdminDisabled('${user.user.id}', true)">Disable</a>
					</c:otherwise>
				</c:choose>
			</td>
		</tr>
  	</c:forEach>
</table>
</p>
</dht:body>
</html>
