<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="underline" required="false" type="java.lang.Boolean" %>

<dht:actionLink oneLine="${oneLine}" underline="${underline}" href="javascript:dh.actions.signOut();" title="Keep others from using your account on this computer">Log out</dht:actionLink>