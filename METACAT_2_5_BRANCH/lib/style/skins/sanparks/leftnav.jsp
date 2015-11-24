<%@ include file="settings.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>SANParks - South African National Park Data Repository</title>
<link rel="stylesheet" type="text/css" href="<%=STYLE_SKINS_URL%>/sanparks/sanparks.css"/>
</head>

<body class="templateleftcolclass">
<table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0" align="center">
	<!-- Left Nav -->
	<tr class="leftnav" valign="middle">
		<td class="headermenu">Parks A-Z</td>
	</tr>
	<tr class="leftnav" valign="top">
		<td>
			
			<form action="<%=STYLE_SKINS_URL%>/sanparks/index.jsp" method="post" target="_top" name="orgForm">
				<input name="organizationScope" type="hidden" value="" />
				<table>
					<tr>
						<td>
							<div id="nav">
							<span class="home">
								<a href="#" onclick="orgForm.organizationScope.value='';orgForm.submit();">All</a>
							</span>	
							<ul class="level-1">
								<li>
									<a href="#" onclick="orgForm.organizationScope.value='SANParks, South Africa';orgForm.submit();">SANParks</a>
									<ul class="level-2">
										<li><a href="#" onclick="orgForm.organizationScope.value='SANParks, South Africa';orgForm.submit();">All</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Addo Elephant National Park, South Africa';orgForm.submit();">Addo Elephant National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Agulhas National Park, South Africa';orgForm.submit();">Agulhas National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Augrabies Falls National Park, South Africa';orgForm.submit();">Augrabies Falls National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Bontebok National Park, South Africa';orgForm.submit();">Bontebok National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Camdeboo National Park, South Africa';orgForm.submit();">Camdeboo National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Golden Gate Highlands National Park, South Africa';orgForm.submit();">Golden Gate Highlands National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Karoo National Park, South Africa';orgForm.submit();">Karoo National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Kgalagadi Transfrontier Park, South Africa';orgForm.submit();">Kgalagadi Transfrontier Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Knysna National Lake Area, South Africa';orgForm.submit();">Knysna National Lake Area</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Kruger National Park, South Africa';orgForm.submit();">Kruger National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Mapungubwe National Park, South Africa';orgForm.submit();">Mapungubwe National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Marakele National Park, South Africa';orgForm.submit();">Marakele National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Mokala National Park, South Africa';orgForm.submit();">Mokala National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Mountain Zebra National Park, South Africa';orgForm.submit();">Mountain Zebra National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Namaqua National Park, South Africa';orgForm.submit();">Namaqua National Park</a></li>
										<!-- <li><a href="#" onclick="orgForm.organizationScope.value='Richtersveld National Park, South Africa';orgForm.submit();">Richtersveld National Park</a></li> -->
										<li><a href="#" onclick="orgForm.organizationScope.value='Table Mountain National Park, South Africa';orgForm.submit();">Table Mountain National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Tankwa Karoo National Park, South Africa';orgForm.submit();">Tankwa Karoo National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Tsitsikamma National Park, South Africa';orgForm.submit();">Tsitsikamma National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='West Coast National Park, South Africa';orgForm.submit();">West Coast National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Wilderness National Park, South Africa';orgForm.submit();">Wilderness National Park</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Ai-|Ais/Richtersveld Transfrontier National Park, South Africa';orgForm.submit();">Ai-|Ais/Richtersveld Transfrontier National Park</a></li>
									</ul>
								</li>
								<li>
									<a href="#" onclick="orgForm.organizationScope.value='SAEON, South Africa';orgForm.submit();">SAEON</a>
									<ul class="level-2">
										<li><a href="#" onclick="orgForm.organizationScope.value='SAEON, South Africa';orgForm.submit();">All</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Arid Zone SAEON Node, South Africa';orgForm.submit();">Arid Zone SAEON Node</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Egagasini SAEON Node, South Africa';orgForm.submit();">Egagasini SAEON Node</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Elwandle SAEON Node, South Africa';orgForm.submit();">Elwandle SAEON Node</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Fynbos SAEON Node, South Africa';orgForm.submit();">Fynbos SAEON Node</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Grassland, wetland and forests SAEON Node, South Africa';orgForm.submit();">Grassland, wetland and forests SAEON Node</a></li>
										<li><a href="#" onclick="orgForm.organizationScope.value='Ndlovu SAEON Node, South Africa';orgForm.submit();">Ndlovu SAEON Node</a></li>
									</ul>
								</li>
							</ul>		
							</div>
						</td>
					</tr>
				</table>			
			</form>	
		</td>
	</tr>
</table>
</body>
</html>