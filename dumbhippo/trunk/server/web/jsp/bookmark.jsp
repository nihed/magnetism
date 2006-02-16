<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="bookmark" class="com.dumbhippo.web.BookmarkPage" scope="request"/>

<head>
        <title>Bookmark This Link</title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">
	<div id="dhMainArea">
		<dht:logo/>

		<dht:toolbar/>
	
		<h2>Bookmark this link:</h2>
		
		<p>
		&nbsp;
		</p>
		
		<p style="text-align: center;">
			<a href="javascript:window.open('${bookmark.baseUrl}/sharelink?v=1&url='+encodeURIComponent(location.href)+'&title='+encodeURIComponent(document.title)+'&next=close','_NEW','menubar=no,location=no,toolbar=no,scrollbars=yes,status=no,resizable=yes,height=450,width=550,top='+((screen.availHeight-450)/2)+',left='+((screen.availWidth-550)/2));void(0);" style="font-size: 200%;">Dumb Hippo It!</a>
		<br/>
		Use the bookmark to share a web site with your friends.
		</p>
		
		<p>
		&nbsp;
		</p>
		
		<p><b>Tip:</b>
	
			<c:choose>
				<c:when test="${bookmark.browser.geckoAtLeast10}">
					To bookmark the link in Firefox, click it with the <b>right</b> mouse button
					and choose "Bookmark This Link..." from the menu. Where it says "Create In" choose the 
					option "Personal Toolbar Folder" to create a toolbar button.
					<br/><br/>
					<b>Advanced Tip:</b> You can also drag-and-drop the link onto the Bookmarks Toolbar.
					<br/><br/>
					<b>Troubleshooting:</b>
					If the bookmark doesn't show up on your toolbar, try going to the View menu
					and under Toolbars be sure Bookmarks Toolbar has been checked.
					<br/>
					If you have a bookmarks toolbar but the DumbHippo link is not on it,
					try choosing Manage Bookmarks from the Bookmarks menu, and 
					moving the bookmark to your Personal Toolbar folder.
				</c:when>
				<c:when test="${bookmark.browser.ieAtLeast60}">
					To bookmark the link in Internet Explorer, click it with the <b>right</b> mouse
					button and choose "Add to Favorites..." from the menu.
					<br/>
					With Internet Explorer, you can also <a href="/welcome">download the DumbHippo 
					application</a> instead of using this bookmark.
				</c:when>
				<c:otherwise>
					To bookmark the link in most browsers, click it with the <b>right</b>
					mouse button and choose "Add to Favorites..." or "Bookmark This Link..."
				</c:otherwise>
			</c:choose>
	
		</p>
	</div>
	<dht:bottom/>
</div>
	
</body>
</html>

