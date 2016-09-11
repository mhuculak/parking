package parking.map;

public interface GeocoderInterface {
	
	public Position geocode(String placeString, Position orig, double maxDistKm);
	public Place findPlace(String placeString, Position orig, double maxDist);
}