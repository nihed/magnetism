<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:choose>
	<c:when test="${person.self}">
		<c:set var="title" value="YOUR PROFILE" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="title" value="PROFILE" scope="page"/>
	</c:otherwise>
</c:choose>

<dht:sidebarBox boxClass="dh-profile-box" title="${title}">
	<div class="dh-compact-item">
		<table cellpadding="0" cellspacing="0">
			<tbody>
				<tr valign="top">
					<td>
						<div class="dh-image">
							<dht:headshot person="${person.viewedPerson}"/>
						</div>
					</td>
					<td>
						<div class="dh-next-to-image">
							<div class="dh-name"><c:out value="${person.viewedPerson.name}"/></div>
							
					<c:choose>
					<c:when test="${person.self}">
							<dht:actionLink href="/account" title="Set preferences, add addresses, and update your profile">Edit account</dht:actionLink>
					</c:when>
					<c:otherwise>
					    <c:if test="${signin.valid}">
					        <c:choose>
		    		        <c:when test="${person.contact}">
		    			        <dht:actionLink href="javascript:dh.actions.removeContact('${person.viewedPerson.viewPersonPageId}')" title="Remove this person from your friends list">Remove from your friends</dht:actionLink>
			    	        </c:when>
					        <c:otherwise>
								<dht:actionLink href="javascript:dh.actions.addContact('${person.viewedPerson.viewPersonPageId}')" title="Add this person to your friends list">Add to your friends</dht:actionLink>
							</c:otherwise>
							</c:choose>
					    </c:if>
					</c:otherwise>
					</c:choose>
					
					</div>
					</td>
				</tr>
			</tbody>
		</table>
		<c:if test="${!empty person.viewedPerson.bioAsHtml}">
			<div class="dh-bio">
				<c:out value="${person.viewedPerson.bioAsHtml}" escapeXml="false"/>
			</div>
		</c:if>
	</div>
</dht:sidebarBox>
