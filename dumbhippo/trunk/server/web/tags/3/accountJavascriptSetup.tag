<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="account" required="true" type="com.dumbhippo.web.pages.AccountPage" %>

<dh:script modules="dh.account,dh.password"/>
<script type="text/javascript">
	dh.account.active = ${signin.active};
	dh.password.active = ${signin.active};
	dh.formtable.currentValues = {
		'dhUsernameEntry' : <dh:jsString value="${signin.viewedUserFromSystem.name}"/>,
		'dhBioEntry' : <dh:jsString value="${signin.user.account.bio}"/>,
		'dhMusicBioEntry' : <dh:jsString value="${signin.user.account.musicBio}"/>,
		'dhWebsiteEntry' : <dh:jsString value="${account.websiteUrl}"/>,
		'dhBlogEntry' : <dh:jsString value="${account.blogUrl}"/>
	};
	dh.account.userId = <dh:jsString value="${signin.user.id}"/>
	dh.account.reloadPhoto = function() {
		dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer')], 60);
	}
</script>
