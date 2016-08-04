package parking.map;

import parking.util.Exif;
import parking.schedule.ParkingSchedule;

import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import java.io.File;

public class Sign {
	
	private Position position;
//	private Direction direction;
	private String pictureTag;
	private ParkingSchedule autoSchedule;  // schedule generated by image processing
	private ParkingSchedule schedule;      // verified schedule
	private Address address;

	public Sign(File signPicture, Position position) {
		this.position = position;
		processSignImage(signPicture);
		address = reverseGeocode(position);
		if (address != null) {
			String shortAdd = address.getShortAddress();
			String prefix = shortAdd.replace(" ", "_");
			pictureTag = prefix + "_" + signPicture.getName();
		}
		else {
			System.out.println("ERROR: cannot get address from " + position.toString());
		}
	}
/*
	public Sign(File signPicture, Direction direction) {
		this.direction = direction;
		position = processSignImage(signPicture);
		if (position != null) {
			this.position = position;			
			address = reverseGeocode(position);
			if (address != null) {
				String shortAdd = address.getShortAddress();
				String prefix = shortAdd.replace(" ", "_");
				pictureTag = prefix + "_" + signPicture.getName();
			}
			else {
				System.out.println("ERROR: cannot get address from " + position.toString());
			}
		}		
		else {
			System.out.println("ERROR: cannot get position from " + signPicture.getName());
		}
	}
*/
	public Sign(Position p, String tag, Address a) {
		position = p;
		pictureTag = tag;
		address = a;
	}
/*
	public void setDirection(Direction d) {
		direction = d;
	}
*/
	public void setParkingSchedule(ParkingSchedule sched) {
		schedule = sched;
	}

	public void setAutoSchedule(ParkingSchedule schedule) {
		autoSchedule = schedule;
	}

	public Position getPosition() {
		return position;
	}

	public Address getAddress() {
		return address;
	}

	public String getPictureTag() {
		return pictureTag;
	}
/*
	public Direction getDirection() {
		return direction;
	}
*/
	public ParkingSchedule getParkingSchedule() {
		return schedule;
	}

	public ParkingSchedule getAutoSchedule() {
		return autoSchedule;
	}

	private  Position processSignImage(File signPicture) {
		return Exif.getPosition(signPicture);
		//
		// TODO: add algorithm to read parking schedule from image
		//
	}

	public static Address reverseGeocode(Position p) {
		try {	
			GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyDni-ZQemF7eA1P-A76acHMF2tREyFM3HI");
			LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
			GeocodingResult[] results = GeocodingApi.newRequest(context).latlng(latLng).await();
//			System.out.println(results[0].formattedAddress);
			return new Address(results[0].addressComponents);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


}