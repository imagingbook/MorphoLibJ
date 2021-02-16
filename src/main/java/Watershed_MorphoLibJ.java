import java.io.File;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.Watershed;

/**
 * Taken from imglib2-tutorials
 */
public class Watershed_MorphoLibJ {

	public Watershed_MorphoLibJ() {
		// define the file to open
//		File file = new File( "DrosophilaWing.tif" );
//		File file = new File("D://images/boats-tiny.png");
//		File file = new File("D://images/boats-tiny-w2.png");
//		File file = new File("D://images/boats-tiny-b.png");
//		File file = new File("D://images/boats.png");
		File file = new File("D://images/boats-gauss5.png");

		// open a file with ImageJ
		ImagePlus imp = new Opener().openImage(file.getAbsolutePath());
		ImageProcessor ip = imp.getProcessor();
		
		imp.show();
		
		ImageProcessor ip2 = Watershed.computeWatershed(ip, null, 4);
		new ImagePlus("Watershed1", ip2).show();
		
		ip.flipHorizontal();
		ImageProcessor ip3 = Watershed.computeWatershed(ip, null, 4);
		new ImagePlus("Watershed1", ip3).show();
	}
	
	public static void main(String[] args) {
		// open an ImageJ window
		new ImageJ();

		// run the example
		new Watershed_MorphoLibJ();
	}
	

}

