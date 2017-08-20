package parking.map;

//
// Initialize stuff that is different on android and regular java
//
// can we do this using a bridge?
//
// SOLN: have Position and Address extend a base class that contains the Geocoder and ReverseGecoder
//       (this code should no longer be required in this case)
//
public class MapInit {
	static {
		ReverseGeocoder rc = new ReverseGeocoder();
		Address.setReverseGeocoder(rc);
		Place.setReverseGeocoder(rc);
		
		MyGeocoder gc = new MyGeocoder();
		Position.setGeocoder(gc);
		Place.setGeocoder(gc);

	}
}