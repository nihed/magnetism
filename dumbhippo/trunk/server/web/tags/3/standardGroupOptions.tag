<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="groupId" required="true" type="java.lang.String" %>
<%@ attribute name="selected" required="true" type="java.lang.String" %>

<div>
	Group:&nbsp;
	<dht3:pageOptionLink name="Home" selected="${selected}" link="/group?who=${groupId}"/> |
	<dht3:pageOptionLink name="Members" selected="${selected}" link="/members?group=${groupId}"/> | 	
	<dht3:pageOptionLink name="Edit Group" selected="${selected}" link="/group-account?group=${groupId}"/>
</div>
