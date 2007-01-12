<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<c:if test="${empty showHomeUrl}">
	<c:set var="showHomeUrl" value="true"/>
</c:if>

<dht3:shinyBox color="orange">
    <dht3:groupHeader who="${who}" embedVersion="true">
		 <dht:actionLink oneLine="true" href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="You were invited to join this group">Join group (invited)</dht:actionLink>      
	</dht3:groupHeader>
</dht3:shinyBox>		
