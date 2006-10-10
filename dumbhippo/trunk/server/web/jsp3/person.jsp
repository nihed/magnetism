<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:personPage pageName="Overview" stackSize="5">
	<c:choose> 
		<c:when test="${person.contacts.size > 0}">
			<c:forEach items="${person.pageableContactStacks.results}" var="personStack" varStatus="stackIdx">
			    <%-- stack with stackOrder 1 is the stack for the person --%>
			    <dht3:personStack contact="${personStack.contact}" stack="${personStack.stack}" stackOrder="${stackIdx.count + 2}"/>
			</c:forEach>
  		    <dht:expandablePager pageable="${person.pageableContactStacks}" anchor="dhStackedContacts"/>
		</c:when>
		<c:otherwise>
		    <c:choose>
			    <c:when test="${person.signin.user.account.invitations > 0}">
				    <p>Email <a href="/invitation">invites</a> to some friends</p>
			    </c:when>
			    <c:otherwise>
				    <p>No Mugshot friends?  Try using the search box to find people you may know by email address,
				       or <a href="/public-groups">browse public groups</a>.</p>
			    </c:otherwise>
		    </c:choose>
		</c:otherwise>
	</c:choose>
</dht3:personPage>
</html>