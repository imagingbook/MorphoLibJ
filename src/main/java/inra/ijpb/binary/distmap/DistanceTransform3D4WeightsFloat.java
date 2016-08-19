package inra.ijpb.binary.distmap;

import static java.lang.Math.min;

import java.util.ArrayList;

import ij.ImageStack;
import inra.ijpb.algo.AlgoStub;
import inra.ijpb.data.image.Images3D;

/**
 * Computes 3D distance transform in a 5x5x5 neighborhood using four chamfer
 * weights and floating point calculation.
 * 
 * @author David Legland
 * 
 */
public class DistanceTransform3D4WeightsFloat extends AlgoStub implements DistanceTransform3D 
{
	private final static int DEFAULT_MASK_LABEL = 255;

	private float[] weights;

	int maskLabel = DEFAULT_MASK_LABEL;

	/**
	 * Flag for dividing final distance map by the value first weight. 
	 * This results in distance map values closer to Euclidean, but with 
	 * non integer values. 
	 */
	private boolean normalizeMap = true;

	
	private int sizeX;
	private int sizeY;
	private int sizeZ;

	private byte[][] maskSlices;

	/**
	 * The result image that will store the distance map. The content
	 * of the buffer is updated during forward and backward iterations.
	 */
	private float[][] resultSlices;
	
	
	/**
	 * Default constructor that specifies the chamfer weights.
	 * @param weights an array of four weights direction orthogonal, diagonal, cube diagonal, and (+-2,+-1,+-1).
	 */
	public DistanceTransform3D4WeightsFloat(float[] weights)
	{
		this.weights = weights;
		if (weights.length < 4)
		{
			throw new IllegalArgumentException("Weights array must have length equal to 4");
		}
	}

	/**
	 * Constructor specifying the chamfer weights and the optional normalization.
	 * @param weights
	 *            an array of two weights for orthogonal and diagonal directions
	 * @param normalize
	 *            flag indicating whether the final distance map should be
	 *            normalized by the first weight
	 */
	public DistanceTransform3D4WeightsFloat(float[] weights, boolean normalize)
	{
		this(weights);
		this.normalizeMap = normalize;
	}

	/**
	 * Computes the distance map from a 3D binary image. Distance is computed
	 * for each foreground (white) pixel, as the chamfer distance to the nearest
	 * background (black) pixel.
	 * 
	 * @param image
	 *            a 3D binary image with white pixels (255) as foreground
	 * @return a new 3D image containing:
	 *         <ul>
	 *         <li>0 for each background pixel</li>
	 *         <li>the distance to the nearest background pixel otherwise</li>
	 *         </ul>
	 */
	public ImageStack distanceMap(ImageStack image) 
	{
		// size of image
		sizeX = image.getWidth();
		sizeY = image.getHeight();
		sizeZ = image.getSize();
		
		// store wrapper to mask image
		this.maskSlices = Images3D.getByteArrays(image);

		// create new empty image, and fill it with black
		ImageStack buffer = ImageStack.create(sizeX, sizeY, sizeZ, 32);
		this.resultSlices = Images3D.getFloatArrays(buffer);

		// initialize empty image with either 0 (background) or max value (foreground)
		fireStatusChanged(this, "Initialization..."); 
		for (int z = 0; z < sizeZ; z++) 
		{
			byte[] maskSlice = this.maskSlices[z];
			float[] resultSlice = this.resultSlices[z];
			
			for (int y = 0; y < sizeY; y++) 
			{
				for (int x = 0; x < sizeX; x++) 
				{
					int index = sizeX * y + x;
					int val = maskSlice[index];
					resultSlice[index] = val == 0 ? 0 : Float.MAX_VALUE;
				}
			}
		}
		fireProgressChanged(this, 1, 1); 
		
		// Two iterations are enough to compute distance map to boundary
		forwardIteration();
		backwardIteration();

		// Normalize values by the first weight
		if (this.normalizeMap) 
		{
			fireStatusChanged(this, "Normalize map..."); 
			for (int z = 0; z < sizeZ; z++) 
			{
				fireProgressChanged(this, z, sizeZ); 

				byte[] maskSlice = this.maskSlices[z];
				float[] resultSlice = this.resultSlices[z];
				
				for (int y = 0; y < sizeY; y++) 
				{
					for (int x = 0; x < sizeX; x++) 
					{
						int index = sizeX * y + x;
						if (maskSlice[index] != 0)
						{
							resultSlice[index] = resultSlice[index] / weights[0];
						}
					}
				}
			}
			fireProgressChanged(this, 1, 1); 
		}
				
		return buffer;
	}
	
