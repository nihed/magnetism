<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- This tag is used by sharelink.jsp and sharephotoset.jsp.  It
     is the HTML container for a recipient list; requires the
     JavaScript dh.sharelink to be loaded too --%>
<%-- TODO reindent --%>     
			<div id="dhRecipientListArea" class="dhBackgroundBox">		
				<div id="dhRecipientList"> </div>
				
				<div id="dhCreateGroupPopup" class="dhItemBox dhInvisible">
					<div class="dhLabel">New <u>G</u>roup Name</div>
					<div>
						<input id="dhCreateGroupName"/>
						<input type="button" accesskey="g" class="dhButton" value="Create" 
							onclick="dh.sharelink.doCreateGroup();"/>
					</div>
					<div id="dhCreateGroupAccessButtons">
						<input type="radio" id="dhCreateGroupPrivateRadio" name="groupAccess" />
						<a href="javascript:dh.util.toggleCheckBox('dhCreateGroupPrivateRadio');dh.sharelink.updateAccessTip();">Private</a>
						<input type="radio" id="dhCreateGroupPublicRadio" name="groupAccess" checked="checked"/>
						<a href="javascript:dh.util.toggleCheckBox('dhCreateGroupPublicRadio');dh.sharelink.updateAccessTip();">Public</a>
			
						<div id="dhPrivateGroupAccessTip" class="dh-help-bubble">
							<dh:png style="left:27px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png" style="width: 27; height: 21;"/>
							<div class="dh-help-bubble-message">
								Private groups are for <strong>families</strong> and super secret CIA agents
								<a href="/privacy" target="_blank">privacy</a>
							</div><!-- help bubble message -->
						</div><!-- help bubble -->
						<div id="dhPublicGroupAccessTip" class="dh-help-bubble">
							<dh:png style="left:110px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
							<div class="dh-help-bubble-message">
								Public groups are for <strong>friends</strong>, <strong>co-workers</strong>, and others who don't
								mind people seeing the links they are sharing.
								<a href="/privacy" target="_blank">privacy</a>
							</div><!-- help bubble message -->
						</div><!-- help bubble -->
					</div>
					<p id="dhCreateGroupStatus" class="dhStatusLabel dhInvisible"></p>
				</div>
				<a id="dhCreateGroupLink" class="dhActionLink dhInvisible"
					 	onmouseover="dh.sharelink.highlightPossibleGroup();"
				 		onmouseout="dh.sharelink.unhighlightPossibleGroup();"
					 	href="javascript:dh.sharelink.toggleCreateGroup();">
				 	Create Group from These
				</a>
				<a id="dhAddMemberLink" class="dhActionLink dhInvisible"
		 				onmouseover="dh.sharelink.highlightPossibleGroup();"
					 	onmouseout="dh.sharelink.unhighlightPossibleGroup();"
		 				href="javascript:dh.sharelink.doAddMembers();">
					 Add <span id="dhAddMemberDescription"></span> to <span id="dhAddMemberGroup"></span>
				</a>
			</div><!-- end of div containing create group link and recipient list -->