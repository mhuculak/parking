//map.js

//Set up some of our variables.
var map; 
var marker = false;
var server = "parking.allowed.org";
var port = "8080";   

function initMap() {

    var centerOfMap = new google.maps.LatLng(45.43371388888889, -73.69053611111111);

    var options = {
      center: centerOfMap, //Set center.
      zoom: 17 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    port = document.getElementById('port').value;
    console.log("query db using port "+port);
    httpGetAsync("http://"+server+":"+port+"/parking/main/signs", processSigns);

    google.maps.event.addListener(map, 'click', function(event) {        
        var clickedLocation = event.latLng;
        if (marker === false) {
            marker = new google.maps.Marker({
                position: clickedLocation,
                map: map,
                draggable: true 
            });
            google.maps.event.addListener(marker, 'dragend', function(event){
                markerLocation();
            });
        }
        else {
            marker.setPosition(clickedLocation);
        }
        markerLocation();
    });   
}

function markerLocation(){
    //Get location.
    var currentLocation = marker.getPosition();
    document.getElementById('lat').value = currentLocation.lat(); //latitude
    document.getElementById('lng').value = currentLocation.lng(); //longitude
}

function updateSignPosition(sign) {
    selectedMarker = sign.marker;
    var loc = sign.marker.getPosition();
    console.log("marker " + sign.id + " moved to " + loc.lat() + " " + loc.lng());
    var params = "id="+sign.id+"&lat="+loc.lat()+"&lng="+loc.lng();
    httpPost("http://"+server+":"+port+"/parking/main/signs", params);
}

function getSignDetails(sign) {
    selectedMarker = sign.marker;
    console.log("get details for " + sign.id);
    httpGetAsync("http://"+server+":"+port+"/parking/main/signs?id="+sign.id, displayDetails);
}

function displayDetails(data) {
    //    console.log("response from dbase is " + data);
//    var sign = JSON.parse(data);
//    var details = sign.pictureTag;                   
    var details = data;
    var infowindow = new google.maps.InfoWindow({
        content: details
    });

    infowindow.open(map, selectedMarker);
}

function processSigns(data) {
//    console.log("response from dbase is " + data);
    var signs = JSON.parse(data);    
    for (var i=0 ;i<signs.length ; i++) {
        addMarker(signs[i]);
    }
}

function addMarker(sign) {
        
    var pos = sign.position;
//  console.log("got sign " + sign.id + " @ " + pos.latitude + " " + pos.longitude);
    var signLatLng = new google.maps.LatLng(pos.latitude, pos.longitude);
    sign.marker = new google.maps.Marker({
                position: signLatLng,
                map: map,
                icon: 'images/no-parking16.png',
                draggable: true //make it draggable
    });

    
    google.maps.event.addListener(sign.marker, 'dragend', (function(sign){
            return function() {
                updateSignPosition(sign);
            }
    })(sign));

    google.maps.event.addListener(sign.marker, 'click', (function(sign){
            return function() {
                getSignDetails(sign);
            }
    })(sign));

    gsigns.push(sign);   
}

function httpPost(theUrl, params) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", theUrl, true);
    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xmlHttp.onreadystatechange = function() {//Call a function when the state changes.
        if(xmlHttp.readyState == 4 && xmlHttp.status == 200) {
            console.log("received response from POST = " + xmlHttp.responseText);
        }
    }
    xmlHttp.send(params);
}  

function httpGetAsync(theUrl, callback)
{
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() { 
//        console.log("httpGetAsync got xmlHttp readyState = " + xmlHttp.readyState + " status = " + xmlHttp.status);
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {            
            callback(xmlHttp.responseText);
        }
    }
    xmlHttp.open("GET", theUrl, true); // true for asynchronous 
    xmlHttp.send(null);
}

var gsigns = new Array();

google.maps.event.addDomListener(window, 'load', initMap);