//map.js

//Set up some of our variables.
var map; 
var marker = false;
var server = "parking.allowed.org";
var port = "8080";
var user = null;
var chris = "chris";
var carol = "carol";
var centerLat = null;
var centerLng = null;
var signNum = null;
var selectedId = null;
var bounds = null;

function initMap() {

    var lachine = new google.maps.LatLng(45.43371388888889, -73.69053611111111);
    var ndg = new google.maps.LatLng(45.472376, -73.615374);
    var westmount = new google.maps.LatLng(45.479080, -73.597492);
    port = document.getElementById('port').value;
    user = document.getElementById('user').value;
    if (document.getElementById('cenLat') != null) {
        centerLat = document.getElementById('cenLat').value;
    }
    if (document.getElementById('cenLng') != null) {
        centerLng = document.getElementById('cenLng').value;
    }   
    if (document.getElementById('signNum') != null) {
        signNum = document.getElementById('signNum').value;
    }

    if (centerLat == null || centerLng == null) {   
        centerOfMap = lachine;
        if (user == carol) {
            centerOfMap = ndg;
        }
        else if (user == chris)  {
            centerOfMap = westmount;
        }
    }
    else {
        centerOfMap = new google.maps.LatLng(centerLat, centerLng);
    }

    var options = {
      center: centerOfMap, //Set center.
      zoom: 17 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    var infoWindow = new google.maps.InfoWindow({map: map});

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            var pos = {
              lat: position.coords.latitude,
              lng: position.coords.longitude
            };

            infoWindow.setPosition(pos);
            infoWindow.setContent('Location found.');
            map.setCenter(pos);
        }, function() {
//            handleLocationError(true, infoWindow, map.getCenter());
        });
    } 

    if (signNum != null) {
        httpGetAsync("http://"+server+":"+port+"/parking/main/signs?signNum="+signNum, processSigns);
    }

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

//    google.maps.event.addListener(map, 'zoom_changed', function() {
//        refreshSigns();    
//    });

//    google.maps.event.addListener(map, 'center_changed', function() {
//        refreshSigns();
//    });

    google.maps.event.addListener(map, 'bounds_changed', function() {
        refreshSigns();
    });   
}

function refreshSigns() {    
    if (signNum == null) {
        bounds = map.getBounds();
 //       console.log("bounds = "+bounds.toString());
        var ne = bounds.getNorthEast();
 //       console.log("ne = "+ne.toString());
        var sw = bounds.getSouthWest();
 //       console.log("ne = "+sw.toString());
        var fetchUrl = "http://"+server+":"+port+"/parking/main/signs?nela="+ne.lat()+"&nelg="+ne.lng()+"&swla="+sw.lat()+"&swlg="+sw.lng();
 //       console.log("fetch signs using url "+fetchUrl);
        httpGetAsync(fetchUrl, processSigns);
    }
}

function markerLocation(){
    //Get location.
    var currentLocation = marker.getPosition();
    document.getElementById('lat').value = currentLocation.lat(); //latitude
    document.getElementById('lng').value = currentLocation.lng(); //longitude
}

function updateSignPosition(sign) {
    selectedMarker = sign.marker;
    selectedId = sign.id;
    var loc = sign.marker.getPosition();
    console.log("marker " + sign.id + " moved to " + loc.lat() + " " + loc.lng());
    var params = "id="+sign.id+"&lat="+loc.lat()+"&lng="+loc.lng()+"&user="+user;
    httpPost("http://"+server+":"+port+"/parking/main/signs", params);
}

function getSignDetails(sign) {
    selectedMarker = sign.marker;
    selectedId = sign.id;
    console.log("get details for " + sign.id);
    httpGetAsync("http://"+server+":"+port+"/parking/main/signs?id="+sign.id, displayDetails);
}

function displayDetails(data) {
    var infoContent=document.createElement('div'); 
    infoContent.innerHTML="<p>"+data+"<br>click this text to edit/delete this sign</p>"; 
    infoContent.onclick=editSign;                    
    var infowindow = new google.maps.InfoWindow({
        content: infoContent
    });

    infowindow.open(map, selectedMarker);
}

function editSign() {
    var editUrl = "http://"+server+":"+port+"/parking/main/edit";
    var returnUrl = "http://"+server+":"+port+"/parking/main";
    console.log("edit url "+editUrl+" ret url "+returnUrl);
    console.log("marker id = "+selectedId);
    var faction = "<form action=\"" + editUrl + "\" method=\"POST\">";
    var fedit = "<input type=\"radio\" name=\"action\" value=\"edit\" checked>Edit Sign<br>";
    var fdelete = "<input type=\"radio\" name=\"action\" value=\"delete\">Delete Sign<br>";
    var fid = "<input type=\"hidden\" name=\"id\" value=\""+selectedId+"\">";
    var furl = "<input type=\"hidden\" name=\"returnUrl\" value=\""+returnUrl+"\">";
    var fsub = "<input type=\"submit\" value=\"Submit\"></form>";
    document.getElementById('editForm').innerHTML =  faction + fedit + fdelete + fid + furl + fsub;
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
                icon: "http://"+server+":"+port+"/parking/images/no-parking16.png",
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

function handleLocationError(browserHasGeolocation, infoWindow, pos) {
    infoWindow.setPosition(pos);
    infoWindow.setContent(browserHasGeolocation ?
                              'Error: The Geolocation service failed.' :
                              'Error: Your browser doesn\'t support geolocation.');
}

var gsigns = new Array();

google.maps.event.addDomListener(window, 'load', initMap);