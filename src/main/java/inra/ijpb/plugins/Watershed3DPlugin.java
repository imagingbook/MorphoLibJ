package inra.ijpb.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.watershed.Watershed;


/**
 * A plugin to perform watershed on a 3D image using flooding simulations, 
 * as described by Soille, Pierre, and Luc M. Vincent. "Determining watersheds 
 * in digital pictures via flooding simulations." Lausanne-DL tentative. 
 * International Society for Optics and Photonics, 1990.
 *
 * @author Ignacio Arganda-Carreras
 */
public class Watershed3DPlugin implements PlugIn 
{

	/** flag to use 26-connectivity */
	public static boolean use26neighbors = true;
	
		
	/**
	 * Apply 3D watershed to a 2D or 3D image (it does work for 2D images too).
	 *	 
	 * @param input the 2D or 3D image (in principle a "gradient" image)
	 * @param mask binary mask to restrict region of interest
	 * @param connectivity 6 or 26 voxel connectivity
	 */
	public ImagePlus process(
			ImagePlus input, 
			ImagePlus mask,
			int connectivity ) 
	{
		final long start = System.currentTimeMillis();
				
		ImagePlus resultImage = Watershed.computeWatershed( input, mask, connectivity );
					
		final long end = System.currentTimeMillis();
		IJ.log( "Watershed 3d took " + (end-start) + " ms.");
						
		return resultImage;
			
	}
	

	@Override
	public void run(String arg) 
	{
		int nbima = WindowManager.getImageCount();
		
		if( nbima == 0 )
		{
			IJ.error( "Watershed 3D", 
					"ERROR: At least one image needs to be open to run watershed in 3D");
			return;
		}
		
        String[] names = new String[ nbima ];
        String[] namesMask = new String[ nbima + 1 ];

        namesMask[ 0 ] = "None";
        
        for (int i = 0; i < nbima; i++) 
        {
            names[ i ] = WindowManager.getImage(i + 1).getShortTitle();
            namesMask[ i + 1 ] = WindowManager.getImage(i + 1).getShortTitle();
        }
        
        GenericDialog gd = new GenericDialog("Watershed 3D");

        int inputIndex = 0;
        int maskIndex = nbima > 1 ? 1 : 0;
        
        gd.addChoice( "Input", names, names[ inputIndex ] );
        gd.addChoice( "Mask", namesMask, namesMask[ maskIndex ] );
        gd.addCheckbox( "Use diagonal connectivity", use26neighbors );

        gd.showDialog();
        
        if (gd.wasOKed()) 
        {
            inputIndex = gd.getNextChoiceIndex();
            maskIndex = gd.getNextChoiceIndex();
            use26neighbors = gd.getNextBoolean();

            ImagePlus inputImage = WindowManager.getImage( inputIndex + 1 );
            ImagePlus maskImage = maskIndex > 0 ? WindowManager.getImage( maskIndex ) : null;
            
            final int connectivity = use26neighbors ? 26 : 6;
            
            ImagePlus result = process( inputImage, maskImage, connectivity );
            
            // Adjust range to visualize result
    		Images3D.optimizeDisplayRange( result );
            
    		// Set result slice to the current slice in the input image
            result.setSlice( inputImage.getCurrentSlice() );
            
            // show result
            result.show();
        }
		
	}

}
