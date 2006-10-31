<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="welcome" class="com.dumbhippo.web.pages.DownloadPage" scope="page"/>
<jsp:setProperty name="welcome" property="invitationId" param="invite"/>
<jsp:setProperty name="welcome" property="inviterId" param="inviter"/>

<c:set var="urlParams" value=''/>
<c:set var="acceptMessage" value='false'/>
<c:if test='${!empty param["acceptMessage"]}'>
    <c:set var="acceptMessage" value='${param["acceptMessage"]}'/>
    <c:set var="urlParams" value='&acceptMessage=${param["acceptMessage"]}'/>    
</c:if>
<c:if test='${!empty param["invite"]}'>
    <c:set var="urlParams" value='${urlParams}&invite=${param["invite"]}'/>    
</c:if>
<c:if test='${!empty param["inviter"]}'>
    <c:set var="urlParams" value='${urlParams}&inviter=${param["inviter"]}'/>    
</c:if>

<head>
	<title>Mugshot</title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/download.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes>
		<dht:script src="dh/download.js"/>
	</dht:scriptIncludes>
	<script type="text/javascript">
		dh.download.needTermsOfUse = ${signin.needsTermsOfUse}
		dojo.event.connect(dojo, "loaded", function () { dh.download.init() })
	</script>
</head>

