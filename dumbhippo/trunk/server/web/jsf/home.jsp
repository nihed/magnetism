<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<f:view>
	<head>
	<title><h:outputText value="#{home.personInfo.humanReadableName}"/></title>
	</head>
	<body>
	<div class="person">
		<p>
	    <c:url value="viewperson.faces?personId=${home.signin.user.id}" var="publicurl"/>
		<strong><h:outputText value="#{home.personInfo.humanReadableName}"/></strong>
		(<a href="${publicurl}">public page</a>)</p>
		<div class="shared-links">	
		<p>Recently seen links:</p>	
		<table>
		<c:forEach items="${home.receivedPostInfos}" var="info">
		    <tr>
			    <td colspan="2"><strong><a href="${info.url}">${info.title}</a></strong></td>
			</tr>
			<tr>
				<th align="right">From:</th><td>${info.posterName} (<fmt:formatDate value="${info.post.postDate}" type="both"/>)</td>
			</tr>
			<tr>
			    <th align="right">To:</th><td>${info.recipientSummary}</td>
		    </tr>
		    <tr>
		    	<th></th><td>${info.post.text}</td>
		    </tr>
		</c:forEach>
		</table>
		</div>
		<br/>
		<div class="groups">
		<!--  list group objects here -->
		<!--  ability to join/share groups [D] -->
		</div>
	</div>	
	</body>
</f:view>
</html>
