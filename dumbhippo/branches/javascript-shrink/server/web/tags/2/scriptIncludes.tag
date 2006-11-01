<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%-- this tag is deprecated. Just include what you need with dh:script and the dependencies are implied,
     including site.js if appropriate. --%>
<dh:script src="dh/site.js"/>
<%-- any other manual scripts follow --%>
<jsp:doBody/>
