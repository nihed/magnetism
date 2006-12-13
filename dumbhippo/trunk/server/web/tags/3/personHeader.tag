<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="linkifyName" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if>

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if>

<c:if test="${empty linkifyName}">
	<c:set var="linkifyName" value="true"/>
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
						<dht:headshot person="${who}" size="60" invited="false" disableLink="${!linkifyName}"/>
					</div>
				</td>
				<td>
					<div class="dh-person-header-next-to-image">
						<dht3:presenceIcon who="${who}"/>		
						<c:choose>
							<c:when test="${!linkifyName}">
								<span class="dh-person-header-name"><c:out value="${who.name}"/>'s Mugshot</span>
							</c:when>
							<c:when test="${who.viewOfSelf}">
								<span class="dh-person-header-name"><a href="/"><c:out value="${who.name}"/></a>'s Mugshot</span>							
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
						<div class="dh-person-header-controls"><jsp:doBody/></div>
						<c:if test="${!embedVersion && who.liveUser != null}">
						    <div class="dh-person-header-stats">
								<span class="dh-info"><dht3:plural n="${who.liveUser.contactsCount}" s="friend"/></span> | 							
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
					                    <div class="dh-person-header-hated-account"><dh:png src="/images3/${buildStamp}/quiphate_icon.png" />&nbsp;<a class="dh-person-header-hated-account-link" href="${account.externalAccount.siteLink}"><c:out value="${account.externalAccount.siteName}" /></a>&nbsp;&mdash;&nbsp;<span class="dh-person-header-hated-account-quip"><c:out value="${account.externalAccount.quip}" /></span></div>
					            </c:forEach>					
					    </div>
					</c:if>
			        </c:if>
                </td>
            </tr>             
		</tbody>
	</table>
	</td>
	<c:if test="${!embedVersion}">          
	    <td align="right">             
	        <table cellpadding="0" cellspacing="0">
	 	    <tbody>
			    <tr valign="top">
				    <td align="right">
				        <dht3:whereAtIcons who="${who}"/>
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
				                    <a href="/person?who=${who.viewPersonPageId}">Back to <c:out value="${who.name}"/>'s Home</a>
                                </div>
                            </td>
                        </tr>
                    </c:otherwise>    
                </c:choose>              			    		
		    </tbody>
	        </table>							            
	    </td>
	</c:if>    
	</tr>
	</tbody>
	</table>
</div>
