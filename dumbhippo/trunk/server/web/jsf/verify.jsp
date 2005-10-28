<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="verify" class="com.dumbhippo.web.VerifyBean" scope="request"/>
<jsp:setProperty name="verify" property="authKey" param="authKey"/>

<head>
	<title>Verify</title>
</head>
<body>
		<h3>verify page</h3>
		<p>This is the verify page for <c:out value="${verify.authKey}"/></p>
		<c:choose>
		   <c:when test="${verify.valid}">
			   <p>You were invited by:</p>
			   <table>
			   <c:forEach items="${verify.inviterNames}" var="name">
			       <tr><td style="font-weight: bold" ><c:out value="${name}"/></td></tr>
			   </c:forEach>
			   </table>
    	   </c:when>
    	   <c:otherwise>
			   <p style="color: #FF0000">Invalid invitation</p>
		   </c:otherwise>	
		</c:choose>		   

        <c:url value="main" var="mainurl"/>
		<p><a href="${mainurl}">Go back to the main page</a></p>
</body>
</html>
