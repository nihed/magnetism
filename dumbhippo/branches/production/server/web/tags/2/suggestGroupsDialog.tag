<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<div id="dhSuggestGroupsPopup" class="dhInvisible">
	<div id="dhSuggestGroupsTopDiv" class="dh-border">
			<div class="dh-content">
	<div class="dh-title">INVITE TO GROUPS</div>
	<div class="dh-explanation-note">
	    Select groups for <span id="dhSuggestGroupsInvitee"></span>
	</div>
	<div id="dhSuggestGroupsArea">
	    <c:set var="count" value="1"/>  
	    <c:choose>
	        <c:when test="${person.groups.size < 1}">
	            <c:out value="You have no groups of your own."/>
	        </c:when>
	        <c:otherwise>        
	            <c:forEach items="${person.groups.list}" var="group">
	                <label for="dhGroupCheckbox${count}"><input id="dhGroupCheckbox${count}" type="checkbox" value="${group.group.id}"/><c:out value="${group.group.name}"/></label>
	                <br/>
                    <c:set var="count" value="${count+1}"/>
                </c:forEach>
            </c:otherwise>
        </c:choose>   
    </div>         
	<div id="dhSuggestGroupsControls">
		<input type="button" id="dhSuggestGroupsOk" class="dhButton" 
		       value="Ok" onclick="dh.invitation.doSuggestGroups();"/>
		<input type="button" id="dhSuggestGroupsCancel" class="dhButton" 
		       value="Cancel" onclick="dh.invitation.cancelSuggestGroups();"/>
	</div>
	</div>
    </div>
</div>