	private void forwardIteration() 
	{
		fireStatusChanged(this, "Forward scan...");
		
		// create array of forward shifts
		ArrayList<WeightedOffset> offsets = new ArrayList<WeightedOffset>();
		
		// offsets in the z-2 plane
		offsets.add(new WeightedOffset(-1, -1, -2, weights[3]));
		offsets.add(new WeightedOffset(+1, -1, -2, weights[3]));
		offsets.add(new WeightedOffset(-1, +1, -2, weights[3]));
		offsets.add(new WeightedOffset(+1, +1, -2, weights[3]));

		// offsets in the z-1 plane
		offsets.add(new WeightedOffset(-1, -1, -1, weights[2]));
		offsets.add(new WeightedOffset( 0, -1, -1, weights[1]));
		offsets.add(new WeightedOffset(+1, -1, -1, weights[2]));
		offsets.add(new WeightedOffset(-1,  0, -1, weights[1]));
		offsets.add(new WeightedOffset( 0,  0, -1, weights[0]));
		offsets.add(new WeightedOffset(+1,  0, -1, weights[1]));
		offsets.add(new WeightedOffset(-1, +1, -1, weights[2]));
		offsets.add(new WeightedOffset( 0, +1, -1, weights[1]));
		offsets.add(new WeightedOffset(+1, +1, -1, weights[2]));
		
		offsets.add(new WeightedOffset(-1, -2, -1, weights[3]));
		offsets.add(new WeightedOffset(+1, -2, -1, weights[3]));
		offsets.add(new WeightedOffset(-2, -1, -1, weights[3]));
		offsets.add(new WeightedOffset(+2, -1, -1, weights[3]));
		offsets.add(new WeightedOffset(-2, +1, -1, weights[3]));
		offsets.add(new WeightedOffset(+2, +1, -1, weights[3]));
		offsets.add(new WeightedOffset(-1, +2, -1, weights[3]));
		offsets.add(new WeightedOffset(+1, +2, -1, weights[3]));
		
		// offsets in the current plane
		offsets.add(new WeightedOffset(-1, -1, 0, weights[1]));
		offsets.add(new WeightedOffset( 0, -1, 0, weights[0]));
		offsets.add(new WeightedOffset(+1, -1, 0, weights[1]));
		offsets.add(new WeightedOffset(-1,  0, 0, weights[0]));

		// iterate on image voxels
		for (int z = 0; z < sizeZ; z++)
		{
			fireProgressChanged(this, z, sizeZ);
			
			byte[] maskSlice = this.maskSlices[z];
			float[] currentSlice = this.resultSlices[z];
			
			for (int y = 0; y < sizeY; y++)
			{
				for (int x = 0; x < sizeX; x++)
				{
					int index = sizeX * y + x;

					// check if we need to update current voxel
					if ((maskSlice[index] & 0x00FF) != maskLabel)
						continue;
					
					double value = currentSlice[index];
					
					double newVal = Float.MAX_VALUE;
					for (WeightedOffset offset : offsets)
					{
						int x2 = x + offset.dx;
						int y2 = y + offset.dy;
						int z2 = z + offset.dz;
						
						// check that current neighbor is within image
						if (x2 >= 0 && x2 < sizeX && y2 >= 0 && y2 < sizeY && z2 >= 0 && z2 < sizeZ)
						{
							newVal = min(newVal, resultSlices[z2][sizeX * y2 + x2] + offset.weight);
						}
						
						if (newVal < value) 
						{
							currentSlice[index] = (float) newVal;
						}
					}
				}
			}
		}
		fireProgressChanged(this, 1, 1); 
	}

