<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%-- this tag has to be accompanied by a position: fixed in the css, to work on both browsers --%>
<dh:relocate where="outsideFixedHackContainer"><jsp:doBody/></dh:relocate>
