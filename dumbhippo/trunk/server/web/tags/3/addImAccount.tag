<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<div id="dhAddImLink">
	<a href="javascript:dh.account.showImAccountPopup()">
		<dh:png klass="dh-add-icon" src="/images3/${buildStamp}/add_icon.png" style="width: 10; height: 10; overflow: hidden;" />
		Add an account
	</a>
	<div id="dhAddImPopup" style="display: none">
		<div id="dhAddImClose">
			<a href="javascript:dh.account.closeImAccountPopup()"><img src="/images3/${buildStamp}/x-close.png" width="10" height="10"/></a>
		</div>
		<div id="dhAddImPopupHeader">ADD AN IM ACCOUNT</div>
		<form>
			<div class="im-account-type"><input type="radio" name="imAccountType" onclick="dh.account.setImAccountType('aim')" checked="1">AIM / ICQ</input></div>
			<div class="im-account-type"><input type="radio" name="imAccountType" onclick="dh.account.setImAccountType('gtalk')">Google Talk</input></div>
			<div class="im-account-type"><input type="radio" name="imAccountType" onclick="dh.account.setImAccountType('xmpp')">Other Jabber / XMPP</input><br/></div>
			<div class="im-account-content" id="dhAddAimContent">
				<a href="javascript:dh.account.aimVerify()">
					<dh:png klass="dh-add-icon" src="/images3/${buildStamp}/add_icon.png" style="width: 10; height: 10; overflow: hidden;" />
					IM our friendly bot to add a new screen name
				</a><br/>
				<div class="im-account-note">Note: we don't support email-address AIM accounts yet.</div>
				<div class="im-action-row">
					<input type="button" value="Close" onclick="dh.account.closeImAccountPopup()"></input>
				</div>
			</div>
			<div class="im-account-content" id="dhAddXmppContent" style="display: none;">
				<dht2:textInput id='dhXmppEntry'/><br/>
				<div class="im-action-row">
					<input type="button" value="Verify" onclick="dh.account.verifyXmpp()"></input>
				</div>
			</div>
		</form>
	</div>
</div>
