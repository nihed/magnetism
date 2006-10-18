<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:personPage pageName="Overview">
	<dht3:shinyBox color="grey">
	    <div class="dh-person-stacker-header">
		    <span class="dh-person-header-name"><c:out value="${person.viewedPerson.name}"/>'s Stacker</span>
		</div>    
		<dht3:stacker person="${person.viewedPerson}" stackOrder="2" stackType="dhStacker" pageable="${person.pageableStack}" showFrom="true"/>
		<c:if test="${person.pageableMugshot.position != 0}">
		    <div class="dh-back">
		        <a href="/person?who=${person.viewedPerson.viewPersonPageId}">Back to <c:out value="${person.viewedPerson.name}"/>'s Overview</a>
		    </div>
		</c:if>    		    
	</dht3:shinyBox>
</dht3:personPage>
</html>