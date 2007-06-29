<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<div id="dhCreateGroupPopup" class="dhItemBox dhInvisible">
	<div class="dhLabel">New <u>G</u>roup Name</div>
	<div>
		<input id="dhCreateGroupName" 
	    	   onkeyup="dh.util.updateButton('dhCreateGroupName', 'dhCreateGroupButton');"/>
		<input type="button" id="dhCreateGroupButton" accesskey="g" class="dhButton" 
		       value="Create" onclick="dh.sharelink.doCreateGroup();" disabled="true"/>
	</div>
	<table cellpadding="0" cellspacing="0">
	    <tr>
	    <td>
	        Privacy:
	    </td>
	    <td>    
		    <input type="radio" id="dhCreateGroupPublicRadio" name="groupAccess" checked="checked"/>
		    <a href="javascript:dh.util.selectCheckBox('dhCreateGroupPublicRadio');dh.sharelink.updateGroupTip();">Public</a>
		</td>
		<td>
		    <input type="radio" id="dhCreateGroupPrivateRadio" name="groupAccess" />
		    <a href="javascript:dh.util.selectCheckBox('dhCreateGroupPrivateRadio');dh.sharelink.updateGroupTip();">Private</a>	           			
		</td>
		</tr>
		<tr>    
		<td>	
	        Membership:
	    </td>
	    <td>
		    <input type="radio" id="dhCreateGroupOpenRadio" name="groupMembership" checked="checked"/>
		    <a href="javascript:dh.util.selectCheckBox('dhCreateGroupOpenRadio');dh.sharelink.updateGroupTip();">Open</a>
		</td>
		<td>    
		    <input type="radio" id="dhCreateGroupByInvitationRadio" name="groupMembership" />
		    <a href="javascript:dh.util.selectCheckBox('dhCreateGroupByInvitationRadio');dh.sharelink.updateGroupTip();">By Invitation</a>
	    </td>
	    </tr>
	</table>           									            
	<div id="dhOpenPublicGroupTip" class="dh-help-bubble">
	    <dh:png style="left: 27px; width: 27px; height: 21px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
		<div class="dh-help-bubble-message">
			Public groups are for <strong>communities</strong>, <strong>co-workers</strong>, and others who don't
		    mind people seeing the links they are sharing. Anyone can <strong>join</strong> an "Open" group.
		</div><!-- help bubble message -->
	</div><!-- help bubble -->
	<!-- EEEEEEEEEEEEEEEEEEEEE -->
	<div id="dhByInvitationPublicGroupTip" class="dh-help-bubble">
		<dh:png style="left: 27px; width: 27px; height: 21px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
		<div class="dh-help-bubble-message">
			Public groups are for <strong>communities</strong>, <strong>co-workers</strong>, and others who don't
		    mind people seeing the links they are sharing. Anyone can <strong>follow</strong> a "By Invitation" group, they need an <strong>invitation</strong> to join.
		</div><!-- help bubble message -->
	</div><!-- help bubble -->
    <div id="dhPrivateGroupTip" class="dh-help-bubble">
		<dh:png style="left: 27px; width: 27px; height: 21px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
		<div class="dh-help-bubble-message">
			Private groups are for <strong>friends</strong>, <strong>families</strong>, and super secret CIA agents. They are by invitation only.
		</div><!-- help bubble message -->
	</div><!-- help bubble -->			
	<p id="dhCreateGroupStatus" class="dhStatusLabel dhInvisible"></p>
</div>
<a id="dhCreateGroupLink" class="dhActionLink dhInvisible"
  	onmouseover="dh.sharelink.highlightPossibleGroup();"
	onmouseout="dh.sharelink.unhighlightPossibleGroup();"
	href="javascript:dh.sharelink.toggleCreateGroup();">
	Create Group
</a>
<a id="dhAddMemberLink" class="dhActionLink dhInvisible"
	onmouseover="dh.sharelink.highlightPossibleGroup();"
	onmouseout="dh.sharelink.unhighlightPossibleGroup();"
 	href="javascript:dh.sharelink.doAddMembers();">
	Add <span id="dhAddMemberDescription"></span> to <span id="dhAddMemberGroup"></span>
</a>