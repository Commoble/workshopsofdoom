package commoble.workshopsofdoom.util;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Additional codecs that don't have anywhere better to be, such as non-canonical codecs for vanilla types
 */
public class Codecs
{	
	public static <T extends Enum<T>> Codec<Set<T>> makeEnumSetCodec(Codec<T> codec)
	{
		return codec.listOf().xmap(EnumSet::copyOf, ImmutableList::copyOf);
	}
}
