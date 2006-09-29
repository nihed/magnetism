<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.PersonView" %>
<%@ attribute name="isSelf" required="true" type="java.lang.Boolean" %>

<div class="dh-person-header">
    <table class="dh-person-info">
    <tbody>
    <tr valign="top">
    <td>
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
					<div class="dh-image">
						<dht:headshot person="${who}" invited="false"/>
					</div>
				</td>
				<td>
					<div class="dh-person-header-next-to-image">
						<span class="dh-presence">
							<c:choose>
								<c:when test="${who.online}">
									<dh:png src="/images3/${buildStamp}/online_icon.png" style="width: 12px; height: 12px;"/>
								</c:when>
								<c:otherwise>
									<dh:png src="/images3/${buildStamp}/offline_icon.png" style="width: 12px; height: 12px;"/>
								</c:otherwise>
							</c:choose>
						</span>			
						<c:choose>
							<c:when test="${isSelf}">
							<span class="dh-person-header-name"><c:out value="${who.name}"/>'s Mugshot</span>
							</c:when>
							<c:otherwise>
							<span class="dh-person-header-name"><a href="/person?who=${who.viewPersonPageId}"><c:out value="${who.name}"/></a>'s Mugshot</span>							
							</c:otherwise>
						</c:choose>
							
						<div class="dh-person-header-controls"><jsp:doBody/></div>
						<div class="dh-person-header-stats">
							<c:if test="${who.liveUser != null}">
								<span class="dh-info">${who.liveUser.contactResourcesSize} friends</span> | 							
								<span class="dh-info">${who.liveUser.groupCount} groups</span> | 
								<span class="dh-info">${who.liveUser.sentPostsCount} posts</span> 
							</c:if>
						</div>
					</div>
				</td>
			</tr>
			<tr>
			<td colspan="2">
			<div class="dh-person-header-bio">
				${who.bioAsHtml}
			</div>
			</td>
			</tr>
		</tbody>
	</table>
	</td>
	<td align="right">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td>
				    <div class="dh-favicons">
				        <%-- TODO: need to include AIM and separate website from external accounts to show it --%>
				        <%-- with a home icon. --%>
				        <c:if test="${!empty who.email}">
						    <dht3:whereAtIcon label="Send me email" linkText="${who.email.email}" linkTarget="${who.emailLink}" imgSrc="/images3/${buildStamp}/mail_icon.png"/>
						</c:if>
						<c:forEach var="account" items="${who.lovedAccounts.list}">
						    <dht3:whereAtIcon label="${account.siteName}" linkText="${account.linkText}" linkTarget="${account.link}" imgSrc="${account.favicon}"/>
						</c:forEach>							
					</div>
				</td>
			</tr>
		</tbody>
	</table>						
	</td>
	</tr>
	</tbody>
	</table>
</div>
