

var Heatmap = function(params) {
	this.map = new google.maps.Map(document.getElementById(params.containerId), {
	    center: params.center || new google.maps.LatLng(40.164971, -96.077127),
	    zoom: params.zoom || 4,
	    disableDefaultUI: null,
	    mapTypeControl: true,
	    styles: Heatmap.getMapStyles()
	});
}

Heatmap.getMapStyles = function generateGetMapStyles() {
  var styles;
  return function() {
    if (typeof styles === 'undefined') {
      styles = [
        {
          featureType: "poi",
          elementType: "labels",
          stylers: [ { visibility: "off" } ]
        },
        {
          featureType: "transit",
          stylers: [ { visibility: "off" } ]
        }
      ];
    }
    return styles;
  }
}();

///////////////////////////////////////////////////////////////////////////////
/// Instance definitions
///////////////////////////////////////////////////////////////////////////////

Heatmap.prototype.map = null;
Heatmap.prototype.heatmapLayer = null;

Heatmap.prototype.addHeatPoints = function(latitudes, longitudes) {
  var pointCount = latitudes.length;
  var latlngPoints = [];
  
  for (i=0; i<pointCount; i++) {
	  latlngPoints.push(new google.maps.LatLng(latitudes[i], longitudes[i]));
  }
  
  
  if (this.heatmapLayer == null) {
	  var pointArray = new google.maps.MVCArray(latlngPoints);
	  this.heatmapLayer = new google.maps.visualization.HeatmapLayer({
		  data: pointArray
	  });
	  this.heatmapLayer.set('dissipating', false);
	  this.heatmapLayer.set('radius', 1);
	  this.heatmapLayer.setMap(this.map);
  } 
  else {
	  var datapoints = this.heatmapLayer.getData().getArray();
	  var pointArray = new google.maps.MVCArray(datapoints.concat(latlngPoints));
	  this.heatmapLayer.setData(pointArray);
  }
}

Heatmap.prototype.clearHeatPoints = function() {
	if (this.heatmapLayer != null && this.heatmapLayer.getData().length > 0) {
		this.heatmapLayer.setData([]);
	} 
}

