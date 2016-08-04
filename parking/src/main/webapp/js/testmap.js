//map.js

//Set up some of our variables.
var map; 
var marker = false;
var server = "parking.allowed.org:8080";     

function initMap() {

    var centerOfMap = new google.maps.LatLng(45.43371388888889, -73.69053611111111);
    var uploadMarker;

    var options = {
      center: centerOfMap, //Set center.
      zoom: 17 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    google.maps.event.addListener(map, 'click', function(event) {
//        if (uploadMarker != null) {
//            uploadMarker.setMap(null);
//        }
        var clickedLocation = event.latLng;  
        uploadMarker = new google.maps.Marker({
                position: clickedLocation,
                map: map,
                draggable: true 
        });
        document.getElementById('lat1').value = clickedLocation.lat(); //latitude
        document.getElementById('lng1').value = clickedLocation.lng();
    });   
}

google.maps.event.addDomListener(window, 'load', initMap);