<dht:body>
	<div id="dhDownloadTopMessageContainer">
		<img src="/images2/${buildStamp}/downloadheader.gif"/>
		<div id="dhDownloadTopMessage">Thanks for trying our free anti-productivity tools!</div>
	</div>
	<table cellspacing="0" cellpadding="0" align="center" valign="top" style="table-layout: fixed;">
	<tr valign="top">
    <td valign="top" width="49%">
    	<div id="dhDownloadInfoAreaContainer">
        <table id="dhDownloadInfoArea" cellspacing="0" cellpadding="0" valign="top">
            <tr>
            	<c:choose>
            		<c:when test="${welcome.inviter != null}">
		                <td class="dh-download-information-image"><img src="${welcome.inviter.photoUrl60}"/></td>               
        		        <td align="left">    	    	            	
	    	            	<div class="dh-download-information dh-download-welcome-information">
	    	            	    <strong><c:out value="${welcome.inviter.name}"/> has invited you to become a Mugshot member.</strong> Sign
	    	            	    up to use our free and fun tools.
	    	            	    <br/>
	    	            	    <a href="${welcome.inviter.homeUrl}" target="_blank" class="dh-download-more-information">View <c:out value="${welcome.inviter.name}"/>'s Mugshot profile</a>     		                			     	    
	    	            	</div>        		                			
        		        </td>
            		</c:when>
            		<c:otherwise>
		                <td colspan="2">
	    	            	<div class="dh-download-information dh-download-welcome-information">
    	    	            <%-- Text suitable for an invitation from Mugshot: You were invited because you signed up earlier on the site. --%>
        	    	        <strong>Welcome to Mugshot!</strong> Sign up to use our free and fun tools.
            	    	    </div>
   	                    </td>
                	</c:otherwise>
                </c:choose>
            </tr>
            <tr>
	            <td colspan="2">
                    <hr size="1px"/>
                </td>            
            </tr>
            <tr>
                <td class="dh-download-information-image"><img src="/images2/${buildStamp}/buzzer50x44.gif"/></td>               
                <td align="left">
                    <div class="dh-download-information">
                        <strong>Web Swarm</strong> lets you share and chat about links with friends.  
                        <br/>
                        <%-- TODO: make all links on this side open in the same second browser, if this is possible --%>
                        <a href="/links" target="_blank" class="dh-download-more-information">See what people are sharing</a> 
                    </div>
                </td>
            </tr>
            <tr>
	            <td colspan="2">
                    <hr size="1px"/>
                </td>            
            </tr>            
            <tr>
                <td class="dh-download-information-image"><img src="/images2/${buildStamp}/beacon32x44.gif"/></td>      
                <td align="left">
                    <div class="dh-download-information">                
                        <strong>Music Radar</strong> shares your music taste with friends and on your blog.   
                        <br/>  
                        <a href="/music" target="_blank" class="dh-download-more-information">See what people are listening to</a>    
                    </div>
                </td>                
            </tr>
            <tr>
	            <td colspan="2">
                    <hr size="1px"/>
                </td>            
            </tr>                      
            <tr>
                <td colspan="2">
                    <div class="dh-download-more-information">
                        <strong>More information from <a href="http://blog.mugshot.org/?page_id=245" target="_blank">Mugshot Help</a>:</strong>
                        <ul>
                    	    <li><a href="http://blog.mugshot.org/?page_id=233#need_download" target="_blank">Why do I need to download anything to use Mugshot?</a></li>
                            <li><a href="http://blog.mugshot.org/?page_id=233#download_missing" target="_blank">What if the download doesn't work?</a></li>
                        </ul>
                    </div>
                </td>                 
            </tr>                  
       </table>
       </div>
    </td>
    <td width="2%">&nbsp;</td>
	<td width="49%">
		<table cellspacing="0" cellpadding="0" valign="top" border="0">
		    <c:if test="${acceptMessage=='true'}">
                <tr id="dhMustAccept"><td colspan="2">You must agree to our Terms of Use and Privacy Policy before logging in to Mugshot.</td></tr>
            </c:if>    
		    <tr><td colspan="2" id="dhDownloadInstructionsHeader" class="dh-download-purple">Start using Mugshot in 3 quick steps:</td></tr>
		    <tr>
		        <td valign="top" class="dh-download-purple dh-download-instruction-number">1.</td> 
		        <td>
					<div class="dh-download-instruction">		        
		            <c:choose>
			            <c:when test="${signin.needsTermsOfUse}">
			                <div id="dhAcceptTermsBox">        
					            Read our <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>.					        
                                </br>
				                <input type="checkbox" id="dhAcceptTerms" onclick="dh.download.updateDownload();">
					                I accept these terms.    
				                </input>
			                </div>
			            </c:when>    
			            <c:otherwise>
			                <dh:png src="/images2/${buildStamp}/check21x20.png" style="width: 21; height: 20;"/>
                            &nbsp;<span class="dh-download-purple">Done!</span>
                            <div class="dh-download-accepted">
                            Agreed to <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>.					        
			                </div>
			                <div class="dh-download-accepted">
			                (See your <a href="/account?termsOfUseNote=true#accountStatus">account page</a> for more info.)
			                </div>
			            </c:otherwise>
			        </c:choose>        			            
			        </div>
		        </td>
		    </tr>
			<tr>
			    <td valign="top" class="dh-download-purple dh-download-instruction-number">2.</td>
			    <td> 
			        <a id="dhDownloadProduct" class="dh-download-product" href="javascript:dh.download.doDownload('${welcome.downloadUrl}')">Click here to download Mugshot.</a>	
				    <div class="dh-download-instruction">
				       	<c:choose>
			                <c:when test="${browser.linuxRequested}">
			                    This download is for Fedora Core 5.  Install it and run 'mugshot'.
			                </c:when>
			                <c:otherwise>
			                    The software will install automatically.
			                </c:otherwise>
			            </c:choose>				        	            
			        </div>
			    </td>
			</tr>
			<tr>
			    <td valign="top" class="dh-download-purple dh-download-instruction-number">3.</td>
			    <td>
				    <div class="dh-download-instruction">   
			    	    <c:choose>
			    		    <c:when test="${welcome.receivedTutorialShare}">
			    			    When you see the Mugshot icon in your tool tray, you're ready to share
			    			    links with friends!
			    			    <br/>
			    			    <br/>
			    			    <c:choose>
					                <c:when test="${browser.linuxRequested}">	
				    				    <img src="/images2/${buildStamp}/linuxtoolbar.gif"/>					            		    			
				    			    </c:when>
				    			    <c:otherwise>
				    				    <img src="/images2/${buildStamp}/tooltray.gif"/>				    				
				    			    </c:otherwise>
				    		    </c:choose>
			    		    </c:when>
			    		    <c:otherwise>
					            When you see this bubble appear, your Mugshot is working! Click the link on the bubble to set up your account. 
					            <br/>
	            		        <img src="/images2/${buildStamp}/minibubble_account.gif"/>
	            		    </c:otherwise>
	            	    </c:choose>
	            	</div>    			 
	            </td>
	        </tr>
	    </table>
	</td>
	</tr>
	</table>    
	<div id="dhDownloadFooter">
 		<a id="dhSkipDownload" href="javascript:dh.download.doDownload()">I can't install on this computer, skip download.</a> (Not recommended.)
 		<span class="dh-download-footer-text">| Get Mugshot for <a href="/download?platform=windows${urlParams}">Windows</a> | <a href="/download?platform=linux${urlParams}">Linux</a></span>
		<div class="dh-download-footer-text"><dht:notevil/></div>	
	</div>
</dht:body>

</html>
