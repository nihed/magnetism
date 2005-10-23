<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<f:view>
	<head>
	<title><h:outputText value="#{viewperson.name}"/></title>
	</head>
	<body>
	<div class="person">
		<strong><h:outputText value="#{viewperson.name}"/></strong>
    	<br/>
		<div class="shared-links">	
		<p>Recently posted links:</p>	
		<table>
		<c:forEach items="${viewperson.postInfos}" var="info">
		    <tr>
			    <td><fmt:formatDate value="${info.post.postDate}" type="both"/></td>
			    <td><a href="${info.url}">${info.title}</a></td>
			    <td>${info.recipientSummary}</td>
		    </tr>
		    <tr>
		    	<td colspan="3">${info.post.text}</td>
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
