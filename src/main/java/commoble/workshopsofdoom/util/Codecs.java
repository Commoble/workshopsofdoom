package commoble.workshopsofdoom.util;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;

/**
 * Additional codecs that don't have anywhere better to be, such as non-canonical codecs for vanilla types
 */
public class Codecs
{
	/**
	 * Vanilla JigsawConfiguration codec disallows sizes > 7, this doesn't
	 */
	public static final Codec<JigsawConfiguration> JUMBO_JIGSAW_CONFIG_CODEC = RecordCodecBuilder.create(instance -> instance.group(
				StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(JigsawConfiguration::startPool),
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("size").forGetter(JigsawConfiguration::maxDepth)
			).apply(instance, JigsawConfiguration::new)
		);
	
	public static <T extends Enum<T>> Codec<Set<T>> makeEnumSetCodec(Codec<T> codec)
	{
		return codec.listOf().xmap(EnumSet::copyOf, ImmutableList::copyOf);
	}
}
