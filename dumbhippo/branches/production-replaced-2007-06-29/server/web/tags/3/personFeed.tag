<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView" %>

<%-- http://www.w3.org/QA/Tips/use-links --%>
<link rel="alternate" type="application/rss+xml" title="${person.name}'s Mugshot" href="${baseUrl}/xml/userRSS?who=${person.viewPersonPageId}&participantOnly=true" />
<link rel="alternate" type="application/rss+xml" title="${person.name}'s Stacker" href="${baseUrl}/xml/userRSS?who=${person.viewPersonPageId}" />
