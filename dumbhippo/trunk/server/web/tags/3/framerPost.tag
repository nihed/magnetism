<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.views.PostView"%>
<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.PostBlockView"%>

<table id="dhBlockInfoTable" cellspacing="0" cellpadding="0" width="100%">
    <tr height="62px">
        <%-- 5 extra pixels are for the margin on the right --%>
        <td valign="top" width="65px">
            <dht:headshot person="${post.poster}" size="60"/>
		</td>
		<td valign="top">
		    <div id="dhBlockContent">
	        <dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/>
			<dht3:simpleBlockTitle block="${block}" oneLine="false" homeStack="false" linkClass="dh-framer-title" framerPostId="${post.post.id}"/>
			<%-- TODO: see if we need to truncate this, it used to be limited to 70 characters --%>
			<%-- it is overflow: hidden, so at the very least should set height on this table row, so that --%>
			<%-- we have enough space for the list of people it was sent to, and who's around --%> 
			<div class="dh-stacker-block-header-description">
			    ${block.descriptionAsHtml}
			</div>    
			</div>
        </td>       
	</tr>
	<tr>
	    <td colspan="2">
	        <div class="dh-block-details">
	            From <span class="dh-entity-list"><a href="${post.poster.homeUrl}" id="dhBlockSender" class="dh-entity-${post.poster.identifyingGuid}" target="_blank"><c:out value="${post.poster.name}"/></a></span>	        
			    Sent to <span class="dh-entity-list"><dh:entityList value="${post.recipients}" separator=", " cssClass="dh-entity-" target="_blank"/></span>
			    ${block.postTimeAgo}
			</div>    			
	    </td>
	</tr>    
</table>