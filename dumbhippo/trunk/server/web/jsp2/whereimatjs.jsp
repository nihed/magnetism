<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/javascript" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%--  serve out where im at javascript  --%>

<dht:requirePersonBean asOthersWouldSee="true" needExternalAccounts="true"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>

<c:choose>
<c:when test="${!person.disabled}">
	<c:choose>
		<c:when test="${person.lovedAccounts.size > 0}">
			document.write('<ul class="mugshot-external-accounts">');
			<%-- Loved items with just links --%>
			<c:forEach var="account" items="${person.lovedAccounts.list}">
					document.write('<li class="mugshot-external-account">' + '<a href="${account.link}">' + '<c:out value="${account.siteName}"/>' + '</a></li>');
			</c:forEach>
			document.write('</ul>');
		</c:when>
		<c:otherwise> <%-- need to + in the jsString value by itself as it comes wrapped in single quotes --%>
			document.write('<div class="mugshot-error"><a href="' + <dh:jsString value="${baseUrl}"/> + '/person?who=${person.viewedUserId}">where\'s the love?</a></div>');
		</c:otherwise>
	</c:choose>
</c:when>
<c:otherwise>
	document.write('<div class="mugshot-error"><a href="' + <dh:jsString value="${baseUrl}"/> + '/person?who=${person.viewedUserId}">This account is disabled</a>.<br/>No web 2.0 for you!</div>');
</c:otherwise>
</c:choose>
