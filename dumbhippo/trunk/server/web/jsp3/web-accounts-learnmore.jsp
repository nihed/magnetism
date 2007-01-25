<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Web Accounts - Learn More</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="download"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="web-accounts-learnmore">
	<dht3:shinyBox color="grey">
		<div class="dh-learnmore-section dh-learnmore-section-top">
			<div class="dh-learnmore-header">Web Accounts</div>
			<dht3:learnMoreNextStep page="webAccounts"/>
			<c:if test="${signin.valid}">
			    <span>
			        Add external Web accounts on <i><a href="/account">your account page</a></i>.
			    </span>
			</c:if>   
			We currently support blogs, Facebook, Flickr, and YouTube, with more to come soon.     
		</div>
		<hr size="1" color="#999999" style="margin: 5px 0px">
		<div class="dh-learnmore-section">
			<div class="dh-learnmore-header">Public Accounts</div>
			<div class="dh-learnmore-illustration">
			    <img src="/images3/${buildStamp}/public_accounts_sample.jpg"/></a>
			</div>
			<div class="dh-learnmore-explanation">
			    <p>When you post new blog entries, or upload Flickr photos and YouTube videos, your Mugshot friends 
			    will see those updates on their Mugshot pages and Stackers. You'll also get those updates, to see 
			    how they are displayed to friends. These kinds of updates are visible to anyone, just like on the 
			    sites they come from.</p>
			</div>
			<div class="dh-grow-div-around-floats"></div>		
		</div>		
		<hr size="1" color="#999999" style="margin: 5px 0px">
		<div class="dh-learnmore-section">
			<div class="dh-learnmore-header">Private Accounts</div>
			<div class="dh-learnmore-illustration">
			    <img src="/images3/${buildStamp}/private_accounts_sample.jpg"/></a>			
			</div>
			<div class="dh-learnmore-explanation">
			    <p>Updates from external accounts with private information, like Facebook, will only be visible 
			    to the account owner. Private updates are indicated by a lock icon.</p>          
			    <c:if test="${signin.valid}">
                    <p>Log in to your Facebook account through Mugshot on <i><a href="/account">your account page</a></i> 
                    to receive updates. If you have a Facebook account listed in your Mugshot, you'll get a 
                    Stacker block once a day reminding you to log in. After you log in, a Stacker block will 
                    confirm it.</p>
                </c:if>    
			</div>
			<div class="dh-grow-div-around-floats"></div>			
		</div>		
		<hr size="1" color="#999999" color="#666666" style="margin: 5px 0px">
		<dht3:learnMoreOptions exclude="webAccounts"/>
	</dht3:shinyBox>
</dht3:page>
