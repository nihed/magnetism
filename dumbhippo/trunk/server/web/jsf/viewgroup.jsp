<html>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<f:view>
	<head>
	<title>Group: <h:outputText value="#{viewgroup.name}"/></title>
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
				<dh:entity value="${info}/>
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
	</div>	
	</body>
</f:view>
</html>
