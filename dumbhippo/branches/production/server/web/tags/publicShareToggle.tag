<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="defaultPublic" required="true" type="java.lang.Boolean"%>

<dh:png src="/images/${buildStamp}/worldShare.png" style="width: 48; height: 48; float: left;"/>
Bubble up posts sent to <b>The World</b>:<br/>
<form>
<dh:script module="dh.actions"/>
<input name="defaultPublicShare" type="radio" id="defaultPublicOn" 
       <c:if test="${defaultPublic}">checked="true"</c:if>
       onclick="dh.actions.setNotifyPublicShares(true);"> <label for="defaultPublicOn">Show me public shares</label><br/>
<input name="defaultPublicShare" type="radio" id="defaultPublicOff" 
       <c:if test="${!defaultPublic}">checked="true"</c:if>
       onclick="dh.actions.setNotifyPublicShares(false);"> <label for="defaultPublicOff">Don't bubble public shares</label><br/>
</form>

