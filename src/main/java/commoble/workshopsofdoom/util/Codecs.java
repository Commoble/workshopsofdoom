package commoble.workshopsofdoom.util;

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
}
