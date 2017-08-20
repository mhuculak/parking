//map.js

//Set up some of our variables.
var map; 
var marker = false;
var server = "parking.allowed.org";
var port = "8080";
var user = null;
var centerLat = null;
var centerLng = null;
var selectedId = null;
var bounds = null;
var selectSegmentMode = "SelectSegment";
var viewSegmentMode = "ViewSegment";
var addCornerMode = "AddCorner";
var addPointMode = "AddPoint";
var findWork = "FindWork";
var publish = "Publish";
var editMode = selectSegmentMode;
var selectedMarker = null;
var selectedSegmentId = null;
var selectedSegment = null;
var gSigns = new Array();
var gLines = new Array();
var gSegments = new Array();
var count = 0;

function initMap() {

    port = document.getElementById('port').value;
    user = document.getElementById('user').value;

    if (document.getElementById('editMode') != null) {
        editMode = document.getElementById('editMode').value;
        console.log("Edit mode is "+editMode);
    }
    else {
        editMode = selectSegmentMode;
    }
    if (document.getElementById('selectedSegmentId') != null) {
        selectedSegmentId = document.getElementById('selectedSegmentId').value;
    }

    if (document.getElementById('cenLat') != null) {
        centerLat = document.getElementById('cenLat').value;
    }
    if (document.getElementById('cenLng') != null) {
        centerLng = document.getElementById('cenLng').value;

    }       

    if (centerLat == null || centerLng == null) {   
        centerOfMap = new google.maps.LatLng(45.4337, -73.6905);
    }
    else {
        centerOfMap = new google.maps.LatLng(centerLat, centerLng);
        console.log("center is "+centerLat+" "+centerLng);
    }

    var options = {
      center: centerOfMap, //Set center.
      zoom: 17 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    var infoWindow = new google.maps.InfoWindow({map: map});

    google.maps.event.addListener(map, 'click', function(event) {        
        var clickedLocation = event.latLng;
        console.log("click listener for "+editMode+" location = "+clickedLocation);
        if (editMode == selectSegmentMode) {
            console.log("select segment...");
            selectSegment(clickedLocation);
        }
        else if (editMode == findWork) {
            console.log("find work...");
            // nothing to do here
        }
        else if (editMode == publish) {
            console.log("publish...");
            // nothing to do here
        }
        else if (editMode == viewSegmentMode) {
            console.log("view...");
            // nothing to do here
        }
        else if (editMode == addPointMode) {
            console.log("insert segment position...");
            insertSegmentPosition(clickedLocation, editMode);
        }
        else if (editMode == addCornerMode) {
            console.log("add segment corner...");
            addSegmentCorner(clickedLocation, editMode);
        }
        else {
            console.log("ERROR: unsupported edit mode "+editMode);
        }        
    });

    google.maps.event.addListener(map, 'bounds_changed', function() {
        refreshSegments();
    });   
}

function setSelectMode() {
    console.log("select a segment on the map");
    editMode = selectSegmentMode;
    if (selectedSegment != null) {
        cleanupSegment(selectedSegment);
        selectedSegment = null;
    }
    selectedSegmentId = null;
    document.getElementById('statusMessage').innerHTML = "Click near a sign to select a street segment.";   
}

function setViewMode() {
    editMode = viewSegmentMode;
    document.getElementById('statusMessage').innerHTML = "pan and zoom without modifying the segments.";
}

function setAddPointMode() {
    console.log("add a point to selected segment");
    editMode = addPointMode;
    map.setOptions({ draggableCursor: 'crosshair' });
    document.getElementById('statusMessage').innerHTML = "Click a point in the middle of the street to define the selected segment.";   
}

function setInsertPointMode() {
    console.log("insert a point to selected segment");
    editMode = insertPointMode;
    map.setOptions({ draggableCursor: 'crosshair' });
    document.getElementById('statusMessage').innerHTML = "Insert a point in the selected segment.";   
}

function setAddCornerMode() {
    console.log("add a corner to selected segment");
    editMode = addCornerMode;
    map.setOptions({ draggableCursor: 'crosshair' });
    document.getElementById('statusMessage').innerHTML = "Click a street corner within the selected segment."; 
}

function saveSegment() {
    if (selectedSegment != null) {
        var params = "id="+selectedSegmentId+"&action=SaveSegment&user="+user;
        if (selectedSegment.points != null && selectedSegment.points.length > 0) {
            pString = posAsSting(selectedSegment.points[0]);
            for ( var i=1 ; i<selectedSegment.points.length ; i++) {
                pString += ":"+posAsSting(selectedSegment.points[i]);
            }
             params += "&points="+pString;
        }        
        if (selectSegment.corners != null && selectSegment.corners.length > 0) {
            cString = posAsSting(selectedSegment.corners[0]);
            for ( var i=0 ; i<selectedSegment.corners.length ; i++) {
                cString +=  ":"+posAsSting(selectedSegment.corners[i]);
            }
            params += "&corners="+cString;
        }       
        httpPostAsync("http://"+server+":"+port+"/parking/map-edit/segments", params, reloadSegment);
    }    
}

function undoUnsavedEdits() {
    console.log("reloading segment "+selectedSegmentId+" from database ");
    reloadSegment();
}

function clearSegment() {
   var params = "id="+selectedSegmentId+"&action=ClearSegment&user="+user; 
   httpPostAsync("http://"+server+":"+port+"/parking/map-edit/segments", params, reloadSegment);
}

function reloadSegment() {
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/segments?action=select&id="+selectedSegmentId;
    httpGetAsync(fetchUrl, getSegment);
}

function posAsSting(pos) {
    return pos.latitude+"_"+pos.longitude;
}

function refreshSegments() {    
    bounds = map.getBounds();
    var ne = bounds.getNorthEast();
    var sw = bounds.getSouthWest();
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/segments?action=find&nela="+ne.lat()+"&nelg="+ne.lng()+"&swla="+sw.lat()+"&swlg="+sw.lng();
    httpGetAsync(fetchUrl, processSegments);
}

function processSegments(data) {
    if (data == null) {
        console.log("no segments available");
    }
    for (var i=0 ; i<gSegments.length ; i++) {
        if (gSegments[i] != selectedSegment) {
            cleanupSegment(gSegments[i]);
        }
    }
    gSegments = new Array();
    var segments = JSON.parse(data);
    for (var i=0 ;i<segments.length ; i++) {
        var segment = segments[i];
        if (selectedSegment != null && selectedSegmentId.localeCompare(segment.id) == 0) {
//            console.log("Keeping selected segment");
            gSegments.push(selectedSegment);
        }
        else {
            gSegments.push(segment);
        }
        displaySegment(segment, false)        
    }        
}

function selectSegment(loc) {
    if (selectedSegment != null) {
        console.log("cleanup previous selection "+selectedSegmentId)
        cleanupSegment(selectedSegment);
    }
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/segments?action=select&sela="+loc.lat()+"&selg="+loc.lng();
    console.log("select segment "+fetchUrl);
    httpGetAsync(fetchUrl, getSegment);
}

function getSegment(data) {
//    console.log("server responded with "+data);
    var segment = JSON.parse(data);
    segment.saved = true;
    selectedSegment = segment;   
    selectedSegmentId = segment.id;    
    document.getElementById('selectedSegmentId').value = selectedSegmentId;    
    console.log("selected segment id is "+selectedSegmentId);
    for ( var i=0 ; i<gSegments.length ; i++) {
        var seg = gSegments[i];
        if (seg.id == selectedSegmentId) {
            cleanupSegment(seg);
            gSegments[i] = segment;         
            displaySegment(segment, true);
        }
        else {
            displaySegment(seg, false);
        }
    }     
}

function displaySegment(segment, selected) {
//    console.log("display "+segment);
    var weight = 1;
    var signUrl = "http://"+server+":"+port+"/parking/images/no-parking10.png";
    if (selected) {
        weight = 3;
        signUrl = "http://"+server+":"+port+"/parking/images/no-parking16.png";
        document.getElementById('topForm').innerHTML = selectedSegmentId+" street is "+segment.name; 
    }
    if (segment.path != null) {
       segment.path.setMap(null); 
    }
    if (segment.points != null && segment.points.length > 1) {
        segment.path = addPath( segment.points, "#0000FF", weight);
        if (selected) {
            for ( var j=0 ; j<segment.points.length ; j++ ) {
                addSegmentMarker(segment.id, segment.points[j], "MovePoint");
            }
        }
        else {
            for ( var j=0 ; j<segment.points.length ; j++ ) {
                if (segment.points[j].marker != null) {
                    segment.points[j].marker.setMap(null);
                }
            }
        }
    }
    if (segment.signs != null && segment.save == null) { 
        for ( var j=0 ; j<segment.signs.length ; j++) {
                addSignMarker(segment.signs[j], signUrl);
        }
    }
    if (segment.corners != null && selected) {
        var corners = segment.corners;
        for ( var j=0 ; j<segment.corners.length ; j++ ) {
            addSegmentMarker(segment.id, segment.corners[j], "MoveCorner");
        } 
    }    
}

function addPath( positions, color, weight) {
    if (positions == null || positions.length < 2) {
        return null;
    }
    var pathArray = new Array();
    for ( var j=0 ; j<positions.length ; j++ ) {
        var point = positions[j];
        var pos = null;
        if (point.latitude != null) {
            pos = new google.maps.LatLng( point.latitude, point.longitude );
        }
        else {
            pos = new google.maps.LatLng( point.position.latitude, point.position.longitude );
        }
        pathArray.push(pos);   
    }
    var polyLine = new google.maps.Polyline({
        path: pathArray,              
        strokeColor: color,
        strokeOpacity:0.6,
        strokeWeight: weight
    });
//    console.log("display line with "+pathArray);
    polyLine.setMap(map);
    gLines.push(polyLine);
    return polyLine;
}

function addSegmentCorner(loc, action) {
    var p = new Object( { latitude: loc.lat(), longitude: loc.lng()});    
    addSegmentMarker(selectedSegmentId, p, action);    
    if (selectSegment.corners == null) {
        selectSegment.corners = new Array();
    }
    selectSegment.corners.push(p);          
    displaySegment(selectedSegment, true);
    saveSegment();     
}

function cleanupSegment(segment) {
    if (segment.path != null) {
       segment.path.setMap(null); 
    }
    if (segment.points != null) {                       
        for ( var j=0 ; j<segment.points.length ; j++ ) {            
            if (segment.points[j].marker != null) {
                segment.points[j].marker.setMap(null);
            }
        }
    }
    if (segment.corners != null) {                       
        for ( var j=0 ; j<segment.corners.length ; j++ ) {            
            if (segment.corners[j].marker != null) {
                segment.corners[j].marker.setMap(null);
            }
        }
    }
    if (segment.signs != null) { 
        for ( var j=0 ; j<segment.signs.length ; j++) {
            if (segment.signs[j].marker != null) {
                segment.signs[j].marker.setMap(null);
            }
        }
    }
}

function addSegmentMarker(id, p, action) {
    if (p.marker != null) {
        p.marker.setMap(null);
    }
    var latLng = new google.maps.LatLng(p.latitude, p.longitude);
    var color = null;
    if (action == addPointMode) {
        color = "blue";
    }
    else if (action == addCornerMode) {
        color = "green";
    }
    var marker = new google.maps.Marker({
                position: latLng,
                map: map,
                strokeColor: color,
                draggable: true //make it draggable
    });

    p.marker = marker;
    p.id = id;
    p.action = action;

     google.maps.event.addListener(p.marker, 'dragend', (function(p){
            return function() {
                console.log("moving point "+p);
                updateMarkerPosition(p);
            }
    })(p)); 
}

function updateMarkerPosition(p) {
    var loc = p.marker.getPosition();
    console.log("marker " + p.id + " moved to " + loc.lat() + " " + loc.lng());
    var params = "id="+p.id+"&action="+p.action+"&olat="+p.latitude+"&olng="+p.longitude+"&lat="+loc.lat()+"&lng="+loc.lng()+"&user="+user;
    console.log("sending updated position "+params);
    httpPostAsync("http://"+server+":"+port+"/parking/map-edit/segments", params, reloadSegment);
    p.latitude = loc.lat();
    p.longitude = loc.lng();
}


function addSignMarker(sign, iconValue) {
    if (sign.marker != null) {
        sign.marker.setMap(null);
    }    
    var pos = sign.position;
//  console.log("got sign " + sign.id + " @ " + pos.latitude + " " + pos.longitude);
    var signLatLng = new google.maps.LatLng(pos.latitude, pos.longitude);
    sign.marker = new google.maps.Marker({
                position: signLatLng,
                map: map,
                icon: iconValue,
                draggable: true //make it draggable
    });

    google.maps.event.addListener(sign.marker, 'click', (function(sign){
            return function() {
                getSignDetails(sign);
            }
    })(sign));

    gSigns.push(sign);   
}

function updateSignPosition(sign) {
    selectedMarker = sign.marker;
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
    var thumbUrl = "http://"+server+":"+port+"/parking/main/signs?id="+sign.id+"&action=thumbnail&size=200";
    console.log("fetch "+thumbUrl);
    httpGetFileAsync(thumbUrl, displayThumb);
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

function displayThumb(data) {
    var urlCreator = window.URL || window.webkitURL;
    var imageUrl = urlCreator.createObjectURL(this.response); // response ?
    document.querySelector("#image").src = imageUrl;
}

function editSign() {
    var editUrl = "http://"+server+":"+port+"/parking/main/edit";
    var returnUrl = "http://"+server+":"+port+"/parking/main";
    console.log("edit url "+editUrl+" ret url "+returnUrl);
    console.log("marker id = "+selectedId);
    var faction = "<form action=\"" + editUrl + "\" method=\"POST\" target=\"_blank\">";
    var fedit = "<input type=\"radio\" name=\"action\" value=\"edit\" checked>Edit Sign<br>";
    var fdelete = "<input type=\"radio\" name=\"action\" value=\"delete\">Delete Sign<br>";
    var fid = "<input type=\"hidden\" name=\"id\" value=\""+selectedId+"\">";
    var furl = "<input type=\"hidden\" name=\"returnUrl\" value=\""+returnUrl+"\">";
    var fsub = "<input type=\"submit\" value=\"Submit\"></form>";
    document.getElementById('editForm').innerHTML =  faction + fedit + fdelete + fid + furl + fsub;
}

function insertSegmentPosition(loc, action) {
    var p = new Object( { latitude: loc.lat(), longitude: loc.lng()});
    addSegmentMarker(selectedSegmentId, p, action);
    var points = selectedSegment.points;
    if (points == null) {
        points = new Array();
        points.push(p);
        selectedSegment.points = points;
        return;
    }
    if (points.length == 1) {
        points.push(p);
        selectedSegment.path = addPath( selectedSegment.points, "#0000FF");
        saveSegment();
        return;
    }
    console.log("insert point is "+p);
    var dist = new Array();
    var min1 = -1;

    var index1 = 0;
    for ( var i=0 ; i<points.length ; i++ ) {
        var d = getDist(p, points[i]);
        console.log("point "+i+" is "+points[i]+" dist = "+d);
        dist.push(d);
        if (min1 < 0 || d < min1) {
            min1 = d;
            index1 = i;
        }
    }
    console.log("closest index is "+index1);
    var min2 = -1;
    for ( var i=0 ; i<dist.length ; i++ ) {
        if (dist[i] != min1) {
            if ( i != index1 ) {
                if ( min2 < 0 || dist[i] < min2 ) {
                    index2 = i;
                    min2 = dist[i];
                }
            }
        }
    }
    console.log("2nd closest index is "+index2);
    if (index1 > index2) {
        var temp = index1;
        index1 = index2;
        index2 = temp;
        temp = min1;
        min1 = min2;
        min2 = temp;
    }
    if (index2-index1 != 1) {
        console.log("ERROR: cannot insert into incorrectly ordered list "+points);
        document.getElementById('statusMessage').innerHTML = "points are incorrectly ordered, clear the segment and restart";
        return;
    }
    var d12 = getDist(points[index1], points[index2]);
    if (min1 > d12 || min2 > d12) {
        if (points.length == 2) {
            if (min1 < min2) {
                index1 = -1;
                index2 = 0;                
            }
            else {
                index1 = 1;
                index2 = 2;
            }
        }
        else if (index1 == 0) {
            index1 = -1;  // add new point at the start of list
            index2 = 0;
        }
        else if (index2 == points.length-1) {
            index1 = index2; // add new point at end of list
            index2 = points.length;
        }
    }
    var newPoints = new Array();
    for ( var i=0 ; i<=index1 ; i++) {
        console.log("add point "+i);
        newPoints.push(points[i]);
    }
    console.log("insert ");
    newPoints.push(p);
    for ( var i=index2 ; i<points.length ; i++) {
        console.log("add point "+i);
        newPoints.push(points[i]);
    }
    selectedSegment.points = newPoints;
    if (selectedSegment.path != null) {
        console.log("remove old path");
        selectedSegment.path.setMap(null); 
    }
    else {
        console.log("no old path to remove");
    }
    selectedSegment.path = addPath( selectedSegment.points, "#0000FF");
    saveSegment();
}

function getDist(p1, p2) {
    var radiusOfEarthKm = 6371;
    var dlat = Math.abs( p1.latitude - p2.latitude);
    var dlng = Math.abs( p1.longitude - p2.longitude);
    var angleDegrees = Math.sqrt(dlat*dlat + dlng*dlng);
    var angleRadians = angleDegrees*Math.PI/180;
    return angleRadians * radiusOfEarthKm;
}

function httpPost(theUrl, params) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", theUrl, true);
    xmlHttp.withCredentials = true;
    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xmlHttp.onreadystatechange = function() {//Call a function when the state changes.
        if(xmlHttp.readyState == 4 && xmlHttp.status == 200) {
           console.log("received response from POST = " + xmlHttp.responseText);
        }
    }
    xmlHttp.send(params);
} 

function httpPostAsync(theUrl, params, callback) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", theUrl, true);
    xmlHttp.withCredentials = true;
    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xmlHttp.onreadystatechange = function() {//Call a function when the state changes.
        if(xmlHttp.readyState == 4 && xmlHttp.status == 200) {
           callback(xmlHttp.responseText); 
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
    xmlHttp.withCredentials = true;
    xmlHttp.send(null);
}

function httpGetFileAsync(theUrl, callback)
{
    var xmlHttp = new XMLHttpRequest();
    
    xmlHttp.open("GET", theUrl, true); // true for asynchronous 
    xmlHttp.withCredentials = true;
    xmlHttp.responseType = "blob";
    xmlHttp.onload = callback;
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