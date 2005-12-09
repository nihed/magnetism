<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.persistence.Group"%>

<a href="/viewgroup?groupId=${group.id}" style="text-decoration: none;"><dh:png klass="dh-headshot" src="/files/groupshots/${group.id}?v=${group.version}"/></a>
