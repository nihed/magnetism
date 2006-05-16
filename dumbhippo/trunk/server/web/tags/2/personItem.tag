<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>
<%@ attribute name="invited" required="false" type="java.lang.Boolean" %>

<div class="dh-compact-item">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:headshot person="${who}" invited="${invited}"/>
					</div>
				</td>
				<td>
					<div class="dh-next-to-image">
						<dht:personName who="${who}"/>
						<c:if test="${who.liveUser != null}">
							<div class="dh-info">${who.liveUser.groupCount} groups</div>
							<div class="dh-info">${who.liveUser.sentPostsCount} posts</div> 
						</c:if>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</div>