	private void backwardIteration() 
	{
		fireStatusChanged(this, "Backward scan..."); 
		
		// create array of backward shifts
		ArrayList<WeightedOffset> offsets = new ArrayList<WeightedOffset>();
		
		// offsets in the z+2 plane
		offsets.add(new WeightedOffset(-1, -1, +2, weights[3]));
		offsets.add(new WeightedOffset(+1, -1, +2, weights[3]));
		offsets.add(new WeightedOffset(-1, +1, +2, weights[3]));
		offsets.add(new WeightedOffset(+1, +1, +2, weights[3]));

		// offsets in the z+1 plane
		offsets.add(new WeightedOffset(-1, -1, +1, weights[2]));
		offsets.add(new WeightedOffset( 0, -1, +1, weights[1]));
		offsets.add(new WeightedOffset(+1, -1, +1, weights[2]));
		offsets.add(new WeightedOffset(-1,  0, +1, weights[1]));
		offsets.add(new WeightedOffset( 0,  0, +1, weights[0]));
		offsets.add(new WeightedOffset(+1,  0, +1, weights[1]));
		offsets.add(new WeightedOffset(-1, +1, +1, weights[2]));
		offsets.add(new WeightedOffset( 0, +1, +1, weights[1]));
		offsets.add(new WeightedOffset(+1, +1, +1, weights[2]));
		
		offsets.add(new WeightedOffset(-1, -2, +1, weights[3]));
		offsets.add(new WeightedOffset(+1, -2, +1, weights[3]));
		offsets.add(new WeightedOffset(-2, -1, +1, weights[3]));
		offsets.add(new WeightedOffset(+2, -1, +1, weights[3]));
		offsets.add(new WeightedOffset(-2, +1, +1, weights[3]));
		offsets.add(new WeightedOffset(+2, +1, +1, weights[3]));
		offsets.add(new WeightedOffset(-1, +2, +1, weights[3]));
		offsets.add(new WeightedOffset(+1, +2, +1, weights[3]));
		
		// offsets in the current plane
		offsets.add(new WeightedOffset(-1, +1, 0, weights[1]));
		offsets.add(new WeightedOffset( 0, +1, 0, weights[0]));
		offsets.add(new WeightedOffset(+1, +1, 0, weights[1]));
		offsets.add(new WeightedOffset(+1,  0, 0, weights[0]));

		// iterate on image voxels in backward order
		for (int z = sizeZ - 1; z >= 0; z--)
		{
			fireProgressChanged(this, sizeZ-1-z, sizeZ); 
			
			byte[] maskSlice = this.maskSlices[z];
			float[] currentSlice = this.resultSlices[z];
			
			for (int y = sizeY - 1; y >= 0; y--)
			{
				for (int x = sizeX - 1; x >= 0; x--)
				{
					int index = sizeX * y + x;

					// check if we need to update current voxel
					if ((maskSlice[index] & 0x00FF) != maskLabel)
						continue;
					
					double value = currentSlice[index];
					
					double newVal = Float.MAX_VALUE;
					for (WeightedOffset offset : offsets)
					{
						int x2 = x + offset.dx;
						int y2 = y + offset.dy;
						int z2 = z + offset.dz;
						
						// check that current neighbor is within image
						if (x2 >= 0 && x2 < sizeX && y2 >= 0 && y2 < sizeY && z2 >= 0 && z2 < sizeZ)
						{
							newVal = min(newVal, resultSlices[z2][sizeX * y2 + x2] + offset.weight);
						}
						
						if (newVal < value) 
						{
							currentSlice[index] = (float) newVal;
						}
					}
				}
			}
		}
		fireProgressChanged(this, 1, 1); 
	}
	
	private class WeightedOffset
	{
		int dx;
		int dy;
		int dz;
		float weight;
		
		public WeightedOffset(int dx, int dy, int dz, float weight)
		{
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.weight = weight;
		}
	}
}
