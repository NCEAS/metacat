<%@page import="edu.ucsb.nceas.metacat.properties.PropertyService"%>

<% 

/**
 *  Redirect to the configured UI context
 */
 
String metacatUiContext = PropertyService.getProperty("ui.context");
String redirectURI = "/" + metacatUiContext;

%>
<script type="text/javascript">
	document.location.href="<%= redirectURI %>";
</script>