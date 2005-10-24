<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<f:view>
	<head>
	<title><h:outputText value="#{viewperson.personInfo.humanReadableName}"/></title>
	</head>
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
	</div>	
	</body>
</f:view>
</html>
