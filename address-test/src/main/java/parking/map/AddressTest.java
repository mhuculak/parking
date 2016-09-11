package parking.map;

import java.io.File;

public class AddressTest {

	public static void main(String[] args) {
		Position p = null;
		MapInit mapInit = new MapInit();
/*		
		System.out.println("found "+args.length+" args");
		for ( int i=0 ; i<args.length ; i++) {
			System.out.println(args[i]);
		}
*/		
		if (args.length == 1) {
			String[] stuff = args[0].split(",");
			p = new Position(stuff[0], stuff[1]);
		}
		else if (args.length == 2) {
			p = new Position(args[0].replaceAll(",",""), args[1]);
		}	
		else {
			System.out.println("Usage: lat,lng");
		}
		if (p != null) {
			Address a = Address.reverseGeocode(p);
			System.out.println(a.fullString());
		}
	}
	
}