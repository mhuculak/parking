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
import java.text.Normalizer;
import java.text.Normalizer.Form;

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
		BasicDBObject searchQuery = new BasicDBObject("entity", country.toString());
		DBCursor cursor = m_map.find(searchQuery);
		MapEntityType type = MapEntityType.Country;
		if (cursor.count()==0) {
			m_logger.log("Add Country "+country.toString()+" to map DB");
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("entity", country.toString());
			document.append("type", type.toString());
			MapEntity newEntity = new MapEntity(m_mongo, id.toString(), null, country.toString(), type);
			m_map.insert(document);
			return newEntity;
		}
		else {
			m_logger.log("Country "+country.toString()+" already exists");
		}
		return null;
	}

	public MapEntity getCountry(Country country) {
		BasicDBObject searchQuery = new BasicDBObject("entity", country.toString());
		DBCursor cursor = m_map.find(searchQuery);
		MapEntityType type = MapEntityType.Country;
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			MapEntity entity = MapEntity.getEntity(m_mongo, null, document);
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
	    			matches.add(MapEntity.getEntity(m_mongo, null, document));
	    		}
	    		else if (childBorder.contains(p)) {
	    			matches.add(MapEntity.getEntity(m_mongo, null, document));
	    		}
	    	}
	    }
	    if (matches.size() > 0) {
	    	return matches;
	    }
	    return null;
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

	public MapEntity addLoc1(Country country, String stateProv, Locality loc1) {
		if (stateProv != null) {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.addEntity(loc1);
			}
			else {
				stateEntity = addStateProv(country, stateProv);
				if (stateEntity != null) {
					return stateEntity.addEntity(loc1);
				}
				m_logger.error("failed to add segment because "+stateProv+" could not be added");
			}
		}
		else {
			m_logger.error("failed to add segment because "+stateProv+" is null");
		}
		return null;
	}

	public MapEntity getLoc1(Country country, String stateProv, Locality loc1, MapBorder segBorder) {
		if (loc1 != null) {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.getEntity(loc1, segBorder);
			}
		}
		return null;
	}

	public MapEntity addLoc2(Country country, String stateProv, Locality loc1, Locality loc2, MapBorder segBorder) {
		MapEntity parentEntity = null;
		if (loc2 == null) {
			m_logger.error("VIOLATION: addLoc2 assumes that loc2 is not null!"); // FIXME: throw exception here
			return null;
		}
		if (loc1 == null) {
			parentEntity = getStateProv(country, stateProv);
			if (parentEntity != null) {
				return parentEntity.addEntity(loc2, segBorder);
			}
			else {
				parentEntity = addStateProv(country, stateProv);
				if (parentEntity != null) {
					return parentEntity.addEntity(loc2, segBorder);
				}
				m_logger.error("failed to add segment because "+stateProv+" could be added");
			}
		}
		else {
			parentEntity = getLoc1(country, stateProv, loc1, segBorder);
			if (parentEntity != null) {	
				return parentEntity.addEntity(loc2, segBorder);
			}
			else {
				parentEntity = addLoc1(country, stateProv, loc1);
				if (parentEntity != null) {
					return parentEntity.addEntity(loc2, segBorder);
				}
				m_logger.error("failed to add segment because "+loc1+" could be added");
			}
		}
		return null;
	}

	public MapEntity getLoc2(Country country, String stateProv, Locality loc1, Locality loc2, MapBorder segBorder) {
		if (loc1 != null) {
			MapEntity loc1Entity = getLoc1(country, stateProv, loc1, segBorder);
			if (loc1Entity != null) {
				return loc1Entity.getEntity(loc2, segBorder);
			}
		}
		else {
			MapEntity stateEntity = getStateProv(country, stateProv);
			if (stateEntity != null) {
				return stateEntity.getEntity(loc2, segBorder);
			}
		}
		return null;
	}

	public MapEntity addStreet(Country country, String stateProv, Locality loc1, Locality loc2, String street, MapBorder segBorder) {
		MapEntity parentEntity = null;
		if (loc2 == null) {
			parentEntity = getLoc1(country, stateProv, loc1, segBorder);
			if (parentEntity != null) {
				return parentEntity.addEntity(street, segBorder);
			}
			parentEntity = addLoc1(country, stateProv, loc1);
			if (parentEntity != null) {
				return parentEntity.addEntity(street, segBorder);
			}
			m_logger.error("failed to add segment because "+loc1+" could be added");
		}
		else {
		 	parentEntity = getLoc2(country, stateProv, loc1, loc2, segBorder);
		 	if (parentEntity != null) {
		 		return parentEntity.addEntity(street, segBorder);
		 	}
		 	else {
		 		parentEntity = addLoc2(country, stateProv, loc1, loc2, segBorder);
		 		if (parentEntity != null) {
		 			return parentEntity.addEntity(street, segBorder);
		 		}
		 		m_logger.error("failed to add segment because "+loc2+" could not be added");
		 	}

		}		
		return null;
	}
	//
	//  Possible FIXME: we do not pass the border to getEntity because we assume that each street name is unique within the town/city
	//                  if we find a case where this is not true, we need to create a border for the street
	//
	public MapEntity getStreet(Country country, String stateProv, Locality loc1, Locality loc2, String street, MapBorder segBorder) {
		if (loc2 != null) {
			MapEntity loc2Entity = getLoc2(country, stateProv, loc1, loc2, segBorder);
			if (loc2Entity != null) {
				return loc2Entity.getEntity(street);
			}
		}
		else {
			MapEntity loc1Entity = getLoc1(country, stateProv, loc1, segBorder);
			if (loc1Entity != null) {
				return loc1Entity.getEntity(street);
			}
		}
		return null;
	}

	private boolean addSegment(Country country, String stateProv, Locality loc1, Locality loc2, 
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
		MyAddress myAddress = new MyAddress(segment.getStreetName());
		if (myAddress.set(segBorder.getCenter())) {
			Country country = myAddress.country() == null ? null : Country.countryReverseMap.get(myAddress.country());
			String state = myAddress.state();
			String admin = myAddress.admin();
			String city = myAddress.city();
			String town = myAddress.town();
			String street = myAddress.street();
			m_logger.log("Add segment to country ="+country.toString()+" state = "+state+" street = "+street);
			if (town != null) {
				if (city != null) {
					m_logger.log("Add segment to loc1 ="+city+"(city) loc2 = "+town+"(town)");
					Locality loc1 = new Locality(city, MapEntityType.City);
					Locality loc2 = new Locality(town, MapEntityType.Town);
					if (!addSegment(country, state, loc1, loc2, street, segment, segBorder)) {
						m_logger.error("failed to segment with address "+myAddress.toString());
					}
					else {
						return true;
					}
				}
				else {
					m_logger.log("Add segment to loc1 ="+admin+"(admin) loc2 = "+town+"(town)");
					Locality loc1 = admin == null ? null : new Locality(admin, MapEntityType.AdminRegion);
					Locality loc2 = new Locality(town, MapEntityType.Town);
					if (!addSegment(country, state, loc1, loc2, street, segment, segBorder)) {
						m_logger.error("failed to segment with address "+myAddress.toString());
					}
					else {
						return true;
					}
				}
			}
			else {
				m_logger.log("Add segment to loc1 ="+admin+"(admin) loc2 = "+city+"(city)");
				Locality loc1 = admin == null ? null : new Locality(admin, MapEntityType.AdminRegion);
				Locality loc2 = city == null ? null : new Locality(city, MapEntityType.Town);
				if (!addSegment(country, state, loc1, loc2, street, segment, segBorder)) {
					m_logger.error("failed to segment with address "+myAddress.toString());
				}
				else {
					return true;
				}
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
			if (!myAddress.set(p)) {
				m_logger.error("Cannot get address from "+p.toString());
				return null;
			}
			String countryString = myAddress.country();
			if (countryString == null) {
				m_logger.error("could not get country from "+myAddress.toString());
				return null;
			}
			Country country = Country.countryReverseMap.get(countryString);
			if (country == null) {
				m_logger.error("country "+countryString+" is not in the database");
			}
			countryEntities = new ArrayList<MapEntity>();
			MapEntity entity = getCountry(country);
			if (entity == null) {
				m_logger.error("country "+country.toString()+" is not in the database");
				return null;
			}
			countryEntities.add(entity);
		}
		else {
			m_logger.log("found "+countryEntities.size()+" country entities");
		}
		return countryEntities;
	}

	private List<MapEntity> getStateEntities(Position p, MapBounds bounds, MyAddress myAddress, List<MapEntity> countryEntities) {
		List<MapEntity> stateEntities = getStateMatches(p, bounds, countryEntities);
		if (stateEntities == null) {
			stateEntities = new ArrayList<MapEntity>();
			if (!myAddress.set(p)) {
				m_logger.error("Cannot get address from "+p.toString());
				return null;
			}
			String state = myAddress.state();
			if (state == null) {
				m_logger.error("could not get state from "+myAddress.toString());
				return null;
			}
			for (MapEntity ctryEnt : countryEntities) {
				m_logger.log("get state entity "+state+" from country entity "+ctryEnt.getName());
				MapEntity se = ctryEnt.getEntity(state);
				if (se != null) {
					m_logger.log(state+" found");
					Country country = Country.valueOf(ctryEnt.getName());
					String countryString = Country.countryMap.get(country);
					if (countryString.equals(myAddress.country())) {
						m_logger.log(state+"adding "+state);
						stateEntities.add(se);
					}
					else {
						m_logger.log("country "+myAddress.country()+" does not match "+countryString+" "+ctryEnt.getName());
					}
				}
				else {
					m_logger.log(state+" not found");
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

	//
	//  FIXME: need to add AdminRegion
	//
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
			m_logger.log(" found "+localityEntities.size()+" locality entities");
			return localityEntities;
		}
		if (!myAddress.set(p)) {
			m_logger.error("Cannot get address from "+p.toString());
			return null;
		}
		String state = myAddress.state();
		String city = myAddress.city();
		String town = myAddress.town();
		if (city != null || town != null) {
			for (MapEntity stateEnt : stateEntities) {
				MapEntity ce = stateEnt.getEntity(city);
				MapEntity te = null;
				if (ce != null) {
					if ( stateEnt.getName().equals(state)) {
						m_logger.log("add entity for "+city);
						localityEntities.add(ce);
						te = ce.getEntity(town);
						if (te != null) {
							m_logger.log("add entity for "+town+" from "+city);
							localityEntities.add(te);
						}
						else {
							m_logger.log("no entity found for "+town);
						}
					}
					else {
						m_logger.log(stateEnt.getName()+" does not match "+state);
					}
				}
				else {
					m_logger.log("no entity found for "+city);
				}
				te = stateEnt.getEntity(town);
				if (te != null) {
					m_logger.log("add entity for "+town);
					localityEntities.add(te);
				}
				else {
					m_logger.log("no entity found for "+town);
				}
			}
		}
		if (localityEntities.size() > 0) {
			return localityEntities;
		}
		return null;
	}

	//
	//  FIXME: need to add AdminRegion
	//
	private List<MapEntity> getStreetEntiies(Position p, MapBounds bounds, MyAddress myAddress, List<MapEntity> localityEntities) {
		List<MapEntity> streetEntities = getMatchesByType(p, bounds, localityEntities, MapEntityType.Street);
		if (streetEntities != null) {
			m_logger.log(" found "+streetEntities.size()+" street entities");
			return streetEntities;
		}
		if (!myAddress.set(p)) {
			m_logger.error("Cannot get address from "+p.toString());
			return null;
		}
		streetEntities = new ArrayList<MapEntity>();
		String street = myAddress.street();
		String city = myAddress.city();
		String town = myAddress.town();
		if (street != null) {
			m_logger.log(" look for "+street+" in "+localityEntities.size()+" locality ntities");
			for (MapEntity locEnt : localityEntities) {
				if (locEnt.getName().equals(city) || locEnt.getName().equals(town)) {
					m_logger.log("locality "+locEnt.getName()+" matches "+city+" or "+town);
					MapEntity streetEnt = locEnt.getEntity(street);
					if (streetEnt != null) {
						m_logger.log("found street "+street+" in "+locEnt.getName());
						streetEntities.add(streetEnt);
					}
					else {
						m_logger.log("street "+street+" not found in "+locEnt.getName());
					}
				}
				else {
					m_logger.log("locality "+locEnt.getName()+" does not match "+city+" or "+town);
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
		MyAddress myAddress = new MyAddress(null);
		List<MapEntity> countryEntities = getCountryEntities(p, bounds, myAddress);
		if (countryEntities != null) {
			m_logger.log(" found "+countryEntities.size()+" country entities");
			List<MapEntity> stateEntities = getStateEntities(p, bounds, myAddress, countryEntities);
			if (stateEntities != null) {
				m_logger.log(" found "+stateEntities.size()+" state entities");
				List<MapEntity> localityEntities = getLocalityEntities(p, bounds, myAddress, stateEntities);					
				if (localityEntities != null) {
					m_logger.log(" found "+localityEntities.size()+" locality entities");
					List<MapEntity> streetEntities = getStreetEntiies(p, bounds, myAddress, localityEntities);
					if (streetEntities != null) {
						m_logger.log(" found "+streetEntities.size()+" street entities");
						return getSegments(p, bounds, streetEntities);
					}
					else {
						m_logger.error("no streets found for "+myAddress.asString(p));
					}
				}
				else {
					m_logger.error("no localities found for "+myAddress.asString(p));
				}
			}
			else {  // do we want to support an state-less address?
				
				m_logger.error("no state available for "+myAddress.asString(p));
			}		
		}
		m_logger.error("no country found for "+myAddress.asString(p));
		return null;
	}
	
}

class MyAddress {
	private Address address;
	private String streetName; //  the street name from the originating segment must override the reverse geocode result

	public MyAddress(String streetName) {
		this.streetName = normalize(streetName);
	}
	public boolean set(Position p) {		
		if (address == null) {
			address = Address.reverseGeocode(p);
			if (address != null && streetName == null) {
				streetName = normalize(address.getStreetName());
			}
			return address != null;
		}
		return true;
	}
	public String street() {
		return streetName;
	}
	public String town() {
		return normalize(address.getTown());
	}
	public String city() {
		return normalize(address.getCity());
	}
	public String admin() {
		return normalize(address.getAdminRegion());
	}
	public String state() {
		return normalize(address.getProvinceState());
	}
	public String country() {
		return normalize(address.getCountry());
	}
	public String toString() {
		return address.toString();
	}
	public String asString(Position p) {
		if (set(p)) {
			return address.toString();
		}
		return p.toString();
	}

	public String normalize(String value) {
		if (value != null) {
			String norm = Normalizer.normalize(value, Form.NFD);
			String clean = norm.replaceAll("[^\\p{ASCII}]", "");
			return clean;
		}
		return null;
	}
}