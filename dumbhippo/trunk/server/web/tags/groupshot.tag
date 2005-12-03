<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="groupId" required="true" type="java.lang.String"%>

<a href="/viewgroup?groupId=${groupId}">
	<dh:png klass="cool-person" src="/files/groupshots/${groupId}"/><!-- width/height in the css -->
</a>
