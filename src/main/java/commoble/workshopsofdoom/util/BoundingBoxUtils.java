package commoble.workshopsofdoom.util;

import javax.annotation.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class BoundingBoxUtils
{
	/**
	 * Returns the intersection of two bounding boxes
	 * @param a A bounding box
	 * @param b Another bounding box
	 * @return The intersection of the boxes, or null if the resulting box would be inverted or have a volume of less than 1
	 */
	public static @Nullable BoundingBox intersection(BoundingBox a, BoundingBox b)
	{
		int minX = Math.max(a.minX(), b.minX());
		int maxX = Math.min(a.maxX(), b.maxX());
		if (maxX < minX)
			return null;
		int minY = Math.max(a.minY(), b.minY());
		int maxY = Math.min(a.maxY(), b.maxY());
		if (maxY < minY)
			return null;
		int minZ = Math.max(a.minZ(), b.minZ());
		int maxZ = Math.min(a.maxZ(), b.maxZ());
		if (maxZ < minZ)
			return null;
		
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	/**
	 * @param outer A bounding box in normal form (min dimensions <= max dimensions)
	 * @param inner Another bounding box in normal form
	 * @return 
	 * Returns true if the inner BoundingBox is entirely contained within the outer bounding box, false otherwise
	 */
	public static boolean doesBoxEncapsulate(BoundingBox outer, BoundingBox inner)
	{
		return outer.minX() <= inner.minX()
			&& outer.minY() <= inner.minY()
			&& outer.minZ() <= inner.minZ()
			&& outer.maxX() >= inner.maxX()
			&& outer.maxY() >= inner.maxY()
			&& outer.maxZ() >= inner.maxZ();
	}
}
