<%-- "notebook tab" links appearing at the top of the set of pages related to a particular person --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="selected" required="false" type="java.lang.String" %>

<c:if test="${empty person}">
	<dht:errorPage>This page is broken! person required</dht:errorPage>
</c:if>

<c:choose>
	<c:when test="${person.self}">
	    <c:set var="whoName" value="my" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="whoName" value="${person.viewedPerson.name}'s" scope="page"/>
	</c:otherwise>
</c:choose>

<c:set var="whoParam" value="?who=${person.viewedPerson.viewPersonPageId}" scope="page"/>

<div class="dh-page-options-options-area">
    View ${whoName}: 
	<dht3:pageOptionLink name="Home" selected="${selected == 'person'}" link="/person${whoParam}"/> |
	<dht3:pageOptionLink name="Friends" selected="${selected == 'friends'}" link="/friends${whoParam}" disabled="${!person.viewedPerson.viewerIsContact}"/> |
	<dht3:pageOptionLink name="Groups" selected="${selected == 'groups'}" link="/groups${whoParam}"/>
</div>
