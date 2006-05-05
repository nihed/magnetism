<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.GroupView" %>

<div class="dh-compact-item">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:groupshot group="${group}"/>
					</div>
				</td>
				<td>
					<div class="dh-next-to-image">
						<div class="dh-name"><a href="/group?who=${group.group.id}"><c:out value="${group.group.name}"/></a></div>
						<div class="dh-info"><c:out value="${group.liveGroup.memberCount}"/> members</div>
						<div class="dh-info"><c:out value="${group.liveGroup.totalReceivedPosts}"/> posts</div>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</div>
