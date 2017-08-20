package parking.display;

import parking.map.Position;

public class TestGoogle {

	private static double R0;
	private static Position p0;
	private static int baseDim;

	public TestGoogle() {
		p0 = new Position(0,0);
		R0 = Position.getEarthRadiusMeters(p0);
		System.out.println("R0 = "+R0);
		baseDim = 512;	
	}

	private static CellSize getCellSize(Position p, int level) {
		double lngRes = getResolution(p0, level);
		double latRes = getResolution(p, level);		
		return new CellSize(pixelsToDegrees(baseDim, R0, latRes), pixelsToDegrees(baseDim, R0, lngRes));
	}
	//
	//
	//
	//     cell size (deg) = b * k * cos(lat) / 2^level
	//
	//     where:
	//            b = cell size in pixels
	//
	//
	//     lat_i+1 = b * k * cos(lat_i) + lat_i
	//
	private void testLevel(int level, double max) {
		double lat = 0;
		int indx = 0;
		System.out.println("Level "+level);
		while (lat < max) {							
			Position p = new Position(lat, 0);
			double latRes = getResolution(p, level);
			double latSize = pixelsToDegrees(baseDim, R0, latRes);
			System.out.println(indx+" lat "+lat+" "+latSize);
			lat += latSize;
			indx++;
		}
	}
	//
	//      meters/pixel =  k * R0 * cos( latitude )    
	//						----------------------- 
	//                            2^level              
	//  R = 6378137 k = 0.024543
	//
	public static double getResolution(Position p, int level) { 
		return 156543.03392 * Math.cos(p.getLatitude() * Math.PI / 180) / Math.pow(2, level);
	}

	private static double pixelsToDegrees(int lenPixels, double radiusOfEarthMeters, double metersPerPixel) {
		double lenMeters = lenPixels * metersPerPixel;	
		double angleRadians = lenMeters / radiusOfEarthMeters;				
		return angleRadians * 180 / Math.PI;
	}
/*	
	public static void main( String args[]) {
		TestGoogle test = new TestGoogle();
		for ( int z=1 ; z<5 ; z++) {
			test.testLevel(z, 90.0);
		}
	}
*/	
}