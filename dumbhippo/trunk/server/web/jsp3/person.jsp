<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:personPage pageName="Overview" stackSize="5">
	<dht3:shinyBox color="grey">
	    <div class="dh-person-stacker-header">
		    <span class="dh-person-header-name"><c:out value="${person.viewedPerson.name}"/>'s Stacker</span>
		</div>    
		<dht3:stacker person="${person.viewedPerson}" stack="${person.stack}" stackSize="${stackSize}" stackOrder="2"/>
	</dht3:shinyBox>
</dht3:personPage>
</html>