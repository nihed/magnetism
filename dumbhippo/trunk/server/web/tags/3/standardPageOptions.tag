<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="selected" required="true" type="java.lang.String" %>

<dht3:requirePersonBean/>

<c:if test="${!empty param['who']}">
	<c:set var="whoParam" value="?who=${param['who']}" scope="page"/>
</c:if>

<div class="dh-page-options-options-area">
    View ${person.viewedPerson.name}'s: 
	<dht3:pageOptionLink name="Home" selected="${selected}" link="/person${whoParam}"/> |
	<dht3:pageOptionLink name="Friends" selected="${selected}" link="/friends${whoParam}" disabled="${!person.viewedPerson.viewerIsContact}"/> | 	       
	<dht3:pageOptionLink name="Groups" selected="${selected}" link="/groups${whoParam}"/>
</div>
