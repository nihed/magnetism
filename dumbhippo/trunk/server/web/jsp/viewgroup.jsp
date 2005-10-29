<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<jsp:useBean id="signin" class="com.dumbhippo.web.SigninBean" scope="session"/>
<jsp:useBean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="signin" value="${signin}"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<head>
	<title><c:out value="${viewgroup.name}"/></title>
	<link rel="stylesheet" href="/css/group.css" type="text/css" />
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dh.server");
	    
	    function joinGroup() {
	    	dh.server.doPOST("joingroup",
						     { "groupId" : "${viewgroup.viewedGroupId}" },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't join group");
				  	    	 });
	    }
	    function leaveGroup() {
	    	dh.server.doPOST("leavegroup",
						     { "groupId" : "${viewgroup.viewedGroupId}" },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't leave group");
				  	    	 });
	    }
	    
    </script>
</head>
<body>
	<div class="header">
	<table>
		<tr>
		<td><span class="first-letter dh">D</span><span class="dh">umb</span><span class="first-letter dh">H</span><span class="dh">ippo</span></td>
		<td class="right"><a href=""><c:out value="${viewgroup.name}"/></a></td>
		</tr>
	</table>
	</div>
	<div class="toolbar">
	<c:url value="sharelink?next=viewgroup" var="share"/>
	Do It: <a href="${share}">&#187; Share</a> &#151; <a href="/jsf/home.faces">Your Page</a> &#151;
		<c:if test="${viewgroup.isMember}"><a href="javascript:leaveGroup()">Leave <c:out value="${viewgroup.name}"/></a></c:if>
		<c:if test="${!viewgroup.isMember}"><a href="javascript:joinGroup()">Join <c:out value="${viewgroup.name}"/></a></c:if>
	</div>

	<div class="main">

	<table>
	<tr><td>

	<div class="shared-links">	
		<strong>Cool New Links</strong>
		<c:forEach items="${viewgroup.postInfos}" var="info">

		<div class="cool-bubble-shadow">		
		<table class="cool-bubble">
		<tr>
		    <td class="cool-person" rowSpan="3">
			<a class="cool-person" href="">
			<img class="cool-person" src="/files/headshots/${info.posterInfo.person.id}" />
			<br/>
			<dh:entity value="${info.posterInfo}"/>
			</a>
		    </td>
		    <td class="cool-link">
			<div class="cool-link">
			<a class="cool-link" title="${info.url}" href="${info.url}"><c:out value="${info.title}"/></a>
			</div>
		    </td>
		</tr>
		<tr>
		    <td class="cool-link-desc">
			<c:out value="${info.post.text}"/>
		    </td>
		</tr>
		<tr>
		    <td class="cool-link-meta">
			<div class="cool-link-date">
				(<fmt:formatDate value="${info.post.postDate}" type="both"/>)
			</div>
			<div class="cool-link-to">
				<dh:entityList value="${info.recipients}"/>
			</div>
		   </td>
		</tr>
		</table>
		</div>
		</c:forEach>

	</div>
	

	</td>
	<td>
	<div class="group-members">
	<strong>Members:</strong>
	<br/>
	<dh:entityList value="${viewgroup.members}"/>
	</div>
	</td>
	</tr>
	</table>
	</div>

</body>
</html>
