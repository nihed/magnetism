<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.PersonPage"/>

<dh:bean id="account" class="com.dumbhippo.web.pages.AccountPage" scope="page"/>
<%-- This is a Facebook authetication token, we can create a seperate post-facebook-login landing page --%>
<%-- or land people on their account page, get a token, and display a message --%>
<jsp:setProperty name="account" property="facebookAuthToken" param="auth_token"/>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>

<c:set var="termsOfUseNote" value='false'/>
<c:if test='${!empty param["termsOfUseNote"]}'>
    <c:set var="termsOfUseNote" value='${param["termsOfUseNote"]}'/> 
</c:if>

<c:set var="pageName" value="Account" scope="page"/>

<c:choose>
    <c:when test="${browser.ie}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_ie.gif"/>
        <c:set var="browseInputSize" value="0"/>
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <%-- This is ok here, but don't use browser.firefox on the anonymous pages --%>
    <%-- for which we use a single cache for all gecko browsers. --%>
    <c:when test="${browser.firefox && browser.windows}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_ff.gif"/>   
        <c:set var="browseInputSize" value="1"/>  
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:when test="${browser.firefox && browser.linux}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_lff.gif"/>   
        <c:set var="browseInputSize" value="1"/>  
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:otherwise>
        <c:set var="browseButton" value=""/>     
        <c:set var="browseInputSize" value="24"/>
        <c:set var="browseInputClass" value="dh-file-upload"/>
    </c:otherwise>
</c:choose>
    
<head>
    <title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="account" iefixes="true"/>	
	<dht:faviconIncludes/>
		<dh:script module="dh.account"/>
	<script type="text/javascript">
		dh.formtable.currentValues = {
			'dhUsernameEntry' : <dh:jsString value="${signin.user.nickname}"/>,
			'dhBioEntry' : <dh:jsString value="${signin.user.account.bio}"/>,
			'dhMusicBioEntry' : <dh:jsString value="${signin.user.account.musicBio}"/>,
			'dhWebsiteEntry' : <dh:jsString value="${account.websiteUrl}"/>,
			'dhBlogEntry' : <dh:jsString value="${account.blogUrl}"/>
		};
		dh.account.userId = <dh:jsString value="${signin.user.id}"/>
		dh.account.reloadPhoto = function() {
			dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer')], 60);
		}
		dh.account.initialMyspaceName = <dh:jsString value="${account.mySpaceName}"/>;
		dh.account.initialMyspaceHateQuip = <dh:jsString value="${account.mySpaceHateQuip}"/>;
		dh.account.initialYouTubeName = <dh:jsString value="${account.youTubeName}"/>;
		dh.account.initialYouTubeHateQuip = <dh:jsString value="${account.youTubeHateQuip}"/>;
		dh.account.initialLastFmName = <dh:jsString value="${account.lastFmName}"/>;
		dh.account.initialLastFmHateQuip = <dh:jsString value="${account.lastFmHateQuip}"/>;			
		dh.account.initialFlickrEmail = <dh:jsString value="${account.flickrEmail}"/>;
		dh.account.initialFlickrHateQuip = <dh:jsString value="${account.flickrHateQuip}"/>;
		dh.account.initialLinkedInName = <dh:jsString value="${account.linkedInName}"/>;
		dh.account.initialLinkedInHateQuip = <dh:jsString value="${account.linkedInHateQuip}"/>;
		dh.account.initialRhapsodyUrl = <dh:jsString value="${account.rhapsodyListeningHistoryFeedUrl}"/>;
		dh.account.initialRhapsodyHateQuip = <dh:jsString value="${account.rhapsodyHateQuip}"/>;	
		dh.account.initialDeliciousName = <dh:jsString value="${account.deliciousName}"/>;
		dh.account.initialDeliciousHateQuip = <dh:jsString value="${account.deliciousHateQuip}"/>;	
		dh.account.initialTwitterName = <dh:jsString value="${account.twitterName}"/>;
		dh.account.initialTwitterHateQuip = <dh:jsString value="${account.twitterHateQuip}"/>;
		dh.account.initialDiggName = <dh:jsString value="${account.diggName}"/>;
		dh.account.initialDiggHateQuip = <dh:jsString value="${account.diggHateQuip}"/>;
		dh.account.initialRedditName = <dh:jsString value="${account.redditName}"/>;
		dh.account.initialRedditHateQuip = <dh:jsString value="${account.redditHateQuip}"/>;					
	</script>
