<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="chatwindow" class="com.dumbhippo.web.pages.ChatWindowPage" scope="request"/>
<%-- only one of these params is expected at a time... chatId just means 
	"figure out whether it's post or group" which is less efficient but 
	some calling contexts might not know the type of chat --%>
<jsp:setProperty name="chatwindow" property="postId" param="postId"/>
<jsp:setProperty name="chatwindow" property="groupId" param="groupId"/>
<jsp:setProperty name="chatwindow" property="trackId" param="trackId"/>
<jsp:setProperty name="chatwindow" property="blockId" param="blockId"/>
<jsp:setProperty name="chatwindow" property="chatId" param="chatId"/>

<c:if test="${! chatwindow.aboutSomething}">
	<%-- no post/group/track, or invalid/not-allowed post/group/track --%>
	<dht:errorPage>Can't find this chat</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${chatwindow.title}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="chatwindow" iefixes="true"/>	
	<dh:script modules="dh.chatwindow,dh.event"/>
	<style type="text/css"> <%-- Vertical sizes are set here so we can do computed heights--%>
		#dhChatHeader {
			top: 0px;
			height: ${chatwindow.headerHeight}px;
		}

		#dhChatMessages {
			top: ${chatwindow.headerHeight}px;
			bottom: 110px;
			height: expression((document.body.clientHeight - ${chatwindow.headerHeight + 110}) + "px");
		}
	</style>
	<script type="text/javascript">
   	    dh.chatwindow.selfId = "${chatwindow.signin.userId}";
        dh.chatwindow.chatId = "${chatwindow.chatId}";
        <c:if test="${!disabled}">
			dh.event.addPageLoadListener(dhChatwindowInit);
		</c:if>
	</script>
</head>

<body scroll="no">
	<div id="dhChatHeader">
		<dht3:block block="${chatwindow.block}" offset="0" blockId="0" showFrom="true" chatHeader="true"/>
	</div>
	<div id="dhChatFooter">
		<div id="dhChatFooterInner">
			<div id="dhChatWhosAround">
				Who's around: <span id="dhChatUserList"></span>
			</div>
			<dht3:chatInput multiline="true" sendlabel="Add" onsend="dh.chatwindow.sendClicked()"/>
		</div>
	</div>
	<div id="dhChatMessages">
		<c:if test="${disabled}">
			<table class="dh-disabled-table">
			<tr>
			<td align="center" valign="center" class="dh-disabled-message">
			    Sorry, we're working to make things better!<br/>
				<a href="http://blog.mugshot.org/?cat=13" target="_blank">Check the blog for updates</a>
			</td>
			</tr>
			</table>
		</c:if>
		<div id="dhChatTooOld" style="display: none">
			<table class="dh-disabled-table">
			<tr>
			<td align="center" valign="center">
				<div class="dh-too-old-message">
					<c:choose>
						<c:when test="${browser.ie}">
						    Your version of the Mugshot software is too old. Please 
						    <a href="http://mugshot.org/download">install the latest version of Mugshot</a> 
						    then try again.
						</c:when>
						<c:otherwise>
							You need to <b>restart Firefox</b> so it uses the latest version of the
							Mugshot browser plugin. Close all Mugshot and Firefox windows,
						    then try reopening the "Quips and Comments" window.  If that
						    doesn't work, you may need to log out and log in again.
						</c:otherwise>
					</c:choose>
				</div>
			</td>
			</tr>
			</table>
		</div>
	</div>
</body>