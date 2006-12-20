<%-- NOTE this is an XML page, not an HTML page --%>
<%@ page pageEncoding="UTF-8" contentType="text/xml" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<?xml version="1.0" encoding="UTF-8" ?> 
<Module>
  <ModulePrefs 
    title="Mugshot Stacker" height="200"
    description="Shows what your friends are doing online"
    author_email="feedback@mugshot.org"
    title_url="http://mugshot.org">
  </ModulePrefs>
  <Content type="url" href="${baseUrl}/google-stacker-content"/>
</Module>
