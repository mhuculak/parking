package parking.database;

import parking.map.Country;
import parking.map.MapBorder;
import parking.map.MapBounds;
import parking.map.StreetSegment;
import parking.map.Address;
import parking.map.Position;

import parking.util.Logger;
import parking.util.LoggingTag;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

import java.util.List;
import java.util.ArrayList;

public class MapDB {

	private MongoInterface m_mongo;
	private DBCollection m_map;
	private Logger m_logger;

	public MapDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_map = m_mongo.getDB().getCollection("map");
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.MapDB);
	}

	public MapEntity addCountry(Country country) {
		BasicDBObject searchQuery = new BasicDBObject("country", country.toString());
		DBCursor cursor = m_map.find(searchQuery);
		MapEntityType type = MapEntityType.Country;
		if (cursor.count()==0) {
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("country", country.toString());
			document.append("type", type);
			MapEntity newEntity = new MapEntity(m_mongo, id.toString(), country.toString(), type);
			m_map.insert(document);
			return newEntity;
		}
		else {
			m_logger.log("Country "+country.toString()+" already exists");
		}
		return null;
	}

	public MapEntity getCountry(Country country) {
		BasicDBObject searchQuery = new BasicDBObject("country", country.toString());
		DBCursor cursor = m_map.find(searchQuery);
		MapEntityType type = MapEntityType.Country;
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			MapEntity entity = MapEntity.getEntity(m_mongo, document);
			return entity;
		}
		else if (cursor.count()==0) {
			return addCountry(country);
		}
		else {
			m_logger.error("Found multiple entries for country "+country.toString());
		}
		return null;
	}

	public List<MapEntity> getCountryMatches(Position p, MapBounds bounds) {
		DBCursor cursor = m_map.find();
		List<MapEntity> matches = new ArrayList<MapEntity>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	BasicDBList borderObj = (BasicDBList)document.get("border");
	    	if (borderObj != null) {
	    		MapBorder childBorder = MapBorder.getBorder(borderObj);
	    		if (MapBorder.intersects(bounds, childBorder)) {
	    			matches.add(MapEntity.getEntity(m_mongo, document));
	    		}
	    		else if (childBorder.contains(p)) {
	    			matches.add(MapEntity.getEntity(m_mongo, document));
	    		}
	    	}
	    }
	    return matches;
	} 

	public List<MapEntity> getStateMatches(Position p, MapBounds bounds, List<MapEntity> countries) {
		List<MapEntity> states = new ArrayList<MapEntity>();
		for (MapEntity countryEntity : countries) {
			List<MapEntity> s = countryEntity.getMatches(p, bounds);
			if (s != null) {
				states.addAll(s);
			}
		}
		if (states.size() > 0) {
			return states;
		}
		return null;
	} 

	public MapEntity addStateProv(Country country, String stateProv) {
		MapEntity countryEntity = getCountry(country);
		if (countryEntity != null) {
			return countryEntity.addEntity(stateProv);
		}
		else {
			countryEntity = addCountry(country);
			if (countryEntity != null) {
				return countryEntity.addEntity(stateProv);
			}
			m_logger.error("failed to add "+country+" to DB");
		}
		return null;
	}

	public MapEntity getStateProv(Country country, String stateProv) {
		MapEntity countryEntity = getCountry(country);
		if (countryEntity != null) {
			return countryEntity.getEntity(stateProv);
		}
		return null;
	}

	public MapEntity addCity(Country country, String stateProv, String city) {
		if (stateProv != null) {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.addEntity(city);
			}
			else {
				stateEntity = addStateProv(country, stateProv);
				if (stateEntity != null) {
					return stateEntity.addEntity(city);
				}
				m_logger.error("failed to add segment because "+stateProv+" could not be added");
			}
		}
		else {
			m_logger.error("failed to add segment because "+stateProv+" is null");
		}
		return null;
	}

	public MapEntity getCity(Country country, String stateProv, String city, MapBorder segBorder) {
		if (city != null) {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.getEntity(city, segBorder);
			}
		}
		return null;
	}

	public MapEntity addTown(Country country, String stateProv, String city, String town, MapBorder segBorder) {
		MapEntity parentEntity = null;
		if (city == null) {
			parentEntity = getStateProv(country, stateProv);
			if (parentEntity != null) {
				return parentEntity.addEntity(town, segBorder);
			}
			else {
				parentEntity = addStateProv(country, stateProv);
				if (parentEntity != null) {
					return parentEntity.addEntity(town, segBorder);
				}
				m_logger.error("failed to add segment because "+stateProv+" could be added");
			}
		}
		else {
			parentEntity = getCity(country, stateProv, city, segBorder);
			if (parentEntity != null) {	
				return parentEntity.addEntity(town, segBorder);
			}
			else {
				parentEntity = addCity(country, stateProv, city);
				if (parentEntity != null) {
					return parentEntity.addEntity(town, segBorder);
				}
				m_logger.error("failed to add segment because "+city+" could be added");
			}
		}
		return null;
	}

	public MapEntity getTown(Country country, String stateProv, String city, String town, MapBorder segBorder) {
		if (city != null) {
			MapEntity cityEntity = getCity(country, stateProv, city, segBorder);
			if (cityEntity != null) {
				return cityEntity.getEntity(town, segBorder);
			}
		}
		else {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.getEntity(town, segBorder);
			}
		}
		return null;
	}

	public MapEntity addStreet(Country country, String stateProv, String city, String town, String street, MapBorder segBorder) {
		MapEntity parentEntity = null;
		if (town == null) {
			parentEntity = getCity(country, stateProv, city, segBorder);
			if (parentEntity != null) {
				return parentEntity.addEntity(street, segBorder);
			}
			parentEntity = addCity(country, stateProv, city);
			if (parentEntity != null) {
				return parentEntity.addEntity(street, segBorder);
			}
			m_logger.error("failed to add segment because "+city+" could be added");
		}
		else {
		 	parentEntity = getTown(country, stateProv, city, town, segBorder);
		 	if (parentEntity != null) {
		 		return parentEntity.addEntity(street, segBorder);
		 	}
		 	else {
		 		parentEntity = addTown(country, stateProv, city, town, segBorder);
		 		if (parentEntity != null) {
		 			return parentEntity.addEntity(street, segBorder);
		 		}
		 		m_logger.error("failed to add segment because "+town+" could not be added");
		 	}

		}		
		return null;
	}
	//
	//  Possible FIXME: we do not pass the border to getEntity because we assume that each street name is unique within the town/city
	//                  if we find a case where this is not true, we need to create a border for the street
	//
	public MapEntity getStreet(Country country, String stateProv, String loc1, String loc1, String street, MapBorder segBorder) {
		if (town != null) {
			MapEntity townEntity = getTown(country, stateProv, city, town, segBorder);
			if (townEntity != null) {
				return townEntity.getEntity(street);
			}
		}
		else {
			MapEntity cityEntity = getCity(country, stateProv, city, segBorder);
			if (cityEntity != null) {
				return cityEntity.getEntity(street);
			}
		}
		return null;
	}

	private boolean addSegment(Country country, String stateProv, String loc1, String loc2, 
				String street, StreetSegment segment, MapBorder segBorder) {
		if (street != null) {
			MapEntity streetEntity = getStreet(country, stateProv, loc1, loc2, street, segBorder);
			if (streetEntity != null) {
				return streetEntity.addSegment(segment, segBorder);
			}
			else {
				streetEntity = addStreet(country, stateProv, loc1, loc2, street, segBorder);
				if (streetEntity != null) {
					return streetEntity.addSegment(segment, segBorder);
				}
				m_logger.error("failed to add segment because "+street+" could not be added");
			}
		}
		else {
			m_logger.error("failed to add segment because there is no street");
		}
		return false;
	}

	public boolean addSegment(StreetSegment segment) {
		MapBorder segBorder = new MapBorder(segment.getPoints()); 
		Address address = Address.reverseGeocode(segBorder.getCenter());
		if (address != null) {
			Country country = address.getCountry() == null ? null : Country.countryMap.get(address.getCountry());
			String state = address.getProvinceState();
			String admin = getAdminRegion()
			String city = address.getCity();
			String town = address.getTown();
			String street = address.getStreetName();

			if (!addSegment(country, state, city, town, street, segment, segBorder)) {
				m_logger.error("failed to segment with address "+address.toString());
			}
		}
		else {
			m_logger.error("could not get address from "+segBorder.getCenter().toString());
		}
		return false;
	}

	private List<MapEntity> getCountryEntities(Position p, MapBounds bounds, MyAddress myAddress) {
		List<MapEntity> countryEntities = getCountryMatches(p, bounds);		
		if (countryEntities == null) {
			myAddress.address = Address.reverseGeocode(p);
			if (myAddress.address == null) {
				m_logger.error("could not get address from "+p.toString());
				return null;
			}
			if (myAddress.address.getCountry() == null) {
				m_logger.error("could not get country from "+myAddress.address.toString());
				return null;
			}
			Country country = Country.countryMap.get(myAddress.address.getCountry());
			if (country == null) {
				m_logger.error("country "+myAddress.address.getCountry()+" is not in the database");
			}
			countryEntities = new ArrayList<MapEntity>();
			MapEntity entity = getCountry(country);
			if (entity == null) {
				m_logger.error("country "+country.toString()+" is not in the database");
				return null;
			}
			countryEntities.add(entity);
		}
		return countryEntities;
	}

	private List<MapEntity> getStateEntities(Position p, MapBounds bounds, MyAddress myAddress, List<MapEntity> countryEntities) {
		List<MapEntity> stateEntities = getStateMatches(p, bounds, countryEntities);
		if (stateEntities == null) {
			stateEntities = new ArrayList<MapEntity>();
			if (myAddress.address == null) {
				myAddress.address = Address.reverseGeocode(p);
				if (myAddress.address == null) {
					m_logger.error("could not get address from "+p.toString());
					return null;
				}
			}
			String state = myAddress.address.getProvinceState();
			if (state == null) {
				m_logger.error("could not get state from "+myAddress.address.toString());
				return null;
			}
			for (MapEntity ctryEnt : countryEntities) {
				MapEntity se = ctryEnt.getEntity(state);
				if (se != null) {
					if (ctryEnt.getName().equals(myAddress.address.getCountry())) {
						stateEntities.add(se);
					}
				}
			}			
			if (stateEntities.size() == 0) {
				m_logger.error("state "+state+" is not in the database");
				return null;
			}
			
		}
		return stateEntities;
	}

	private List<MapEntity> getMatchesByType(Position p, MapBounds bounds, List<MapEntity> entities, MapEntityType type) {
		List<MapEntity> matches = new ArrayList<MapEntity>();
		for (MapEntity ent : entities) {
			List<MapEntity> m = ent.getMatches(p, bounds, type);
			if (m != null) {
				matches.addAll(m);
			}
		}
		if (matches.size() > 0) {
			return matches;
		}
		return null;
	}

	private List<MapEntity> getLocalityEntities(Position p, MapBounds bounds, MyAddress myAddress, List<MapEntity> stateEntities) {
		List<MapEntity> localityEntities = new ArrayList<MapEntity>();
		List<MapEntity> cityEntities = getMatchesByType(p, bounds, stateEntities, MapEntityType.City);		
		if (cityEntities != null) {
			localityEntities.addAll(cityEntities);
			List<MapEntity> cityTownEntities = getMatchesByType(p, bounds, cityEntities, MapEntityType.Town);
			if (cityTownEntities != null) {
				localityEntities.addAll(cityTownEntities);
			}
		}
		List<MapEntity> townEntities = getMatchesByType(p, bounds, stateEntities, MapEntityType.Town);
		if (townEntities != null) {
			localityEntities.addAll(townEntities);
		}
		if (localityEntities.size() > 0) {
			return localityEntities;
		}
		if (myAddress.address == null) {
			myAddress.address = Address.reverseGeocode(p);
			if (myAddress.address == null) {
				m_logger.error("could not get address from "+p.toString());
				return null;
			}
		}
		String state = myAddress.address.getProvinceState();
		String city = myAddress.address.getCity();
		String town = myAddress.address.getTown();
		if (city != null || town != null) {
			for (MapEntity stateEnt : stateEntities) {
				MapEntity ce = stateEnt.getEntity(city);
				MapEntity te = null;
				if (ce != null && stateEnt.getName().equals(state)) {
					localityEntities.add(ce);
					te = ce.getEntity(town);
					if (te != null) {
						localityEntities.add(te);
					}
				}
				te = stateEnt.getEntity(town);
				if (te != null) {
					localityEntities.add(te);
				}
			}
		}
		if (localityEntities.size() > 0) {
			return localityEntities;
		}
		return null;
	}

	private List<MapEntity> getStreetEntiies(Position p, MapBounds bounds, MyAddress myAddress, List<MapEntity> localityEntities) {
		List<MapEntity> streetEntities = getMatchesByType(p, bounds, localityEntities, MapEntityType.Street);
		if (streetEntities != null) {
			return streetEntities;
		}
		if (myAddress.address == null) {
			myAddress.address = Address.reverseGeocode(p);
			if (myAddress.address == null) {
				m_logger.error("could not get address from "+p.toString());
				return null;
			}
		}
		streetEntities = new ArrayList<MapEntity>();
		String street = myAddress.address.getStreetName();
		String city = myAddress.address.getCity();
		String town = myAddress.address.getTown();
		if (street != null) {
			for (MapEntity locEnt : localityEntities) {
				if (locEnt.getName().equals(city) || locEnt.getName().equals(town)) {
					MapEntity streetEnt = locEnt.getEntity(street);
					if (streetEnt != null) {
						streetEntities.add(streetEnt);
					}
				}
			}
		}	
		if (streetEntities.size() > 0) {
			return streetEntities;
		}
		return null;	
	}

	private List<StreetSegment> getSegments(Position p, MapBounds bounds, List<MapEntity> streetEntities) {
		List<StreetSegment> segments = new ArrayList<StreetSegment>();
		for (MapEntity ent : streetEntities) {
			List<StreetSegment> segs = ent.getSegments(p, bounds);
			if (segs != null) {
				segments.addAll(segs);
			}
		}
		if (segments.size() > 0) {
			return segments;
		}
		return null;
	}

	public List<StreetSegment> search(Position p, MapBounds bounds) {
		MyAddress myAddress = new MyAddress();
		String errorMessage = myAddress.address == null ? p.toString() : myAddress.address.toString();
		List<MapEntity> countryEntities = getCountryEntities(p, bounds, myAddress);
		if (countryEntities != null) {
			List<MapEntity> stateEntities = getStateEntities(p, bounds, myAddress, countryEntities);
			if (stateEntities != null) {
				List<MapEntity> localityEntities = getLocalityEntities(p, bounds, myAddress, stateEntities);					
				if (localityEntities != null) {
					List<MapEntity> streetEntities = getStreetEntiies(p, bounds, myAddress, localityEntities);
					if (streetEntities != null) {
						return getSegments(p, bounds, streetEntities);
					}
					else {
						m_logger.error("no streets found for "+errorMessage);
					}
				}
				else {
					m_logger.error("no localities found for "+errorMessage);
				}
			}
			else {  // do we want to support an state-less address?
				
				m_logger.error("no state available for "+errorMessage);
			}		
		}
		m_logger.error("no country found for "+errorMessage);
		return null;
	}
	
}

class MyAddress {
	public Address address;
}