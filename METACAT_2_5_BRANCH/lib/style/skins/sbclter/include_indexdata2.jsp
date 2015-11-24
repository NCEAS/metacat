<%@ page language="java" contentType="text/html" %>
<%
// these hashes contain data to be used by sbc's metacat
// index page. Used ~2005-2007, instead of metacat queries.

// create arrayLists for each research area data packages
  ArrayList hydrology_list = new ArrayList();
  ArrayList streamchemistry_list = new ArrayList();
  ArrayList gis_list = new ArrayList();  
  ArrayList biogeochemistry_list = new ArrayList();  // not used
  ArrayList biogeochemistry_core_list = new ArrayList();
  ArrayList biogeochemistry_campaign_list = new ArrayList();
  ArrayList biomasspp_phyto_list = new ArrayList();
  ArrayList biomasspp_kelp_list = new ArrayList();
  ArrayList population_list = new ArrayList();
  ArrayList foodweb_list = new ArrayList();

  // DEFINE THE DATA PACKAGES:
  // a.2 hydrology
  Map dp1 = new HashMap();
  dp1.put("name","NCDC Climate Data");
  dp1.put("docid","knb-lter-sbc.1");
  hydrology_list.add(dp1);

  Map dp2 = new HashMap();
  dp2.put("name","High-frequency precipitation in key SBC Watersheds");
  dp2.put("docid","knb-lter-sbc.2");
  hydrology_list.add(dp2);

  Map dp3 = new HashMap();
  dp3.put("name","Daily precipitation from Santa Barbara County Flood Control (Public Works Department)");
  dp3.put("docid","knb-lter-sbc.3");
  hydrology_list.add(dp3);

  //Map dp4 = new HashMap();
  //dp4.put("name","SBCLTER Stream discharge in key Watersheds");
  //dp4.put("docid","knb-lter-sbc.4");
  //hydrology_list.add(dp4);

  Map dp5 = new HashMap();
  dp5.put("name","USGS Stream Discharge (links to source)");
  dp5.put("docid","knb-lter-sbc.5");
  hydrology_list.add(dp5);

  // a.1 stream chemistry 
  Map dp6 = new HashMap();
  dp6.put("name","Stream Chemistry in the Santa Barbara Coastal Drainage Area");
  dp6.put("docid","knb-lter-sbc.6");
  dp6.put("pi", "Melack");   streamchemistry_list.add(dp6);

  
  //a.3 watershed characteristics
  Map dp7 = new HashMap();
  dp7.put("name","Watershed Characteristics: GIS Layers");
  dp7.put("docid","knb-lter-sbc.7");
  gis_list.add(dp7);


   // a.4 biogeochemistry
   Map dp2001 = new HashMap();
   dp2001.put("name","Moored CTD and ADCP: Arroyo Quemado, 2001-2004 (AQM)");
   dp2001.put("docid","knb-lter-sbc.2001");
   dp2001.put("pi", "Washburn, Siegel, Brzezinksi");
   dp2001.put("queryapp_controller","moorings_all");
   biogeochemistry_core_list.add(dp2001);

   Map dp2005 = new HashMap();
   dp2005.put("name","Moored CTD and ADCP: Arroyo Quemado, 2004-ongoing (ARQ)");
   dp2005.put("docid","knb-lter-sbc.2005");
   dp2005.put("pi", "Washburn, Siegel, Brzezinksi");
   dp2005.put("queryapp_controller","moorings_all");
   biogeochemistry_core_list.add(dp2005);
 
   Map dp2002 = new HashMap();
   dp2002.put("name","Moored CTD and ADCP: Naples, 2001-ongoing");
   dp2002.put("docid","knb-lter-sbc.2002");
   dp2002.put("pi", "Washburn, Siegel, Brzezinksi");
   dp2002.put("queryapp_controller","moorings_all");
  biogeochemistry_core_list.add(dp2002);
 
   Map dp2003 = new HashMap();
  dp2003.put("name","Moored CTD and ADCP: Arroyo Burro, intermittent since 2004");
  dp2003.put("docid","knb-lter-sbc.2003");
  dp2003.put("pi", "Washburn, Siegel, Brzezinksi");
   dp2003.put("queryapp_controller","moorings_all");
   biogeochemistry_campaign_list.add(dp2003);
 
  Map dp2004 = new HashMap();
  dp2004.put("name","Moored CTD and ADCP: Carpinteria, 2001-ongoing");
  dp2004.put("docid","knb-lter-sbc.2004");
  dp2004.put("pi", "Washburn, Siegel, Brzezinksi");
  dp2004.put("queryapp_controller","moorings_all");
  biogeochemistry_core_list.add(dp2004);

  Map dp9 = new HashMap();
  dp9.put("name","Ocean: Links to Catalogs of Local Area Imagery");
  dp9.put("docid","knb-lter-sbc.9");
  biogeochemistry_core_list.add(dp9);

  Map dp10 = new HashMap();
  dp10.put("name","Ocean: Nearshore Water Profiles: CTD (1m bins, all stations)");
  dp10.put("docid","knb-lter-sbc.10");
  dp10.put("queryapp_controller","nearshore_ctd_profiles");
  biogeochemistry_core_list.add(dp10);

  // this one is temporary, so that the query interface for both tables can show
   Map dp10a = new HashMap();
   dp10a.put("name","Ocean: Nearshore Water Profiles: Rosette bottle samples (+ CTD, all stations)");
   dp10a.put("docid","knb-lter-sbc.10");
   dp10a.put("queryapp_controller","nearshore_rosette_profiles");
   biogeochemistry_core_list.add(dp10a);
		   

  Map dp13 = new HashMap();
  dp13.put("name","Reef: Bottom Temperature (all stations)");
  dp13.put("docid","knb-lter-sbc.13");
  dp13.put("queryapp_controller","bottom_temperature");
  biogeochemistry_core_list.add(dp13);

