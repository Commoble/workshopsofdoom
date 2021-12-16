package commoble.workshopsofdoom.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

import net.minecraft.Util;

public class ReflectionUtils
{
	public static final sun.misc.Unsafe UNSAFE = Util.make(() ->
	{
		try
		{
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (Unsafe) f.get(null);
		}
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	});
}
