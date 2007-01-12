<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="selected" required="false" type="java.lang.String" %>

<c:if test="${empty person}">
	<dht:errorPage>This page is broken! person required</dht:errorPage>
</c:if>

<c:set var="whoParam" value="?who=${person.viewedPerson.viewPersonPageId}" scope="page"/>

<div class="dh-page-options-sub-options-area dh-page-options">
	<c:if test="${person.userContactCount > 0}"><dht3:pageOptionLink name="Activity" selected="${selected == 'network-activity'}" link="/network${whoParam}"/> |</c:if>
	<dht3:pageOptionLink name="Overview" selected="${selected == 'network-alphabetical'}" link="/network-overview${whoParam}" disabled="${!person.viewedPerson.viewerIsContact}"/>
</div>
