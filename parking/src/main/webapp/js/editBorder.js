var map; 
var server = "parking.allowed.org";
var port = "8080";
var user = null;
var centerLat = null;
var centerLng = null;
var selectedBorder = new Object();
var selectBorderMode = "SelectBorder";
var addPointMode = "AddPoint";
var insertPointMode = "InsertPoint";
var viewMode = "View";
var editMode = addPointMode;
var gBorders = new Array();
var wantAddress = false;
var undoStack = new Array();

function initMap() {

    port = document.getElementById('port').value;
    user = document.getElementById('user').value;


    if (document.getElementById('cenLat') != null) {
        centerLat = document.getElementById('cenLat').value;
    }
    if (document.getElementById('cenLng') != null) {
        centerLng = document.getElementById('cenLng').value;

    }       

    if (centerLat == null || centerLng == null) {   
        centerOfMap = new google.maps.LatLng(45.4337, -73.6905);
        console.log("use default center "+centerOfMap);
    }
    else {
        centerOfMap = new google.maps.LatLng(centerLat, centerLng);
        console.log("center is "+centerLat+" "+centerLng);
    }

    var options = {
      center: centerOfMap, //Set center.
      zoom: 13 //The zoom value.
    };

    map = new google.maps.Map(document.getElementById('map'), options);
    var infoWindow = new google.maps.InfoWindow({map: map});

    console.log("map added waiting for input...");

    google.maps.event.addListener(map, 'click', function(event) {        
        var clickedLocation = event.latLng;
        if (editMode == addPointMode) {        
            addBorderPoint(clickedLocation);
        }
        else if (editMode == insertPointMode) {        
            insertBorderPoint(clickedLocation);
        }
        else if (editMode == selectBorderMode) {
            selectBorder(clickedLocation);
        }    
    });

     document.getElementById('statusMessage').innerHTML = "Enter a region name before adding points";  

    google.maps.event.addListener(map, 'bounds_changed', function() {
        refreshBorders();
    });   
}

function setSelectMode() {
    console.log("select a border on the map");
    editMode = selectBorderMode;
    if (selectedBorder != null) {
        cleanupBorder(selectedBorder);
        selectedBorder = null;
    }
    document.getElementById('statusMessage').innerHTML = "Click map to select a border.";   
}

function setViewMode() {
    editMode = viewMode;
    document.getElementById('statusMessage').innerHTML = "pan and zoom without modifying the borders.";
}

function setAddPointMode() {
    console.log("add a point to selected border");
    editMode = addPointMode;
    map.setOptions({ draggableCursor: 'crosshair' });
    document.getElementById('statusMessage').innerHTML = "Click the map to define the regions border.";   
}

function setInsertPointMode() {
    editMode = insertPointMode;
    map.setOptions({ draggableCursor: 'crosshair' });
    document.getElementById('statusMessage').innerHTML = "Click near the border to insert points.";   
}

function getAddress() {
    wantAddress = true;
    saveBorder();
}

function undo() {
    selectedBorder = undoStack.pop();
    saveBorder();
}

function saveBorder() {
    if (selectedBorder != null) {
        var region = document.getElementById('RegionName').value;
        if (region == null || region.length == 0) {
            document.getElementById('statusMessage').innerHTML = "enter region name before saving";
            console.log("ERROR: no region name");
            return;
        }
        var params = null;
        if (selectedBorder.id != null) {
            params = "id="+selectedBorder.id+"&action=SaveBorder&region="+region;
        }
        else {
            params = "action=SaveBorder&region="+region;
        }
        if (selectedBorder.points != null && selectedBorder.points.length > 0) {
            pString = posAsString(selectedBorder.points[0]);
            for ( var i=1 ; i<selectedBorder.points.length ; i++) {
                pString += ":"+posAsString(selectedBorder.points[i]);
            }
            params += "&points="+pString;
        }
        console.log("params = "+params);                   
        httpPostAsync("http://"+server+":"+port+"/parking/map-edit/borders", params, reloadBorder);
    }    
}

function posAsString(pos) {
    return pos.latitude+"_"+pos.longitude;
}

function refreshBorders() {    
    bounds = map.getBounds();
    var ne = bounds.getNorthEast();
    var sw = bounds.getSouthWest();
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/borders?action=find&nela="+ne.lat()+"&nelg="+ne.lng()+"&swla="+sw.lat()+"&swlg="+sw.lng();
    httpGetAsync(fetchUrl, processBorders);
}

function reloadBorder(resp) {
    selectedBorder.id = resp;
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/borders?action=select&id="+selectedBorder.id;
    httpGetAsync(fetchUrl, getBorder);
}

function processBorders(data) {
    if (data == null) {
        console.log("no borders available");
    }
    for (var i=0 ; i<gBorders.length ; i++) {
        if (gBorders[i] != selectedBorder) {
            cleanupBorder(gBorders[i]);
        }
    }
    gBorders = new Array();
    var borders = JSON.parse(data);
    for (var i=0 ;i<borders.length ; i++) {
        var border = borders[i];
        if (selectedBorder != null && selectedBorder.id != null && selectedBorder.id.localeCompare(border.id) == 0) {
            console.log("Keeping selected border");
            gBorders.push(selectedBorder);
        }
        else {
            gBorders.push(border);
        }
        displayBorder(border, false)        
    }        
}

