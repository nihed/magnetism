<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="personId" required="true" type="java.lang.String"%>

<a href="/viewperson?personId=${personId}" style="text-decoration: none;"><dh:png klass="cool-person" src="/files/headshots/${personId}"/></a>


