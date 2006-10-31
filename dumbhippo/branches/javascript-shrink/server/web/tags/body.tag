<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ attribute name="fixedHack" required="false" type="java.lang.Boolean" %>
<%-- this tag is used for lots of different page templates, so you probably shouldn't add stuff
	to it; instead create a tag for your "kind of page" --%>
<c:choose>
	<c:when test="${fixedHack}">
		<body class="dh-body-with-fixed-hack">
			<div id="dhContainer" class="dh-container-with-fixed-hack">
	</c:when>
	<c:otherwise>
		<body class="dh-body-without-fixed-hack">
			<div id="dhContainer" class="dh-container-without-fixed-hack">
	</c:otherwise>
</c:choose>
<jsp:doBody/>
</div><!-- dhContainer end -->
<dh:relocateDest where="outsideFixedHackContainer"/>
</body>