function selectBorder(loc) {
    if (selectedBorder != null) {
        console.log("cleanup previous selection "+selectedBorder.id)
        cleanupBorder(selectedBorder);
    }
    var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/borders?action=select&sela="+loc.lat()+"&selg="+loc.lng();
    console.log("select border "+fetchUrl);
    httpGetAsync(fetchUrl, getBorder);
}

function displayBorder(border, selected) {
    var weight = 1;
    if (selected) {
        weight = 3;        
    }
    if (border.path != null) {
       border.path.setMap(null); 
    }
    if (border.points != null && border.points.length > 1) {
        border.path = addPath( border.points, "#0000FF", weight);
        if (selected) {
            for ( var j=0 ; j<border.points.length ; j++ ) {
                addMarker(border.id, border.points[j], "MovePoint");
            }
        }
        else {
            for ( var j=0 ; j<border.points.length ; j++ ) {
                if (border.points[j].marker != null) {
                    border.points[j].marker.setMap(null);
                }
            }
        }
    }
}

function cleanupBorder(border) {
    if (border.path != null) {
       border.path.setMap(null); 
    }
    if (border.points != null) {                       
        for ( var j=0 ; j<border.points.length ; j++ ) {            
            if (border.points[j].marker != null) {
                border.points[j].marker.setMap(null);
            }
        }
    }
}

function getBorder(data) {
//    console.log("server responded with "+data);
    var border = JSON.parse(data);
    selectedBorder = border;    
    document.getElementById('BorderId').value = selectedBorder.id;    
    console.log("selected border id is "+selectedBorder.id);
    for ( var i=0 ; i<gBorders.length ; i++) {
        var b = gBorders[i];
        if (b.id == selectedBorder.id) {
            cleanupBorder(b);
            gBorders[i] = border;         
            displayBorder(border, true);
        }
        else {
            displayBorder(b, false);
        }
    }  
    if (wantAddress) {
        var center = selectedBorder.center;
        console.log("get address for border center "+center.latitude+" "+center.longitude);
        var fetchUrl = "http://"+server+":"+port+"/parking/map-edit/borders?action=address&sela="+center.latitude+"&selg="+center.longitude;        
        httpGetAsync(fetchUrl, displayAddress);
    }   
}

function displayAddress(address) {
    document.getElementById('statusMessage').innerHTML = address;  
}

function addMarker(id, p) {
    if (p.marker != null) {
        p.marker.setMap(null);
    }
    var latLng = new google.maps.LatLng(p.latitude, p.longitude);
    var marker = new google.maps.Marker({
                position: latLng,
                map: map,
                draggable: true //make it draggable
    });

    p.marker = marker;
    p.id = id;

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
    if (p.id != null) {
        var params = "id="+p.id+"&action=MovePoint&olat="+p.latitude+"&olng="+p.longitude+"&lat="+loc.lat()+"&lng="+loc.lng()+"&user="+user;
        console.log("sending updated position "+params);
        httpPostAsync("http://"+server+":"+port+"/parking/map-edit/borders", params, reloadBorder);
    }
    p.latitude = loc.lat();
    p.longitude = loc.lng();
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
    return polyLine;
}

function addBorderPoint(loc) {
    var p = new Object( { latitude: loc.lat(), longitude: loc.lng()});
    addMarker(selectedBorder.id, p);
    if (selectedBorder.points == null) {
        selectedBorder.points = new Array();
    }
    selectedBorder.points.push(p);
    if (selectedBorder.path != null) {
        selectedBorder.path.setMap(null); 
    }
    selectedBorder.path = addPath( selectedBorder.points, "#0000FF");
    saveBorder();
}

function insertBorderPoint(loc) {
    var p = new Object( { latitude: loc.lat(), longitude: loc.lng()});
    addMarker(selectedBorder.id, p);
    var points = selectedBorder.points;
    if (points == null) {
        points = new Array();
        points.push(p);
        selectedBorder.points = points;
        return;
    }
    else {
        var prev = new Array();
        for ( var i=0 ; i<points.length ; i++) {
            prev.push(points[i]);
        }
        undoStack.push(prev);
    }
    if (points.length == 1) {
        points.push(p);
        selectedBorder.path = addPath( selectedBorder.points, "#0000FF");
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
/*    
    if (index2-index1 != 1) {
        console.log("ERROR: cannot insert into incorrectly ordered list "+points);
        document.getElementById('statusMessage').innerHTML = "points are incorrectly ordered, clear the border and restart";
        return;
    }
*/    
    var d12 = getDist(points[index1], points[index2]);
    console.log("dist between "+index1+" and "+index2+" is "+d12);
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
    selectedBorder.points = newPoints;
    if (selectedBorder.path != null) {
        console.log("remove old path");
        selectedBorder.path.setMap(null); 
    }
    else {
        console.log("no old path to remove");
    }
    selectedBorder.path = addPath( selectedBorder.points, "#0000FF");
    saveBorder();
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

google.maps.event.addDomListener(window, 'load', initMap);