<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>

<div id="dhPageHeader">
	<div id="dhHeaderLogo"><a href="/main"><img src="/images2/mugshot_logo.gif"/></a></div>
	<div id="dhHeaderLinks">
		<c:choose>
			<c:when test="${signin.valid}">
				<c:if test="${!disableHomeLink}"><a href="/home">HOME</a> | </c:if><a href="">Log out</a>
			</c:when>
			<c:otherwise>
				<a href="">Sign up</a> | <a href="">Log in</a>
			</c:otherwise>
		</c:choose>
	</div>
	<div id="dhSearchBox">
		Search: <input type="text" class="dh-text-input"/> <input type="button" value="Go"/>
	</div>
</div>
