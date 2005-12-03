<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="personId" required="true" type="java.lang.String"%>

<a href="/viewperson?personId=${personId}">
	<dh:png klass="cool-person" src="/files/headshots/${personId}"/><!-- width/height in the css -->
</a>


