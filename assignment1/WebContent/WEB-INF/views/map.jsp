<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<tags:layout activeTab="map">

<div class="row">
	<div class="col-xs-12 col-md-6 col-lg-6">
		<form class="form-inline" role="form">
			<div class="form-group">
				<label for="keyword" class="control-label">Keyword filtering </label>
				<div class="input-group">    
			    	<div class="input-group-addon"><span class="glyphicon glyphicon-filter"></span></div>
			    	<select id="keywords-select" name="keyword" class="form-control">
						<c:forEach var="keyword" items="${keywords}">
							<option>${keyword}</option>
						</c:forEach>
						<option>Show all</option>
					</select>
				</div>
			</div>
		</form>
	</div>
</div>
<div class="row">
	<div id="map" class="col-xs-12">
		
	</div>
</div>
</tags:layout>

<script>

$(function() {
	var heatmap = new Heatmap({containerId: "map"});
	var points = {};
	
	function loadPointsForKeyword(keyword, callback) {
		$.get("tweet_points",
			{keyword: keyword},
			function (jsonData) {
				data = JSON.parse(jsonData);
				points[keyword] = data;
				callback(keyword);
			}
		);
	} 
	
	function showPointsForOne(keyword) {
		if (points.hasOwnProperty(keyword)) {
			heatmap.clearHeatPoints();
			heatmap.addHeatPoints(points[keyword].latitudes, points[keyword].longitudes);	
		}
		else {
			loadPointsForKeyword(keyword, function(keyword) {
				heatmap.clearHeatPoints();
				heatmap.addHeatPoints(points[keyword].latitudes, points[keyword].longitudes);
			});	
		}
	}
	
	function showPointsForAll() {
		var requestsMade = 0;
		heatmap.clearHeatPoints();
		$.each($("#keywords-select")[0].options, function(i, option) {
			var keyword = $(option).val();
			if (keyword != "Show all") {
				if (points.hasOwnProperty(keyword)) {
					heatmap.addHeatPoints(points[keyword].latitudes, points[keyword].longitudes);		
				}
				else {
					loadPointsForKeyword(keyword, function(keyword) { 
						heatmap.addHeatPoints(points[keyword].latitudes, points[keyword].longitudes);
					});
				}
			}
		});
	}
	
	// load initial points
	loadPointsForKeyword($("#keywords-select").val(), function(keyword) {
		heatmap.addHeatPoints(points[keyword].latitudes, points[keyword].longitudes);
	});

	// on select box change
	$("#keywords-select").change(function() {
		var keyword = $(this).val();
		if (keyword == "Show all") {
			showPointsForAll();
		}
		else {
			showPointsForOne(keyword);
		}
	});
		
});

</script>




