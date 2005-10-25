<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>

<f:view>
	<head>
	<title>Group: <h:outputText value="#{viewgroup.name}"/></title>
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
	<div class="person">
		<strong>Group: <h:outputText value="#{viewgroup.name}"/></strong>
    	<br/>
		<div class="shared-links">	
		<p>Links recently posted to the group:</p>	
		<table>
		<c:forEach items="${viewgroup.postInfos}" var="info">
		    <tr>
			    <td colspan="2">
			    <strong><a href="${info.url}"><c:out value="${info.title}"/></a></strong>
			    </td>
			</tr>
			<tr>
				<th align="right">From:</th>
				<td>
				<dh:entity value="${info.posterInfo}"/>
				(<fmt:formatDate value="${info.post.postDate}" type="both"/>)
				</td>
			</tr>
			<tr>
			    <th align="right">To:</th>
			    <td><dh:entityList value="${info.recipients}"/></td>
		    </tr>
		    <tr>
		    	<th></th>
		    	<td><c:out value="${info.post.text}"/></td>
		    </tr>
		</c:forEach>
		</table>
		</div>
		<p><strong>Members:</strong> <dh:entityList value="${viewgroup.members}"/></p>
		<c:if test="${viewgroup.isMember}">
		<p><a href="javascript:leaveGroup()">Leave <c:out value="${viewgroup.name}"/></a></p>
		</c:if>
		<c:if test="${!viewgroup.isMember}">
		<p><a href="javascript:joinGroup()">Join <c:out value="${viewgroup.name}"/></a></p>
		</c:if>
	</div>	
	</body>
</f:view>
</html>
