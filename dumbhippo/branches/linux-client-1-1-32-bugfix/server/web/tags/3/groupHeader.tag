<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="anchor" required="false" type="java.lang.String" %>
<%@ attribute name="disableLink" required="false" type="java.lang.Boolean" %>

<dht3:groupHeaderContainer>
	<dht3:groupHeaderLeft who="${who}" embedVersion="${embedVersion}" anchor="${anchor}" disableLink="${disableLink}"><jsp:doBody/></dht3:groupHeaderLeft>
	<dht3:groupHeaderRight></dht3:groupHeaderRight>
</dht3:groupHeaderContainer>