<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<div id="dhSuggestGroupsPopup" class="dhInvisible">
	<div class="dh-title">SELECT GROUPS TO SUGGEST</div>
	<div class="dh-explanation-note">
	    By suggesting groups to <span id="dhSuggestGroupsInvitee"></span>
	    you are also offering them invitations to these groups.
	</div>
	<c:set var="count" value="1"/>  
	<c:choose>
	    <c:when test="${person.groups.size < 1}">
	        <c:out value="You have no groups of your own."/>
	    </c:when>
	    <c:otherwise>        
	        <c:forEach items="${person.groups.list}" var="group">
	            <%-- TODO use checked later, only if know that the group was already suggested --%>
	            <input id="groupCheckbox${count}" type="checkbox" value="${group.group.id}"/> 
	            <c:out value="${group.group.name}"/>
	            <br/>
                <c:set var="count" value="${count+1}"/>
            </c:forEach>
        </c:otherwise>
    </c:choose>        
    <br/>
	<div>
		<input type="button" id="dhSuggestGroupsOk" class="dhButton" 
		       value="Ok" onclick="dh.invitation.doSuggestGroups();"/>
		<input type="button" id="dhSuggestGroupsCancel" class="dhButton" 
		       value="Cancel" onclick="dh.invitation.cancelSuggestGroups();"/>
	</div>
</div>