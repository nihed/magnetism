<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableLink" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if>

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if>

<td>
<table cellpadding="0" cellspacing="0" class="dh-person-header-left">
	<tbody>
		<tr valign="top">
			<td>
				<div class="dh-image">
					<dht:headshot person="${who}" size="60" disableLink="${disableLink}"/>
				</div>
			</td>
			<td>
				<div class="dh-person-header-next-to-image">
					<div class="dh-person-header-title">
					<dht3:presenceIcon who="${who}"/>		
					<c:choose>
						<c:when test="${who.viewOfSelf}">
							<span class="dh-person-header-name">My Mugshot</span>
						</c:when>
						<c:when test="${disableLink}">
							<span class="dh-person-header-name"><c:out value="${who.name}"/>'s Mugshot</span>
						</c:when>
						<c:otherwise>
							<span class="dh-person-header-name"><a href="${who.homeUrl}"><c:out value="${who.name}"/></a>'s Mugshot</span>
						</c:otherwise>
					</c:choose>
					<c:if test="${!embedVersion}">
					    <c:choose>
						    <c:when test="${who.viewOfSelf}">
							    <span class="dh-person-header-description">What I'm doing online</span>							
						    </c:when>
						    <c:otherwise>
							    <span class="dh-person-header-description">What <c:out value="${who.name}"/> is doing online</span>							
						    </c:otherwise>
					    </c:choose>
					</c:if>
					</div>	
					<div class="dh-person-header-controls"><jsp:doBody/></div>
					<c:if test="${!embedVersion && who.liveUser != null}">
					    <div class="dh-person-header-stats">
							<span class="dh-info"><c:out value="${who.liveUser.userContactsCount} in network"/>
								<c:if test="${who.viewOfSelf && who.user.account.invitations > 0}"> - <a href="/invitation">Invite friends</a>
								</c:if>
							</span> |
							<span class="dh-info"><dht3:plural n="${who.liveUser.groupCount}" s="group"/></span> | 
							<span class="dh-info"><dht3:plural n="${who.liveUser.sentPostsCount}" s="post"/></span> 
					    </div>
					    <c:if test="${who.viewOfSelf}">
						<div class="dh-person-header-controls">
							<a href="/badges"><dh:png src="/images3/${buildStamp}/mini_icon.png" style="width: 28px; height: 11px;"/> Get a Mini Mugshot for your site</a>
						</div>
					    </c:if>
					</c:if>
					<c:if test="${embedVersion}">
					    <dht3:whereAtIcons who="${who}"/>
					</c:if>    
				</div>
			</td>
		</tr>
		<tr>
		    <td colspan="2">
		        <c:if test="${!shortVersion && !embedVersion}">
		            <div class="dh-person-header-bio">
			            ${who.bioAsHtml}
		            </div>
				<%-- Accounts you won't find me at --%>
				<c:if test="${!empty who.hatedAccounts.list}" >
				    <div class="dh-person-header-hated-accounts">
					<strong>Where you won't find <c:out value="${who.name}"/></strong>
		 		            <c:forEach var="account" items="${who.hatedAccounts.list}">
				                    <div class="dh-person-header-hated-account"><dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>&nbsp;<a class="dh-person-header-hated-account-link" href="${account.externalAccount.siteLink}"><c:out value="${account.externalAccount.siteName}" /></a>&nbsp;&mdash;&nbsp;<span class="dh-person-header-hated-account-quip"><c:out value="${account.externalAccount.quip}" /></span></div>
				            </c:forEach>					
				    </div>
				</c:if>
		        </c:if>
               </td>
           </tr>             
	</tbody>
</table>
</td>
