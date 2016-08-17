//map.js

//Set up some of our variables.
var map; 
var marker = false;
var server = "parking.allowed.org:8080";     
var uploadMarker;

function initMap() {

    var centerOfMap = new google.maps.LatLng(45.43371388888889, -73.69053611111111);
    

    var options = {
      center: centerOfMap, //Set center.
      zoom: 17 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    google.maps.event.addListener(map, 'click', function(event) {

        var clickedLocation = event.latLng;  
        uploadMarker = new google.maps.Marker({
                position: clickedLocation,
                map: map,
                draggable: true 
        });

        google.maps.event.addListener(uploadMarker, 'click', function(event){
                displayInfo();
        });
    });   
}

function displayInfo() {
    var infoContent=document.createElement('div'); 
    infoContent.innerHTML="<p>Hello World</p>"; 
    infoContent.onclick=test1;                
    var infowindow = new google.maps.InfoWindow({
        content: infoContent,
    });

    infowindow.open(map, uploadMarker);
}

function test1() {
    console.log("Hello world clicked!");
    document.getElementById('form').innerHTML = "<form><input type=\"radio\" name=\"action\" value=\"edit\" checked>Edit Sign<br> \
                                                <input type=\"radio\" name=\"action\" value=\"delete\">Delete Sign<br> \
                                                <input type=\"submit\" value=\"Submit\"></form>";
}

google.maps.event.addDomListener(window, 'load', initMap);