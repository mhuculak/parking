package parking.opencv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class OpenCVConvert {


	public static Mat bufferedImageToMat(BufferedImage image) {
		byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, data);
        return mat;
	}

    public static Mat bufferedImageIntToMat(BufferedImage image) {
        int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4); 
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, byteBuffer.array());
        return mat;
    }

	public static BufferedImage matToBufferedImage(Mat matrix, BufferedImage bimg) {
    if ( matrix != null ) { 
        int cols = matrix.cols();  
        int rows = matrix.rows();  
        int elemSize = (int)matrix.elemSize();  
        byte[] data = new byte[cols * rows * elemSize];  
        int type;  
        matrix.get(0, 0, data);  
        switch (matrix.channels()) {  
        case 1:  
            type = BufferedImage.TYPE_BYTE_GRAY;  
            break;  
        case 3:  
            type = BufferedImage.TYPE_3BYTE_BGR;  
            // bgr to rgb  
            byte b;  
            for(int i=0; i<data.length; i=i+3) {  
                b = data[i];  
                data[i] = data[i+2];  
                data[i+2] = b;  
            }  
            break;  
        default:  
            return null;  
        }  

        // Reuse existing BufferedImage if possible
        if (bimg == null || bimg.getWidth() != cols || bimg.getHeight() != rows || bimg.getType() != type) {
            bimg = new BufferedImage(cols, rows, type);
        }        
        bimg.getRaster().setDataElements(0, 0, cols, rows, data);
    } else { // mat was null
        bimg = null;
    }
    return bimg;  
}   
}