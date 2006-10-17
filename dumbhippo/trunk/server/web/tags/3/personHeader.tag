<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="isSelf" required="true" type="java.lang.Boolean" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if>

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
			        <c:if test="${!shortVersion}">
			            <div class="dh-person-header-bio">
				            ${who.bioAsHtml}
			            </div>
			        </c:if>
                </td>
            </tr>             
		</tbody>
	</table>
	</td>
	<td align="right">
	<table cellpadding="0" cellspacing="0">
		<tbody>
			<tr valign="top">
				<td align="right">
				    <div class="dh-favicons">
				        <c:if test="${!empty who.email}">
						    <dht3:whereAtIcon label="Send me email" linkText="${who.email.email}" linkTarget="${who.emailLink}" imgSrc="/images3/${buildStamp}/mail_icon.png"/>
						</c:if>
						<c:if test="${!empty who.aim}">
						    <c:set var="aimIcon" value="/images3/${buildStamp}/aim_icon.png"/>
						    <c:if test="${!empty who.aimPresenceImageLink}">
						        <c:set var="aimIcon" value="${who.aimPresenceImageLink}"/>
						    </c:if>    
                            <dht3:whereAtIcon label="AIM" linkText="${who.aim.screenName}" linkTarget="${who.aimLink}" imgSrc="${aimIcon}"/>
						</c:if>						
						<c:forEach var="account" items="${who.lovedAccounts.list}">
						    <dht3:whereAtIcon label="${account.siteName}" linkText="${account.linkText}" linkTarget="${account.link}" imgSrc="/images3/${buildStamp}/${account.iconName}"/>
						</c:forEach>							
					</div>
				</td>
			</tr>
			<c:choose>
                <c:when test="${!shortVersion}">
			        <tr>
			            <td align="right">
			                <%-- Accounts with thumbnail boxes --%>
			 		        <c:forEach var="account" items="${who.lovedAccounts.list}">
						        <c:if test="${account.hasThumbnails}">
					                <dht:whereAtThumbnailBox account="${account}" />
						        </c:if>
					        </c:forEach>
				        </td>     
			        </tr>
			    </c:when>  
			    <c:otherwise>
			        <tr valign="bottom">
			            <td align="right">
			            	<div class="dh-back">
				                <a href="/person?who=${who.viewPersonPageId}">Back to <c:out value="${who.name}"/>'s Overview</a>
                            </div>
                        </td>
                    </tr>
                </c:otherwise>    
            </c:choose>              			    		
		</tbody>
	</table>						
	</td>
	</tr>
	</tbody>
	</table>
</div>
