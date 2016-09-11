package parking.map;

//
// Initialize stuff that is different on android and regular java
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