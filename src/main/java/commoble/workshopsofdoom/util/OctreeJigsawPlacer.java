package commoble.workshopsofdoom.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Queues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.EmptyPoolElement;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

// as JigsawPlacement but with octrees instead of voxelshapes
// that means the whole thing has to be rewritten! eugh
public record OctreeJigsawPlacer(Registry<StructureTemplatePool> templatePools, int maxDepth, JigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, StructureManager structureManager, List<? super PoolElementStructurePiece> pieces, Random rand, Deque<OctreePieceState> placingQueue)
{
	public static final Logger LOGGER = LogManager.getLogger();
	
	public static Optional<PieceGenerator<JigsawConfiguration>> addPieces(PieceGeneratorSupplier.Context<JigsawConfiguration> context, JigsawPlacement.PieceFactory pieceFactory, BlockPos origin, boolean snapToHeightMap)
	{
		long seed = context.seed();
		ChunkPos chunkPos = context.chunkPos();
		RegistryAccess registries = context.registryAccess();
		JigsawConfiguration config = context.config();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		StructureManager structureManager = context.structureManager();
		LevelHeightAccessor heightAccessor = context.heightAccessor();
		Predicate<Biome> biomePredicate = context.validBiome();
		
		WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(0L));
		rand.setLargeFeatureSeed(seed, chunkPos.x, chunkPos.z);
		StructureFeature.bootstrap();
		Registry<StructureTemplatePool> templatePools = registries.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
		Rotation rotation = Rotation.getRandom(rand);
		StructureTemplatePool startPool = config.startPool().get();
		StructurePoolElement startElement = startPool.getRandomTemplate(rand);
		if (startElement == EmptyPoolElement.INSTANCE)
		{
			return Optional.empty();
		}
		
		PoolElementStructurePiece startPiece = pieceFactory.create(
			structureManager, startElement, origin, startElement.getGroundLevelDelta(), rotation,
			startElement.getBoundingBox(structureManager, origin, rotation));
		
		BoundingBox startBounds = startPiece.getBoundingBox();
		int centerX = (startBounds.maxX() + startBounds.minX()) / 2;
		int centerZ = (startBounds.maxZ() + startBounds.minZ()) / 2;
		int startHeight = snapToHeightMap
			? origin.getY() + chunkGenerator.getFirstFreeHeight(centerX, centerZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor)
			: origin.getY();
		
		if (!biomePredicate.test(chunkGenerator.getNoiseBiome(QuartPos.fromBlock(centerX), QuartPos.fromBlock(startHeight), QuartPos.fromBlock(centerZ))))
			return Optional.empty();
		
