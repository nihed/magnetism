<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableSignupLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<body class="dh-gray-background-page">
	<div id="dhPage">
		<dht3:header disableHomeLink="${disableHomeLink}" disableSignupLink="${disableSignupLink}" searchText="${searchText}"/>
		<jsp:doBody/>
	</div>
	<dht:footer/>
</body>