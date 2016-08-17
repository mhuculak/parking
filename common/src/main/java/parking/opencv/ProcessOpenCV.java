package parking.opencv;

import parking.opencv.Line;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.Point;

import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.util.List;
import java.util.ArrayList;

public class ProcessOpenCV {

	static boolean openCVinit = false;

	public static void initOpenCV() {
		try {
			if (openCVinit == false) {
				System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
				openCVinit = true;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static BufferedImage scale(BufferedImage img, double factor) {

		Mat source = OpenCVConvert.bufferedImageToMat(img);
         
		System.out.println("original image has " + source.cols() + " cols " + source.rows() + " rows");
		int rows = (int)(source.rows() * factor +0.5);
		int cols = (int)(source.cols() * factor+0.5);
		Mat dest = new Mat(cols, rows, source.type());
		System.out.println("new image has " + cols + " cols " + rows + " rows");
		dest = source;
		Imgproc.pyrDown(source, dest, new Size(cols, rows));

		return OpenCVConvert.matToBufferedImage(dest, null);
	}

	public static BufferedImage nop(BufferedImage img) {
		System.out.println("Convert buffered image to Mat");
		Mat source = OpenCVConvert.bufferedImageToMat(img);
		System.out.println("Convert Mat to buffered image");
		return OpenCVConvert.matToBufferedImage(source, null);
	}

	public static List<Circle> getCircles(BufferedImage image, int expectedRadius) {
		Mat sourceImage = OpenCVConvert.bufferedImageToMat(image);
		Mat grayLevelImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		Imgproc.cvtColor(sourceImage, grayLevelImage, Imgproc.COLOR_RGB2GRAY);
		Mat filteredImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		Size size = new Size(3,3);
		Imgproc.blur(grayLevelImage, filteredImage, size);
		return getCircles2(filteredImage, expectedRadius);
	}

	public static List<Circle> getCircles2(Mat sourceImage, int expectedRadius) {
		List<Circle> cir = new ArrayList<Circle>();
		int iCannyUpperThreshold = 200;
		int iMinRadius = expectedRadius/2;
		int iMaxRadius = expectedRadius*2;
		int iAccumulator = 100;
		Mat circleImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		System.out.println("Find cirecles with min radius =" + iMinRadius + " max radius = "+ iMaxRadius);
		Imgproc.HoughCircles(sourceImage, circleImage, Imgproc.CV_HOUGH_GRADIENT, 1, 10, iCannyUpperThreshold, iAccumulator, iMinRadius, iMaxRadius);

    	if (circleImage.cols() > 0) {
    		for (int x = 0; x < circleImage.cols(); x++) {
        		double vCircle[] = circleImage.get(0,x);

        		if (vCircle == null) {
            		break;
            	}

        		Point pt = new Point(vCircle[0], vCircle[1]);
        		double r = vCircle[2];
        		Circle circle = new Circle( pt, r);
        		System.out.println("Found circle "+circle);
           		cir.add( circle);
      		}
      	}
      	return cir;
    }

	public static List<Line> getLinesP(Mat sourceImage) {
		Mat lineImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		int threshold = 50;
    	int minLineSize = 20;
    	int lineGap = 20;
    	Imgproc.HoughLinesP(sourceImage, lineImage, 1, Math.PI/180, threshold, minLineSize, lineGap);
    	
    	List<Line> lines = new ArrayList<Line>();
    	for (int x = 0; x < lineImage.cols(); x++) {
          	double[] vec = lineImage.get(0, x);
          	double x1 = vec[0], 
                 y1 = vec[1],
                 x2 = vec[2],
                 y2 = vec[3];
          	Point start = new Point(x1, y1);
          	Point end = new Point(x2, y2);
          	Line line = new Line(start, end);          
          	lines.add(line);
    	}	
    	return lines;
	}

	public static List<Line> getLines(Mat sourceImage) {
		Mat lineImage = new Mat(sourceImage.height(),sourceImage.width(),CvType.CV_8UC1);
		Imgproc.HoughLines(sourceImage, lineImage, 1, Math.PI/180, 100);
		List<Line> lines = new ArrayList<Line>();
    	
    	return lines;
    }
}