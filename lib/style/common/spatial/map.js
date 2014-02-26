// global variables for the map
var map;
var control;

function initMap(GEOSERVER_URL, SERVLET_URL, qformat, bounds, additionalLayers) {
	if (!bounds) {
		bounds = new OpenLayers.Bounds(-180,-90,180,90);
	}
    map = new OpenLayers.Map('map', { 'maxExtent':bounds, 'maxResolution':'auto'});

    var metacat_points = new OpenLayers.Layer.WMS( "Metacat Doc Points",
        GEOSERVER_URL + "/wms",
        {layers: "data_points",
         transparent: "true", format: "image/gif"} );

    var metacat_bounds = new OpenLayers.Layer.WMS( "Metacat Doc Bounds",
    	GEOSERVER_URL + "/wms",
        {layers: "data_bounds",
         transparent: "true", format: "image/gif"} );

    var world_borders = new OpenLayers.Layer.WMS( "World Borders",
    	GEOSERVER_URL + "/wms",
        {layers: "world_borders",
         format: "image/jpeg"} );

    var ol_wms = new OpenLayers.Layer.WMS( "OpenLayers WMS", 
        "http://labs.metacarta.com/wms/vmap0",
        {layers: 'basic'} );

    // NOTE: as of Jan 2012, it seems that this WMS service requires basic authentication
    //var demis = new OpenLayers.Layer.WMS( "Demis World Map",
    //    "http://www2.demis.nl/WMS/wms.asp?wms=WorldMap",
    //    {layers: 'Bathymetry,Countries,Topography,Hillshading,Coastlines,Waterbodies,Inundated,Rivers,Streams,Builtup+areas,Railroads,Highways,Roads,Trails,Borders,Cities,Settlements,Airports'} );
	
	//map.addLayers([demis, ol_wms, world_borders, metacat_points, metacat_bounds]);	
	map.addLayers([world_borders, metacat_points, metacat_bounds]);	

	// perhaps a skin has added more layers
	if (additionalLayers) {
		map.addLayers(additionalLayers);
	}
	
    // off by default
	metacat_bounds.setVisibility(false);

    // get the drag box event
	//control = new OpenLayers.Control();
	control = new OpenLayers.Control({title: "Metacat spatial query", displayClass: "spatialQuery"});
	OpenLayers.Util.extend(
			control, {
			    draw: function () {
			        // this Handler.Box will intercept the shift-mousedown
			        // before Control.MouseDefault gets to see it
			        this.box = new OpenLayers.Handler.Box( control,
			        		{"done": this.notice}
                            //,{keyMask: OpenLayers.Handler.MOD_SHIFT}
			        		);
			        this.box.activate();
			    },
			    notice: function (bounds) {
			    	var min_px;
			        var max_px;
			        // check that it is a rectangle
			        if (bounds.left) { 
				        min_px = new OpenLayers.Pixel( bounds.left, bounds.bottom);
				        max_px = new OpenLayers.Pixel( bounds.right, bounds.top);
					} else {
						var tolerance = new OpenLayers.Pixel(3, 3);
					    min_px = new OpenLayers.Pixel( bounds.x - tolerance.x, bounds.y + tolerance.y);
					    max_px = new OpenLayers.Pixel( bounds.x + tolerance.x, bounds.y - tolerance.y);
					}
			        var min_ll = map.getLonLatFromPixel(min_px);
			        var max_ll = map.getLonLatFromPixel(max_px);
			      	//alert("longitude: " + round(mid_ll.lon,3) + " , latitude: " + round(mid_ll.lat,3) );
				    url = SERVLET_URL + '?action=spatial_query&xmin='+min_ll.lon+'&ymin='+min_ll.lat+'&xmax='+max_ll.lon+'&ymax='+max_ll.lat+'&skin=' + qformat;
				    OpenLayers.ProxyHost = '';
				    newwindow = 
					    window.open(
							    url,
							    'queryWin',
				                'height=600,width=800,status=yes,toolbar=yes,menubar=no,location=yes,resizable=yes,scrollbars=yes');
				
			    }
	});
	var tb = OpenLayers.Class(OpenLayers.Control.NavToolbar, {
		initialize: function() {
			var options = null;
			OpenLayers.Control.Panel.prototype.initialize.apply(this, [options]);
			this.addControls([
			                  new OpenLayers.Control.Navigation({'zoomWheelEnabled': false}),
			                  new OpenLayers.Control.ZoomBox(),
			                  control
			                  ]);
		}
	});
	map.addControl(new tb());
	//map.addControl(control);

	// some built-in controls
	//map.addControl(new OpenLayers.Control.NavToolbar());
	map.addControl(new OpenLayers.Control.LayerSwitcher());
	map.addControl(new OpenLayers.Control.MousePosition());

	// zoom to center
    if (!map.getCenter()) {
         map.zoomToMaxExtent();
    }

}

// for named location navigation
function zoomToLocation(locations) {
	var selectedLocation = locations.options[locations.selectedIndex].value;
	// add a comma for the rectangle
	selectedLocation = selectedLocation.replace(" ", ",")
	var bounds = OpenLayers.Bounds.fromString(selectedLocation);
    map.zoomToExtent(bounds);
}