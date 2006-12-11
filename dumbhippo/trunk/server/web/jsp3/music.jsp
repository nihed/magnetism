<%-- We keep /music around because old music radar embeds link to it.
     This should really be a redirect not a forward... --%>
<jsp:forward page="/person">
	<jsp:param name="who" value="${param.who}"/>
</jsp:forward>

