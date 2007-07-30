<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="account" required="true" type="com.dumbhippo.web.pages.AccountPage" %>

<dh:script modules="dh.account,dh.password"/>
<script type="text/javascript">
	dh.account.active = ${signin.active};
	dh.password.active = ${signin.active};
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
	dh.account.initialNetflixUrl = <dh:jsString value="${account.netflixFeedUrl}"/>;
	dh.account.initialNetflixHateQuip = <dh:jsString value="${account.netflixHateQuip}"/>;	
	dh.account.initialGoogleReaderUrl = <dh:jsString value="${account.googleReaderUrl}"/>;
	dh.account.initialGoogleReaderHateQuip = <dh:jsString value="${account.googleReaderHateQuip}"/>;
	dh.account.initialPicasaName = <dh:jsString value="${account.picasaName}"/>;
	dh.account.initialPicasaHateQuip = <dh:jsString value="${account.picasaHateQuip}"/>;
	dh.account.initialAmazonUrl = <dh:jsString value="${account.amazonUrl}"/>;
	dh.account.initialAmazonHateQuip = <dh:jsString value="${account.amazonHateQuip}"/>;
</script>
