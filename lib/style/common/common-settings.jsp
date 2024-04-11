<%@ page errorPage="jsperrorpage.html"%>
<%@page import="edu.ucsb.nceas.metacat.properties.PropertyService,edu.ucsb.nceas.metacat.util.SystemUtil"%>

<%
	// GLOBAL CONSTANTS FOR ALL METACAT SKINS
	// These can be overridden by individual javascripts in each skin
	String COMMON_SEARCH_METACAT_POST_FIELDS = "<input type=\"hidden\" name=\"action\"        value=\"query\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"originator/individualName/surName\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"originator/individualName/givenName\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"originator/organizationName\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"creator/individualName/surName\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"creator/organizationName\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/title\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"dataset/title/value\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"keyword\"\\>\n"
			+ "<input type=\"hidden\" name=\"returnfield\"   value=\"keyword/value\"\\>\n"
			+ "<input name=\"returnfield\" value=\"creator/individualName/givenName\" type=\"hidden\">\n"
			+ "<input name=\"returnfield\" value=\"idinfo/citation/citeinfo/title\" type=\"hidden\">\n"
			+ "<input name=\"returnfield\" value=\"idinfo/citation/citeinfo/origin\" type=\"hidden\">\n"
			+ "<input name=\"returnfield\" value=\"idinfo/keywords/theme/themekey\" type=\"hidden\">\n"
			+ "<input name=\"returndoctype\" value=\"metadata\" type=\"hidden\">\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"eml://ecoinformatics.org/eml-2.1.1\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"eml://ecoinformatics.org/eml-2.1.0\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"eml://ecoinformatics.org/eml-2.0.1\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"eml://ecoinformatics.org/eml-2.0.0\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"-//NCEAS//eml-dataset-2.0//EN\"\\>\n"
			+ "<input type=\"hidden\" name=\"returndoctype\" value=\"-//NCEAS//resource//EN\"\\>\n";

	String SIMPLE_SEARCH_METACAT_POST_FIELDS = "<input type=\"hidden\" name=\"operator\"      value=\"UNION\"\\>\n"
			+ COMMON_SEARCH_METACAT_POST_FIELDS;

	String ADVANCED_SEARCH_METACAT_POST_FIELDS = "<input type=\"hidden\" name=\"operator\"      value=\"INTERSECT\"\\>\n"
			+ COMMON_SEARCH_METACAT_POST_FIELDS;

	// if true, POST variables echoed at bottom of client's browser window in a big yellow box
	boolean DEBUG_TO_BROWSER = false;

	// label for logout form button when user *is* logged in:
	String LOGOUT_LABEL = "Logout";

	// label for login form button when user is *not* logged in:
	String LOGIN_LABEL = "Login";

	// last part of LDAP username to be appended after organization
	String LDAP_DOMAIN = ",dc=ecoinformatics,dc=org";

	String KNB_SITE_URL = PropertyService.getProperty("application.knbSiteURL");
	String CONTEXT_NAME = PropertyService.getProperty("application.context");
	String DEFAULT_STYLE = PropertyService.getProperty("application.default-style");
	String SERVER_URL = SystemUtil.getServerURL();
	String CGI_URL = SystemUtil.getCGI_URL();
	String CONTEXT_URL = SystemUtil.getContextURL();
	String SERVLET_URL = SystemUtil.getServletURL();
	String STYLE_SKINS_URL = SystemUtil.getStyleSkinsURL();
	String STYLE_COMMON_URL = SystemUtil.getStyleCommonURL();
	String DEFAULT_STYLE_URL = SystemUtil.getDefaultStyleURL();
	String USER_MANAGEMENT_URL = SystemUtil.getUserManagementUrl();

%>

