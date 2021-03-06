<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh"%>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2"%>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3"%>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome"%>

<%@ attribute name="account" required="true" type="com.dumbhippo.web.pages.AccountPage" %>

<dht2:formTable tableId="dhAccounts" hasInfoCells="true">
    <c:if test="${dh:enumIs(site, 'GNOME') || dh:enumIs(signin.user.account.accountType, 'MUGSHOT')}">
	    <dht2:formTableRowStatus controlId='dhEmailEntry'></dht2:formTableRowStatus>
	    <dht2:formTableRow label="Email"
		icon="/images3/${buildStamp}/mail_icon.png"
		info='${dh:enumIs(site, "MUGSHOT") ? "Only your Mugshot friends see this." : ""}'>
		<table cellpadding="0" cellspacing="0" class="dh-address-table">
		<tbody>
		<c:forEach items="${account.person.allEmails}" var="email">
			<tr>
			<td>
			<c:out value="${email.email}" />
			</td>
			<td>
			<c:if test="${account.canRemoveEmails}">
				<c:set var="emailJs" scope="page">
					<jsp:attribute name="value">
						<dh:jsString value="${email.email}" />
					</jsp:attribute>
				</c:set>
				<a href="javascript:dh.account.removeClaimEmail(${emailJs});">remove</a>
			</c:if>
			</td>
			</tr>
			<tr class="dh-email-address-spacer">
			<td></td>
			<td></td>
			</tr>
		</c:forEach>
		<tr>
		<td>
		<dht2:textInput id='dhEmailEntry' />
		</td>
		<td>
		<img id='dhEmailVerifyButton'
			src="/${siteImageDir}/${buildStamp}/verify_button.gif"
			onclick="dh.account.verifyEmail();" />
		</td>
		</tr>
		</tbody>
		</table>
	    </dht2:formTableRow>
	    <dht2:formTableRowStatus controlId='dhXmppEntry'></dht2:formTableRowStatus>
	    <dht2:formTableRow label="IM"
		icon="/images3/${buildStamp}/chat16x16.png"
		info='${dh:enumIs(site, "MUGSHOT") ? "Only your Mugshot friends see this." : ""}'>
		<table id="dhImTable" cellpadding="0" cellspacing="0" class="dh-address-table">
		<tbody id="dhImTableBody">
		<c:forEach items="${account.person.allAims}" var="aim" varStatus="status">
			<tr>
			<td>
			<div class="dh-aim-address">
			<c:set var="aimJs" scope="page">
				<jsp:attribute name="value">
						    <dh:jsString value="${aim.screenName}" />
					    </jsp:attribute>
			</c:set>
			<c:out value="${aim.screenName}" />
			<c:if test="${!empty account.aimPresenceKey}">
				<a href="aim:GoIM?screenname=${aim.screenName}">
				<img
					src="http://api.oscar.aol.com/SOA/key=${account.aimPresenceKey}/presence/${aim.screenName}"
					border="0" />
				</a>
			</c:if>
			</div>
			</td>
			<td>
			<a href="javascript:dh.account.removeClaimAim(${aimJs});">remove</a>
			</td>
			</tr>
			<tr class="dh-email-address-spacer">
			<td></td>
			<td></td>
			</tr>
		</c:forEach>
		<c:forEach items="${account.person.allXmpps}" var="xmpp">
			<tr>
			<td class="dh-im-address">
			<c:out value="${xmpp.jid}" />
			</td>
			<td>
			<c:set var="xmppJs" scope="page">
				<jsp:attribute name="value">
					<dh:jsString value="${xmpp.jid}" />
				</jsp:attribute>
			</c:set>
			<a href="javascript:dh.account.removeClaimXmpp(${xmppJs});">remove</a>
			</td>
			</tr>
			<tr class="dh-email-address-spacer">
			<td></td>
			<td></td>
			</tr>
		</c:forEach>
		<c:forEach items="${account.claimedXmppResources}" var="xmpp">
			<tr>
			<td class="dh-im-address dh-im-pending-address">
			<c:out value="${xmpp.jid}" />
			</td>
			<td>
			<c:set var="xmppJs" scope="page">
				<jsp:attribute name="value">
					<dh:jsString value="${xmpp.jid}" />
				</jsp:attribute>
			</c:set>
			<a href="javascript:dh.account.removeClaimXmpp(${xmppJs});">cancel</a>
			</td>
			</tr>
			<tr>
			<td class="dh-im-verify-message">You've been sent a verify link</td>
			<td></td>
			</tr>
			<tr class="dh-email-address-spacer">
			<td></td>
			<td></td>
			</tr>
		</c:forEach>
		</tbody>
		</table>
		<dht3:addImAccount/>
	    </dht2:formTableRow>
	    <c:if test="${dh:enumIs(site, 'GNOME')}">
	        <c:forEach items="${account.gnomeSupportedAccounts.list}" var="supportedAccount">
	            <%-- icon is only used when label is set, otherwise we'd also have to set it to "" here --%> 
	            <c:set var="label" value=""/>
	            <c:if test="${supportedAccount.newType}">
	                <c:set var="label" value="${supportedAccount.siteName}"/>  
	            </c:if>
	            <c:set var="icon" value=""/>
	            <c:if test="${!empty supportedAccount.iconName}">
	                <c:set var="icon" value="/images3/${buildStamp}/${supportedAccount.iconName}"/>
	            </c:if>    
	            <dht2:formTableRow controlId="dh${supportedAccount.domNodeIdName}"
		                           label="${label}"				
			                       icon="${icon}">
                    <gnome:loveEntry name="${supportedAccount.siteName}"
			  	   	  	             userInfoType="${supportedAccount.userInfoType}"
							         link="${supportedAccount.onlineAccountType.site}"
					                 baseId="dh${supportedAccount.domNodeIdName}"
					                 mode="${supportedAccount.sentiment}">				        
			        </gnome:loveEntry>
			    </dht2:formTableRow>			
	        </c:forEach>
	    </c:if>
	</c:if>    
	<c:if test="${dh:enumIs(site, 'MUGSHOT')}">
		<dht2:formTableRow label="Website"
			icon="/images3/${buildStamp}/homepage_icon.png"
			controlId="dhWebsiteEntry">
			<dht2:textInput id="dhWebsiteEntry" maxlength="255" />
			<div id="dhWebsiteEntryDescription" style="display: none"></div>
		</dht2:formTableRow>
		<dht2:formTableRow label="Blog"
			icon="/images3/${buildStamp}/blog_icon.png" controlId='dhBlogEntry'>
			<dht2:textInput id="dhBlogEntry" maxlength="255" />
			<div id="dhBlogEntryDescription" style="display: none"></div>
		</dht2:formTableRow>
		<c:forEach items="${account.supportedAccounts.list}"
			var="supportedAccount">
			<c:set var="facebookAppInfo" value=""/>
			<c:set var="facebookAppInfoLink" value=""/>
			<c:if test="${supportedAccount.siteName == 'Facebook'}">
				<tr valign="top">
				<td colspan="3">
				<c:choose>
					<c:when test="${account.facebookErrorMessage != null}">
						<div id="dhFacebookNote">
						<c:out value="${account.facebookErrorMessage}" />
						<a href="http://facebook.com">Log out from Facebook first</a> to re-login here.
                                         </div>
					</c:when>
					<c:when test="${account.facebookAuthToken != null}">
						<div id="dhFacebookNote">Thank you for logging in to Facebook! You will now be getting Facebook updates.</div>
					</c:when>
				</c:choose>				
				</td>
				</tr>
				<c:set var="facebookAppInfo" value="Check out Mugshot application on Facebook!"/>
				<c:set var="facebookAppInfoLink" value="http://www.facebook.com/add.php?api_key=${account.facebookApiKey}"/>
			</c:if>
			<c:set var="prefixIcon" value="" />
			<c:set var="prefixIconWidth" value="" />
			<c:set var="prefixIconHeight" value="" />
			<c:if test="${supportedAccount.externalAccountType.new && supportedAccount.newType}">
				<c:set var="prefixIcon" value="/images3/${buildStamp}/new_icon.png" />
				<c:set var="prefixIconWidth" value="31" />
				<c:set var="prefixIconHeight" value="10" />
			</c:if>
		    <c:set var="label" value=""/>
	        <c:if test="${supportedAccount.newType}">
	            <c:set var="label" value="${supportedAccount.siteName}"/>  
	        </c:if>						
			<dht2:formTableRow controlId="dh${supportedAccount.domNodeIdName}"
				label="${label}"				
				icon="/images3/${buildStamp}/${supportedAccount.iconName}"
				info="${facebookAppInfo}"
				infoLink ="${facebookAppInfoLink}" 
				prefixIcon="${prefixIcon}" prefixIconWidth="${prefixIconWidth}"
				prefixIconHeight="${prefixIconHeight}">
				<c:choose>
					<c:when test="${supportedAccount.siteName == 'Facebook'}">
						<c:choose>
							<c:when test="${account.loggedInToFacebook}">
			                       You are logged in <a
									href="javascript:dh.account.disableFacebookSession();">Log out</a>
							</c:when>
							<c:otherwise>
								<a
									href="http://api.facebook.com/login.php?api_key=${account.facebookApiKey}&v=1.0&next=?next=account">Log in to receive updates</a>
							</c:otherwise>
						</c:choose>
					</c:when>
					<c:otherwise>
					    <%-- it's ok to assume that supportedAccount.externalAccountType is not null here --%>
					    <%-- because the list of supported accounts for Mugshot is generated based on the ExternalAccountType enumeration --%> 
                        <c:choose>
                            <c:when test="${supportedAccount.hateAllowed}">
						        <dht2:loveHateEntry name="${supportedAccount.siteName}"
							        userInfoType="${supportedAccount.externalAccountType.siteUserInfoType}"
							        isInfoTypeProvidedBySite="${supportedAccount.externalAccountType.infoTypeProvidedBySite}"
							        link="${supportedAccount.externalAccountType.siteLink}"
							        baseId="dh${supportedAccount.domNodeIdName}"
							        accountId="${supportedAccount.id}"
							        mode="${supportedAccount.sentiment}"
							        mugshotEnabled="${supportedAccount.mugshotEnabled}">
					                 
							        <c:if test="${supportedAccount.siteName == 'Amazon' && supportedAccount.mugshotEnabled}">
								        <div class="dh-amazon-details">
								        <c:forEach items="${account.amazonLinks}" var="amazonLinkPair">
									        <div>
									            <a href="${amazonLinkPair.second}" target="_blank">${amazonLinkPair.first}</a>
									        </div>
								        </c:forEach>
								        </div>
							        </c:if>
						        </dht2:loveHateEntry>
						    </c:when>    
						    <c:otherwise>
						        <dht3:loveEntry name="${supportedAccount.siteName}"
							        userInfoType="${supportedAccount.externalAccountType.siteUserInfoType}"
							        isInfoTypeProvidedBySite="${supportedAccount.externalAccountType.infoTypeProvidedBySite}"
							        link="${supportedAccount.externalAccountType.siteLink}"
							        baseId="dh${supportedAccount.domNodeIdName}"
							        accountId="${supportedAccount.id}"
							        mode="${supportedAccount.sentiment}"
							        mugshotEnabled="${supportedAccount.mugshotEnabled}">					                 							        
							        <c:if test="${supportedAccount.siteName == 'Amazon' && supportedAccount.mugshotEnabled}">
								        <div class="dh-amazon-details">
								        <c:forEach items="${account.amazonLinks}" var="amazonLinkPair">
									        <div>
									            <a href="${amazonLinkPair.second}" target="_blank">${amazonLinkPair.first}</a>
									        </div>
								        </c:forEach>
								        </div>
							        </c:if>
						        </dht3:loveEntry>						    
						    </c:otherwise>
						</c:choose>
					</c:otherwise>
				</c:choose>
			</dht2:formTableRow>
		</c:forEach>
	</c:if><%-- End of if MUGSHOT site --%>
</dht2:formTable>