</head>
<dht3:page currentPageLink="account">
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:randomTip tipIndex="${person.randomTipIndex}" isSelf="${person.self}"/>
	</dht3:pageSubHeader>
			<c:choose>
			<c:when test="${!signin.user.account.disabled}">
			    <dht3:shinyBox color="grey">
			    <c:if test="${param.fromDownload != 'true'}">
				    <div class="dh-page-shinybox-subtitle"><span class="dh-download-product">Get maximum Mugshot!</span> <a class="dh-underlined-link" href="/download">Download the Mugshot software</a> to use all of our features.  It's easy and free!</div>
				</c:if>
				<dht:formTable bodyId="dhAccountInfoForm">
				<dht3:formTableRowSeparator>
				    <div class="dh-section-header">Public Info</div>
				    <span class="dh-section-explanation">This information will be visible on your <a href="/person">Home</a> page.</span>
                </dht3:formTableRowSeparator>
				<dht:formTableRowStatus controlId='dhUsernameEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="My name">
					<dht:textInput id="dhUsernameEntry" extraClass="dh-username-input"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhBioEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="About me" altRow="true">
					<%--
					<div>
						<input type="button" value="Generate a random bio!" onclick="dh.account.generateRandomBio();"/>
					</div>
					--%>
					<div>
						<dht:textInput id="dhBioEntry" multiline="true"/>
					</div>
				</dht:formTableRow>
				<!-- music bio currently disabled
				<dht:formTableRowStatus controlId='dhMusicBioEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Your music bio">
					<div>
						<dht:textInput id="dhMusicBioEntry" multiline="true"/>
					</div>
				</dht:formTableRow>
				-->
				<dht:formTableRowStatus controlId='dhPictureEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="My picture">
					<div id="dhHeadshotImageContainer" class="dh-image">
						<dht:headshot person="${account.person}" size="60" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" />
					</div>
					<div class="dh-next-to-image">
						<div class="dh-picture-instructions">Upload new picture:</div>
						<c:set var="location" value="/headshots" scope="page"/>
						<c:url value="/upload${location}" var="posturl"/>
						<form id='dhPictureForm' enctype="multipart/form-data" action="${posturl}" method="post">
							<input id='dhPictureEntry' class="${browseInputClass}" type="file" name="photo" size="${browseInputSize}"/>
							<c:if test="${browseInputClass == 'dh-hidden-file-upload'}">
							    <div id='dhStyledPictureEntry' class="dh-styled-file-upload">
							        <img src="${browseButton}">
							    </div>
							</c:if>
							<input type="hidden" name="groupId" value=""/>
							<input type="hidden" name="reloadTo" value="/account"/>
						</form>
						<div class="dh-picture-more-instructions">
						or <a href="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" title="Choose from a library of pictures">choose a stock picture</a>						
						</div>
						<div id="dhChooseStockLinkContainer">
						</div>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</dht:formTableRow>
				<dht:formTableRow label="Accounts" altRow="true">
					<dht:formTable bodyId="dhAccounts">
				    <dht:formTableRowStatus controlId='dhEmailEntry'></dht:formTableRowStatus>
				    <dht:formTableRow label="Email" icon="/images3/${buildStamp}/mail_icon.png" info="Only your Mugshot friends see this.">
					    <table cellpadding="0" cellspacing="0" class="dh-address-table">
						    <tbody>
							<c:forEach items="${account.person.allEmails}" var="email">
								<tr>
									<td><c:out value="${email.email}"/></td>
									<td>
										<c:if test="${account.canRemoveEmails}">
											<c:set var="emailJs" scope="page">
												<jsp:attribute name="value">
													<dh:jsString value="${email.email}"/>
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
							<tr><td><dht:textInput id='dhEmailEntry'/></td><td><img id='dhEmailVerifyButton' src="/images3/${buildStamp}/verify_button.gif" onclick="dh.account.verifyEmail();"/></td></tr>
                            </tbody>
					    </table>
				    </dht:formTableRow>
				    <dht:formTableRow label="AIM" icon="/images3/${buildStamp}/aim_icon.png" info="Only your Mugshot friends see this.">
					    <c:forEach items="${account.person.allAims}" var="aim" varStatus="status">
						    <div class="dh-aim-address">
							    <c:set var="aimJs" scope="page">
								    <jsp:attribute name="value">
									    <dh:jsString value="${aim.screenName}"/>
								    </jsp:attribute>
							    </c:set>
							    <c:out value="${aim.screenName}"/>
							    <c:if test="${!empty account.aimPresenceKey}">
								    <a href="aim:GoIM?screenname=${aim.screenName}"><img src="http://api.oscar.aol.com/SOA/key=${account.aimPresenceKey}/presence/${aim.screenName}" border="0"/></a>
							    </c:if>
							    <a href="javascript:dh.account.removeClaimAim(${aimJs});">remove</a>
						    </div>
					    </c:forEach>
					    <div>
						    <a href="${account.addAimLink}"><dh:png klass="dh-add-icon" src="/images3/${buildStamp}/add_icon.png" style="width: 10; height: 10; overflow: hidden;"/>IM our friendly bot to add a new screen name</a>
					    </div>
				    </dht:formTableRow>				
				    <dht:formTableRowStatus controlId='dhWebsiteEntry'></dht:formTableRowStatus>
				    <dht:formTableRow label="Website" icon="/images3/${buildStamp}/homepage_icon.png">
					    <dht:textInput id="dhWebsiteEntry" maxlength="255"/>
				    </dht:formTableRow>
				    <dht:formTableRowStatus controlId='dhBlogEntry'></dht:formTableRowStatus>
				    <dht:formTableRow label="Blog" icon="/images3/${buildStamp}/blog_icon.png">
					    <dht:textInput id="dhBlogEntry" maxlength="255"/>
				    </dht:formTableRow>				
				    <tr valign="top">
	                    <td colspan="3">
	                        <c:choose>
	                            <c:when test="${account.facebookErrorMessage != null}">
                                    <div id="dhFacebookNote">
                                        <c:out value="${account.facebookErrorMessage}"/>
                                        <a href="http://facebook.com">Log out from Facebook first</a> to re-login here.
                                    </div>                     
                                </c:when>   	     
	                            <c:when test="${account.facebookAuthToken != null}">
                                    <div id="dhFacebookNote">Thank you for logging in to Facebook! You will now be getting Facebook updates.</div>                     
                                </c:when>
                            </c:choose>       	                
                        </td>
                    </tr>     
				    <dht:formTableRow label="Facebook" icon="/images3/${buildStamp}/favicon_facebook.png">
				        <c:choose>
				            <c:when test="${account.loggedInToFacebook}">
				                You are logged in <a href="javascript:dh.account.disableFacebookSession();">Log out</a>
				            </c:when>
				            <c:otherwise>
				                <a href="http://api.facebook.com/login.php?api_key=${account.facebookApiKey}&next=/account">Log in to receive updates</a>
				            </c:otherwise>
				        </c:choose>    			            					   
				    </dht:formTableRow>
				    <c:forEach items="${account.supportedAccounts.list}" var="supportedAccount">
                        <dht:formTableRow label="${supportedAccount.siteName}" icon="/images3/${buildStamp}/${supportedAccount.iconName}">
		                    <dht:loveHateEntry baseId="dh${supportedAccount.siteBaseName}" mode="${supportedAccount.sentiment}">
		                        <c:if test="${supportedAccount.siteBaseName == 'Rhapsody'}">
		                            <a href="http://www.rhapsody.com/myrhapsody/rss.html" target="_blank" class="dh-text-input-help">help me find it</a>          
		                        </c:if>
		                    </dht:loveHateEntry>
				        </dht:formTableRow>	
		            </c:forEach>
				</dht:formTable>    	
				</dht:formTableRow>	
				<dht:formTableRow label="Music Radar preferences">
				    <div id="dhMusicRadarPreferences">
				    Music sharing: 
				    <c:choose>
					<c:when test="${signin.musicSharingEnabled}">
						<dh:script module="dh.actions"/>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>			
					</c:when>
					<c:otherwise>
						<dh:script module="dh.actions"/>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>
					</c:otherwise>
					</c:choose>
					<div>
					<a href="/radar-themes">Edit my Music Radar theme</a> | <a href="/getradar">Get Music Radar HTML</a>
					</div>	
					</div>    
				</dht:formTableRow>
				<dht3:formTableRowSeparator>
				    <div class="dh-section-header">Private Info</div>
				    <span class="dh-section-explanation">Nobody sees this stuff but you.</span>
                </dht3:formTableRowSeparator>
				<dht:formTableRowStatus controlId='dhPasswordEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Set a password" altRow="true">
				    <div class="dh-explanation">
					A password is optional. You can also log into Mugshot by having a log in link sent to any of your
					email addresses or screen names from the <i>Log In</i> screen.
					</div>
					Enter password:
					<br/>
					<dht:textInput id="dhPasswordEntry" type="password" extraClass="dh-password-input"/>
					<br/>
				    Re-type password:
					<br/>
					<dht:textInput id="dhPasswordAgainEntry" type="password" extraClass="dh-password-input"/>
					<br/>
					<c:if test="${!account.hasPassword}">
						<c:set var="removePasswordLinkStyle" value="display: none;" scope="page"/>
					</c:if>
					<img id="dhSetPasswordButton" src="/images3/${buildStamp}/setpassword_disabled.gif"/><a id="dhRemovePasswordLink" style="${removePasswordLinkStyle}" href="javascript:dh.password.unsetPassword();" title="Delete my password">Delete my current password.</a>
				</dht:formTableRow>
				<dht:formTableRow label="Disable account">
				    <div class="dh-explanation">
					Disabling your account means we won't show any information on your
					public Home page, and we will never send you email for any reason.
					You can enable your account again at any time.
					</div>
					<a name="accountStatus"></a>
					<c:if test="${termsOfUseNote=='true'}">
                        <div id="dhTermsOfUseNote">If you no longer agree with <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>, disable your account here.</div>
                    </c:if>    
				    <div>
						<dh:script module="dh.actions"/>						
						<img id="dhDisableAccountButton" src="/images3/${buildStamp}/disable.gif" onclick="javascript:dh.actions.disableAccount();"/>
					</div>
					<div>
					</div>
				</dht:formTableRow>
			    </dht:formTable>
		        </dht3:shinyBox>
				</c:when>
				<c:otherwise>
				    <%-- This code is never hit, because we redirect to /we-miss-you in this case. --%>
					<dht:formTable>				
					<dht:formTableRow label="Enable account">
					    <a name="accountStatus"></a>
					    <c:if test="${termsOfUseNote=='true'}">
                            <div id="dhTermsOfUseNote">Your account is already disabled, which means you no longer agree with <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>.</div>
                        </c:if>     
					    <div class="dh-account-disabled-message">
						    <c:out value="${signin.user.nickname}" />, your account is disabled.
						</div>
						<div>
						<p>
							Enable your account to interact with friends and groups, share 
							links, and use Music Radar to display your playlists.
						</p>
						</div>
						<div>
							<dh:script module="dh.actions"/>
							<img src="/images3/${buildStamp}/enable_acct_button.gif" onclick="javascript:dh.actions.enableAccount();"/>
						</div>
					</dht:formTableRow>						
					</dht:formTable>
				</c:otherwise>
			</c:choose>
	</dht3:page>		
	<dht:photoChooser/>
</html>
