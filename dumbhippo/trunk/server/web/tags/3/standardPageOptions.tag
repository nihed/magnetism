<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="selected" required="true" type="java.lang.String" %>

<dht3:pageOptions>
	<dht3:pageOptionLink name="Overview" selected="${selected}" link="/"/> |
	<dht3:pageOptionLink name="History" selected="${selected}" link="/"/> | 
	<dht3:pageOptionLink name="Friends" selected="${selected}" link="/friends"/> | 	
	<dht3:pageOptionLink name="Groups" selected="${selected}" link="/groups"/> | 	
	<dht3:pageOptionLink name="Faves" selected="${selected}" link="/faves"/>
</dht3:pageOptions>
