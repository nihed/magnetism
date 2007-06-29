<%-- "notebook tab" links appearing at the top of the set of pages related to a particular group --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="selected" required="true" type="java.lang.String" %>

<div>
	Group:&nbsp;
	<dht3:pageOptionLink name="Home" selected="${selected == 'group'}" link="/group?who=${group.group.id}"/> |
	<dht3:pageOptionLink name="Members" selected="${selected == 'members'}" link="/members?group=${group.group.id}"/>
</div>
