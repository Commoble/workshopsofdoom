package commoble.workshopsofdoom;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.settings.StructureSeparationSettings;

// we override a placement check here to keep the structure from spawning near villages (same logic as pillager outposts)
public class PillagerJigsawStructure extends LoadableJigsawStructure
{

	public PillagerJigsawStructure(Codec<LoadableJigsawConfig> codec, Decoration generationStage, boolean restrictSpawnBoxes, Supplier<List<Spawners>> monsterSpawnerGetter,
		Supplier<List<Spawners>> creatureSpawnerGetter, int maxSeperation, int minSeperation, int placementSalt, boolean transformSurroundingLand)
	{
		super(codec, generationStage, restrictSpawnBoxes, monsterSpawnerGetter, creatureSpawnerGetter, maxSeperation, minSeperation, placementSalt, transformSurroundingLand);
	}

	protected boolean func_230363_a_(ChunkGenerator generator, BiomeProvider biomeProvider, long seed, SharedSeedRandom random, int x, int z,
		Biome p_230363_8_, ChunkPos p_230363_9_, VillageConfig p_230363_10_)
	{
		int i = x >> 4;
		int j = z >> 4;
		random.setSeed(i ^ j << 4 ^ seed);
		random.nextInt();
		if (random.nextInt(5) != 0)
		{
			return false;
		}
		else
		{
			return !this.isNearVillage(generator, seed, random, x, z);
		}
	}

	private boolean isNearVillage(ChunkGenerator generator, long seed, SharedSeedRandom random, int x, int z)
	{
		StructureSeparationSettings villageSeparator = generator.func_235957_b_().func_236197_a_(Structure.VILLAGE);
		if (villageSeparator == null)
		{
			return false;
		}
		else
		{
			for (int i = x - 10; i <= x + 10; ++i)
			{
				for (int j = z - 10; j <= z + 10; ++j)
				{
					ChunkPos villagePos = Structure.VILLAGE.getChunkPosForStructure(villageSeparator, seed, random, i, j);
					if (i == villagePos.x && j == villagePos.z)
					{
						return true;
					}
				}
			}

			return false;
		}
	}
}
