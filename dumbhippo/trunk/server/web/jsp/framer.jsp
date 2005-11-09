<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="framer" class="com.dumbhippo.web.FramerPage" scope="request"/>
<jsp:setProperty name="framer" property="postId" param="postId"/>

<c:set var="title" value="${framer.post.title}" scope="page"/>

<head>
	<title><c:out value="${title}"/></title>
	<link rel="stylesheet" href="/css/person.css" type="text/css" />
	<dht:scriptIncludes/>
</head>
<body>
	<div id="dhMain">
		<table>
		<tr>
		<td>
			<dht:postBubble post="${framer.post}"/>
		</td>
		<td>
		    <a href="${framer.post.url}" target=_top>Remove frame</a>
		</td>
		</tr>
		</table>
	</div>
</body>
</html>
