<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>

<dht3:page currentPageLink="licenses">
	<dht3:pageSubHeader title="${title}"/>
		<dht3:shinyBox color="purple">
			<div id="dhLegalContentsOuter">
				<div id="dhLegalContents">
					<jsp:doBody/>
				</div>
			</div>
		</dht3:shinyBox>
	</dht3:page>
</html>