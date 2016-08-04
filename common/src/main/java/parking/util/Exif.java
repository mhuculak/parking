package parking.util;

import parking.map.Position;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.imaging.ImageProcessingException;


import java.io.File;
import java.io.IOException;

public class Exif {
	
	public static void readExif(File file) {
		System.out.println("Reading exif header of file" + file.getAbsolutePath());
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			print(metadata);
		}
		catch (ImageProcessingException ex) {
			System.out.println(ex);
		} catch (IOException ex) {
            System.out.println(ex);
        }
        
	}

	public static Position getPosition(File file) {
		Position p = new Position();
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {
					String tagName = tag.getTagName();
					if (tagName.contains("GPS")) {
						String desc = tag.getDescription();
//						System.out.println(tagName + ":" + desc);
						if (tagName.equals("GPS Longitude")) {							
//							System.out.println("long = " + desc);
							p.setLongitude(desc);
						}
						else if (tagName.equals("GPS Latitude")) {							
//							System.out.println("lat = " + desc);
							p.setLatitude(desc);
						}
					}
				}
			}
		}
		catch (ImageProcessingException ex) {
			System.out.println(ex);
		} catch (IOException ex) {
            System.out.println(ex);
        }
        System.out.println("got position " + p.toString());
        return p;
	}

	private static void print(Metadata metadata)
    {
        System.out.println("-------------------------------------");

        // Iterate over the data and print to System.out

        //
        // A Metadata object contains multiple Directory objects
        //
        for (Directory directory : metadata.getDirectories()) {

            //
            // Each Directory stores values in Tag objects
            //
            for (Tag tag : directory.getTags()) {
              	String tagName = tag.getTagName();
              	String desc = tag.getDescription();
            	if (tagName.contains("GPS")) { // only care about GPS for now
            		System.out.println(tagName + " " + desc);
            	}
            }

            //
            // Each Directory may also contain error messages
            //
            if (directory.hasErrors()) {
                for (String error : directory.getErrors()) {
                    System.err.println("ERROR: " + error);
                }
            }
        }
    }

}