		int adjustedHeight = startBounds.minY() + startPiece.getGroundLevelDelta();
		startPiece.move(0, startHeight-adjustedHeight, 0);
		return Optional.of((builder,subContext)->
		{
			List<PoolElementStructurePiece> pieces = Lists.newArrayList();
			pieces.add(startPiece);
			if (config.maxDepth() > 0)
			{
				int radius = 80;
				BoundingBox totalBounds = new BoundingBox(centerX-radius, startHeight-radius, centerZ-radius, centerX+radius, startHeight+radius, centerZ+radius);
				OctreeJigsawPlacer placer = new OctreeJigsawPlacer(templatePools, config.maxDepth(), pieceFactory, chunkGenerator, structureManager, pieces, rand, Queues.newArrayDeque());
				SubtractiveOctree octree = new SubtractiveOctree.NonEmpty(totalBounds);
				boolean totallySubtracted = octree.subtract(startBounds);
				if (totallySubtracted)
				{
					octree = SubtractiveOctree.Empty.INSTANCE;
				}
				placer.placingQueue.addLast(new OctreePieceState(startPiece, octree, 0));
				
				while (!placer.placingQueue.isEmpty())
				{
					OctreePieceState state = placer.placingQueue.removeFirst();
					placer.tryPlacingChildren(state, heightAccessor);
				}
				
				pieces.forEach(builder::addPiece);
			}
		});
	}
	
	private static Predicate<StructureTemplatePool> isValidPool(ResourceLocation location)
	{
		return pool -> pool.size() != 0 || Objects.equals(location, Pools.EMPTY.location());
	}
	
	private void tryPlacingChildren(OctreePieceState state, LevelHeightAccessor heightAccessor)
	{
		SubtractiveOctree totalOctree = state.octree();
		PoolElementStructurePiece parentPiece = state.piece();
		StructurePoolElement parentElement = parentPiece.getElement();
		BlockPos parentPos = parentPiece.getPosition();
		Rotation parentRotation = parentPiece.getRotation();
		StructureTemplatePool.Projection parentProjection = parentElement.getProjection();
		boolean parentRigid = parentProjection == StructureTemplatePool.Projection.RIGID;
		BoundingBox parentBounds = parentPiece.getBoundingBox();
		SubtractiveOctree parentOctree = new SubtractiveOctree.NonEmpty(parentBounds);
		int parentFloorY = parentBounds.minY();
		
		forEachJigsaw:
		for (StructureBlockInfo parentJigsaw : parentElement.getShuffledJigsawBlocks(this.structureManager, parentPos, parentRotation, this.rand))
		{
			Direction jigsawFacing = JigsawBlock.getFrontFacing(parentJigsaw.state);
			BlockPos parentJigsawPos = parentJigsaw.pos;
			BlockPos jigsawNeighborPos = parentJigsawPos.relative(jigsawFacing);
			int jigsawOffsetY = parentJigsawPos.getY() - parentFloorY;
			int firstFreeHeight = -1;
			ResourceLocation poolLocation = new ResourceLocation(parentJigsaw.nbt.getString("pool"));
			Optional<StructureTemplatePool> maybePool = templatePools.getOptional(poolLocation)
				.filter(isValidPool(poolLocation));
			if (maybePool.isEmpty())
			{
				LOGGER.warn("Empty or non-existent pool: {}", poolLocation);
				continue;
			}
			StructureTemplatePool pool = maybePool.get();
			ResourceLocation fallbackId = pool.getFallback();
			Optional<StructureTemplatePool> maybeFallback = templatePools.getOptional(fallbackId)
				.filter(isValidPool(fallbackId));
			if (maybeFallback.isEmpty())
			{
				LOGGER.warn("Empty or non-existent fallback pool: {}", fallbackId);
				continue;
			}
			StructureTemplatePool fallbackPool = maybeFallback.get();
			boolean inside = parentBounds.isInside(jigsawNeighborPos);
			SubtractiveOctree permittedSpace = inside
				? parentOctree
				: totalOctree;
			List<StructurePoolElement> elements = new ArrayList<>();
			if (state.depth() != this.maxDepth)
			{
				elements.addAll(pool.getShuffledTemplates(this.rand));
			}
			elements.addAll(fallbackPool.getShuffledTemplates(this.rand));
			for (StructurePoolElement childElement : elements)
			{
				if (childElement == EmptyPoolElement.INSTANCE)
				{
					break;
				}
				for (Rotation childRotation : Rotation.getShuffled(this.rand))
				{
					List<StructureBlockInfo> childJigsaws = childElement.getShuffledJigsawBlocks(this.structureManager, BlockPos.ZERO, childRotation, this.rand);
					for (StructureBlockInfo childJigsaw : childJigsaws)
					{
						if (JigsawBlock.canAttach(parentJigsaw, childJigsaw))
						{
							BlockPos childJigsawPos = childJigsaw.pos;
							BlockPos childJigsawOffset = jigsawNeighborPos.subtract(childJigsawPos);
							BoundingBox childBounds = childElement.getBoundingBox(this.structureManager, childJigsawOffset, childRotation);
							int childBoundsFloorY = childBounds.minY();
							StructureTemplatePool.Projection childProjection = childElement.getProjection();
							boolean childRigid = childProjection == StructureTemplatePool.Projection.RIGID;
							int childJigsawY = childJigsawPos.getY();
							int childDeltaY = jigsawOffsetY - childJigsawY + JigsawBlock.getFrontFacing(parentJigsaw.state).getStepY();
							boolean mutualRigid = parentRigid && childRigid;
							if (!mutualRigid && firstFreeHeight == -1)
							{
								firstFreeHeight = this.chunkGenerator.getFirstFreeHeight(parentJigsawPos.getX(), parentJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor);
							}
							int childJigsawTargetY = mutualRigid
								? parentFloorY + childDeltaY
								: firstFreeHeight - childJigsawY;
							int childJigsawOffsetY = childJigsawTargetY - childBoundsFloorY;
							BoundingBox offsetChildBounds = childBounds.moved(0, childJigsawOffsetY, 0);
							BlockPos adjustedChildJigsawOffset = childJigsawOffset.offset(0, childJigsawOffsetY, 0);
							// (legacy offset hack ignored here)
							if (permittedSpace.contains(offsetChildBounds))
							{
								permittedSpace.subtract(offsetChildBounds);
								int parentGroundLevelDelta = parentPiece.getGroundLevelDelta();
								int adjustedChildGroundDelta = childRigid ? parentGroundLevelDelta - childDeltaY : childElement.getGroundLevelDelta();
								PoolElementStructurePiece childPiece = this.pieceFactory.create(this.structureManager, childElement, adjustedChildJigsawOffset, adjustedChildGroundDelta, childRotation, offsetChildBounds);
								if (!mutualRigid && firstFreeHeight == -1)
								{
									firstFreeHeight = this.chunkGenerator.getFirstFreeHeight(parentJigsawPos.getX(), parentJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor);
								}
								int finalChildJigsawY = parentRigid ? parentJigsawPos.getY()
									: childRigid ? childJigsawY + childJigsawTargetY
									: firstFreeHeight + childDeltaY/2;
								parentPiece.addJunction(new JigsawJunction(jigsawNeighborPos.getX(), finalChildJigsawY - jigsawOffsetY + parentGroundLevelDelta, jigsawNeighborPos.getZ(), childDeltaY, childProjection));
								childPiece.addJunction(new JigsawJunction(parentJigsawPos.getX(), finalChildJigsawY - childJigsawY + adjustedChildGroundDelta, parentJigsawPos.getZ(), -childDeltaY, parentProjection));
								this.pieces.add(childPiece);
								int nextDepth = state.depth() + 1;
								if (nextDepth <= this.maxDepth)
								{
									this.placingQueue.addLast(new OctreePieceState(childPiece, permittedSpace, nextDepth));
								}
								continue forEachJigsaw;
							}
						}
					}
				}
			}
		}
	}
}
