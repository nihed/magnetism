<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="rating" required="true" type="com.dumbhippo.persistence.Rating"%>

 			<tr>
		      <td>${rating.id}</td>
		      <td>${rating.type}</td>
		      <td>TODO: post name</td>
		      <td>${rating.score}</td>
		      <td>${rating.reason}</td>
		    </tr>