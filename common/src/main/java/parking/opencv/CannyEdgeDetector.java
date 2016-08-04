package parking.opencv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.Scalar;

import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;

public class CannyEdgeDetector {

	private Mat sourceImage;
	private Mat grayLevelImage;
	private Mat destination;
	private Mat filteredImage;
	private Mat detectedEdges;
	private final int maxLowThresh = 100;
	private int highLowRatio = 3;
	private int edgeThresh = 1;
	private int kernalSize = 3;

	public CannyEdgeDetector(BufferedImage image) {
		System.out.println("Convert to Mat");
		sourceImage = OpenCVConvert.bufferedImageToMat(image);
		System.out.println("image size " + sourceImage.width() + " x " + sourceImage.height());
		System.out.println("image has " + sourceImage.channels() + " channels");
		System.out.println("image has " + sourceImage.depth() + " depth");
		System.out.println("Convert to greyscale");
        grayLevelImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
        Imgproc.cvtColor(sourceImage, grayLevelImage, Imgproc.COLOR_RGB2GRAY);
        filteredImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		Size size = new Size(3,3);
		System.out.println("Filter to remove noise");
		Imgproc.blur(grayLevelImage, filteredImage, size);
	}

	public Mat doCannyThreshold(int lowThreshold) {
		detectedEdges = new Mat(filteredImage.height(),filteredImage.width(),CvType.CV_8UC1);
		System.out.println("Canny Edge Detector with low threashold " + lowThreshold);
		Imgproc.Canny( filteredImage, detectedEdges, lowThreshold,
				lowThreshold*highLowRatio, kernalSize, false);
/*		
		destination = new Mat();		
		Core.add( destination, Scalar.all(0), destination);
		sourceImage.copyTo(destination, detectedEdges);
		return OpenCVConvert.matToBufferedImage(destination, null);
*/
		return detectedEdges;		
	}
}