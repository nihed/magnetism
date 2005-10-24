<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<f:view>
	<head>
	<title><h:outputText value="#{viewperson.personInfo.humanReadableName}"/></title>
	</head>
	<script src="/javascript/config.js" type="text/javascript"></script>
    <script src="/javascript/dojo/dojo.js" type="text/javascript"></script>
    <script src="/javascript/common.js" type="text/javascript"></script>
    <script type="text/javascript">
	    dojo.require("dh.server");
	    
	    function addContact() {
	    	dh.server.doPOST("addcontactperson",
						     { "contactId" : "${viewperson.personInfo.person.id}" },
				  	    	 function(type, data, http) {
				  	    	 	 document.location.reload();
				  	    	 },
				  	    	 function(type, error, http) {
				  	    	     alert("Couldn't add user to contact list");
				  	    	 });
	    }
    </script>
	<body>
	<div class="person">
		<strong><h:outputText value="#{viewperson.personInfo.humanReadableName}"/></strong>
    	<br/>
		<div class="shared-links">	
		<p>Recently posted links:</p>	
		<table>
		<c:forEach items="${viewperson.postInfos}" var="info">
		    <tr>
			    <td colspan="2">
			    <strong><a href="${info.url}"><c:out value="${info.title}"/></a></strong>
			    (<fmt:formatDate value="${info.post.postDate}" type="both"/>)
			    </td>
			</tr>
			<tr>
			    <th align="right">To:</th>
			    <td><c:out value="${info.recipientSummary}"/></td>
		    </tr>
		    <tr>
		    	<th></th><td><c:out value="${info.post.text}"/></td>
		    </tr>
		</c:forEach>
		</table>
		</div>
		<br/>
		<div class="groups">
		<!--  list group objects here -->
		<!--  ability to join/share groups [D] -->
		</div>
		<c:if test="${!viewperson.isContact}">
		<p><a href="javascript:addContact()">Add <c:out value="${viewperson.personInfo.humanReadableName}"/> to my contact list</a></p>
		</c:if>
	</div>	
	</body>
</f:view>
</html>