// b.2 primary producton - phytoplankton
  Map dp1006 = new HashMap();
  dp1006.put("name","Ocean: Cruise in the SB Channel LTER06");
  dp1006.put("docid","knb-lter-sbc.1006");
  dp1006.put("pi", "Carlson, Washburn, Siegel, Brzezinksi");
  biomasspp_phyto_list.add(dp1006);

// b.1 primary producton - kelp
  Map dp21 = new HashMap();
  dp21.put("name","Reef: Kelp Net Primary Production");
  dp21.put("docid","knb-lter-sbc.21");
  dp21.put("pi", "Reed");
  biomasspp_kelp_list.add(dp21);


//c.1 population dynamics
  Map dp15 = new HashMap();
  dp15.put("name","Cover of sessile organisms, UPC, Annual Summer survey");
  dp15.put("docid","knb-lter-sbc.15");
  dp15.put("pi", "Reed");
  population_list.add(dp15);
   
  Map dp17 = new HashMap();
  dp17.put("name","Fish Abundance, year-round survey");
  dp17.put("docid","knb-lter-sbc.17");
  dp17.put("pi", "Reed");
  population_list.add(dp17); 
  
  Map dp18 = new HashMap();
  dp18.put("name","Abundance and Size of Giant Kelp (Macrocystis pyrifera), Annual Summer survey");
  dp18.put("docid","knb-lter-sbc.18");
  dp18.put("pi", "Reed");
  population_list.add(dp18); 
 
  Map dp19 = new HashMap();
  dp19.put("name","Invertebrate and algal density, Annual Summer survey");
  dp19.put("docid","knb-lter-sbc.19");
  dp19.put("pi", "Reed");
  population_list.add(dp19);

//c.2 population dynamics
    Map dp14 = new HashMap();
  dp14.put("name","Reef: Historical Kelp Database of giant kelp (Macrocystis pyrifera) biomass in California and Mexico");
  dp14.put("docid","knb-lter-sbc.14");
  dp14.put("pi", "Reed");
  biomasspp_kelp_list.add(dp14);

//c.2 foodwebs
  Map dp12 = new HashMap();
  dp12.put("name","Foodweb studies with stable isotopes");
  dp12.put("docid","knb-lter-sbc.12");
  dp12.put("pi", "Reed");
  foodweb_list.add(dp12);


pageContext.setAttribute("hydrology_dps", hydrology_list);
pageContext.setAttribute("streamchemistry_dps", streamchemistry_list);
pageContext.setAttribute("gis_dps", gis_list);
pageContext.setAttribute("biogeochemistry_dps", biogeochemistry_list);
pageContext.setAttribute("biogeochemistry_core_dps", biogeochemistry_core_list);
pageContext.setAttribute("biogeochemistry_campaign_dps", biogeochemistry_campaign_list);
pageContext.setAttribute("biomasspp_phyto_dps", biomasspp_phyto_list);
pageContext.setAttribute("biomasspp_kelp_dps", biomasspp_kelp_list);
pageContext.setAttribute("population_dps", population_list);
pageContext.setAttribute("foodweb_dps", foodweb_list);





%>


<%
  // Create an ArrayList with play data
//  ArrayList dp_list = new ArrayList();
  // Map dp1 = new HashMap();
  // dp1.put("name","NCDC Climate Data");
  // dp1.put("docid","knb-lter-sbc.1");
//    ArrayList hab_list = new ArrayList();
//    Map habs = new HashMap();
//    habs.put("","Watershed");
//    habs.put("","Beach"); //    hab_list.add(habs);
//    dp1.put("habitats",hab_list);
// dp_list.add(dp1);
//
//  Map dp12 = new HashMap();
//  dp12.put("name","Kelp NPP");
//  dp12.put("docid","knb-lter-sbc.12");
//  dp_list.add(dp12);
//
//  Map dp1006 = new HashMap();
//  dp1006.put("name","Cruise in the SB Channel LTER06"); //  dp1006.put("docid","knb-lter-sbc.1006");
//  dp_list.add(dp1006);
//
//  pageContext.setAttribute("dataPackages", dp_list);
%>

