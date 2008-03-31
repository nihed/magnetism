<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="admin" class="com.dumbhippo.web.pages.AdminPage" scope="request"/>
<c:set var="mode" value='${param["mode"]}' scope="page"/>
<c:if test="${!admin.valid}">
	<dht2:errorPage>Permission Denied</dht2:errorPage>
</c:if>

<head>
	<title>Admin Console</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/admin.css"/>	
	<dh:script module="dh.admin"/>
</head>
<body>

<c:choose>
<c:when test="${empty mode}">
<div><a href="?mode=shell">Interactive Shell</a></div>
<div><a href="?mode=live">Live State Dump</a></div>
<div><a href="?mode=users">All User Controls</a></div>
</c:when>
<c:when test="${mode == 'shell'}">
<div><a href="?mode=live">Live State Dump</a></div>
<div><a href="?mode=users">All User Controls</a></div>
<h2>Shell on <c:out value="${pageContext.request.localAddr}" escapeXml="true"/></h2>
<div id="dhAdminSamples">
Important variables:<br/>
<i>server</i> - Object with a few handy methods (see reflection)<br/>
<i>em</i> - The EntityManager for this transaction<br/>
<i>out</i> - A PrintWriter you can use to print things<br/>
Sample commands:<br/>
<span class="dh-admin-sample-code">r = 1+1;</span> -> <b>2</b><br/>
<span class="dh-admin-sample-code">r;</span> -> <b>2</b><br/>
<span class="dh-admin-sample-code">out.println("Hello World");</span> -> <b>void</b><br/>
<span class="dh-admin-sample-code">me = user("walters@redhat.com");</span> -> <b>User@12345</b><br/>
<span class="dh-admin-sample-code">me.getGuid();</span> -> <b>52fx2341v33</b><br/>
<span class="dh-admin-sample-code">user("52fx2341v33");</span> -> <b>User@12345</b><br/>
<span class="dh-admin-sample-code">me.getAccount().setMySpaceName("foo");</span> -> <b>void</b><br/>
<span class="dh-admin-sample-code">em.createQuery("from Group where name like 'Red Hat'").getResultList();</span> -> <b>List&lt;Group&gt;</b><br/>
<span class="dh-admin-sample-code">ejb("InvitationSystem").getInvitations(me);</span> -> <b>4</b><br/>
<div>More examples on the <a href="http://developer.mugshot.org/wiki/Server_Admin">Server Admin</a> page.</div>
</div>
<div id="dhAdminShellMessage"></div>
<div>
<div><textarea rows="8" cols="70" accesskey="i" id="dhAdminShellInput" onkeypress="dh.admin.shell.queueParseCheck();"></textarea></div>
<div><input id="dhAdminShellEvalButton" accesskey="e" type="button" value="Eval" onclick="dh.admin.shell.doEval();"/>
     <input id="dhAdminShellTransactionCheck" name="dhAdminShellTransactionCheck"
            accesskey="t" type="checkbox" checked="true"/> <label for="dhAdminShellTransactionCheck">Use transaction</label>
</div>
</div>
<div id="dhAdminShellResultArea">
<div id="dhAdminShellResult" class="dh-admin-shell-result"><p style="color: grey;">Result</p></div>
<div id="dhAdminShellResultReflection" class="dh-admin-shell-result"><p style="color: grey;">Reflection</p></div>
</div>
<h3>Output Stream</h3>
<textarea rows="10" cols="70" id="dhAdminShellOutput"></textarea>
<textarea rows="10" cols="70" id="dhAdminShellTrace" style="display: none;"></textarea>

<hr/>
<h2>New Features</h2>
New features flag is <b><c:out value="${admin.newFeatures}"/></b>
<a href="javascript:dh.admin.setNewFeatures('${!admin.newFeatures}')">Toggle</a>
</p>

<h2>Search</h2>
<p>
<a href="javascript:dh.admin.reindexAll();">Rebuild all Lucene indexes</a>
</p>
</c:when>
<c:when test="${mode == 'live'}">
<div><a href="?mode=shell">Interactive Shell</a></div>
<div><a href="?mode=users">All User Controls</a></div>
<h2>Site Stats</h2>
<p>
<b><c:out value="${admin.numberOfAccounts}"/> active accounts</b>
</p>
<p><c:out value="${admin.systemInvitations}"/> public invitations, 
<c:out value="${admin.userInvitations}"/> invitations allocated to users, 
<c:out value="${admin.totalInvitations}"/> total invitations.
</p>

<h2>Cached live users: </h2>
<p><c:out value="${admin.cachedLiveUsersCount}"/> cached users</p>
<p>
  <c:forEach items="${admin.cachedLiveUsers}" var="user">
	<dht:liveUserDebug user="${user}"/>
  </c:forEach>
</p>
</c:when>
<c:when test="${mode == 'users'}">
<div><a href="?mode=shell">Interactive Shell</a></div>
<div><a href="?mode=live">Live State Dump</a></div>
<h2>All users: </h2>
<p>
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
			<td><a href="/person?who=${user.user.id}">&#187;</a></td>
		</tr>
  	</c:forEach>
</table>
</p>
</c:when>
<c:otherwise>
  Unrecognized mode!  Must be driver error.
</c:otherwise>
</c:choose>
</body>
</html>
