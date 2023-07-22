package commoble.workshopsofdoom.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

// as JigsawPlacement but with octrees instead of voxelshapes
// that means the whole thing has to be rewritten! eugh
public record OctreeJigsawPlacer(Registry<StructureTemplatePool> templatePools, int maxDepth, ChunkGenerator chunkGenerator, StructureTemplateManager structureManager, List<? super PoolElementStructurePiece> pieces, RandomSource rand, Deque<OctreePieceState> placingQueue)
{
	public static final Logger LOGGER = LogManager.getLogger();
	
	public static Optional<Structure.GenerationStub> addPieces(Structure.GenerationContext context, Holder<StructureTemplatePool> startPoolHolder, Optional<ResourceLocation> startJigsawName, int maxDepth, BlockPos chunkCornerPos, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter)
	{
		RegistryAccess registries = context.registryAccess();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
		LevelHeightAccessor heightAccessor = context.heightAccessor();
		WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(0L));
		Registry<StructureTemplatePool> templatePools = registries.registryOrThrow(Registries.TEMPLATE_POOL);
		Rotation rotation = Rotation.getRandom(rand);
		StructureTemplatePool startPool = startPoolHolder.value();
		StructurePoolElement startElement = startPool.getRandomTemplate(rand);
		if (startElement == EmptyPoolElement.INSTANCE)
		{
			return Optional.empty();
		}
		
		BlockPos startPos = chunkCornerPos;
		if (startJigsawName.isPresent())
		{
			ResourceLocation name = startJigsawName.get();
			Optional<BlockPos> randomJigsawPos = JigsawPlacement.getRandomNamedJigsaw(startElement, name, chunkCornerPos, rotation, structureTemplateManager, rand);
			if (randomJigsawPos.isEmpty())
			{
				LOGGER.error("No starting jigsaw {} found in start pool {}", name, startPoolHolder.unwrapKey().get().location());
				return Optional.empty();
			}
			startPos = randomJigsawPos.get();
		}
		
		Vec3i startPosInChunk = startPos.subtract(chunkCornerPos);
		BlockPos startPosReverseOffset = chunkCornerPos.subtract(startPosInChunk);
		
		PoolElementStructurePiece startPiece = new PoolElementStructurePiece(
			structureTemplateManager,
			startElement,
			startPosReverseOffset,
			startElement.getGroundLevelDelta(),
			rotation,
			startElement.getBoundingBox(structureTemplateManager, startPosReverseOffset, rotation));
		
		BoundingBox startBounds = startPiece.getBoundingBox();
		int centerX = (startBounds.maxX() + startBounds.minX()) / 2;
		int centerZ = (startBounds.maxZ() + startBounds.minZ()) / 2;
		int startHeight = projectStartToHeightmap
			.map(heightmap -> chunkCornerPos.getY() + chunkGenerator.getFirstFreeHeight(centerX, centerZ, heightmap, heightAccessor, context.randomState()))
			.orElse(startPosReverseOffset.getY());
		
		int adjustedHeight = startBounds.minY() + startPiece.getGroundLevelDelta();
		startPiece.move(0, startHeight-adjustedHeight, 0);
		int adjustedStartHeight = startHeight + startPosInChunk.getY();
		return Optional.of(new Structure.GenerationStub(new BlockPos(centerX, adjustedStartHeight, centerZ), builder -> {
			List<PoolElementStructurePiece> pieces = Lists.newArrayList();
			pieces.add(startPiece);
			if (maxDepth > 0)
			{
				BoundingBox totalBounds = new BoundingBox(centerX-maxDistanceFromCenter, startHeight-maxDistanceFromCenter, centerZ-maxDistanceFromCenter, centerX+maxDistanceFromCenter, startHeight+maxDistanceFromCenter, centerZ+maxDistanceFromCenter);
				OctreeJigsawPlacer placer = new OctreeJigsawPlacer(templatePools, maxDepth, chunkGenerator, structureTemplateManager, pieces, rand, Queues.newArrayDeque());
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
					placer.tryPlacingChildren(state, heightAccessor, context.randomState());
				}
				
				pieces.forEach(builder::addPiece);
			}
		}));
	}
	
	private static Predicate<StructureTemplatePool> isValidPool(ResourceLocation location)
	{
		return pool -> pool.size() != 0 || Objects.equals(location, Pools.EMPTY.location());
	}
	
	private void tryPlacingChildren(OctreePieceState state, LevelHeightAccessor heightAccessor, RandomState randomState)
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
			Direction jigsawFacing = JigsawBlock.getFrontFacing(parentJigsaw.state());
			BlockPos parentJigsawPos = parentJigsaw.pos();
			BlockPos jigsawNeighborPos = parentJigsawPos.relative(jigsawFacing);
			int jigsawOffsetY = parentJigsawPos.getY() - parentFloorY;
			int firstFreeHeight = -1;
			ResourceLocation poolLocation = new ResourceLocation(parentJigsaw.nbt().getString("pool"));
			Optional<StructureTemplatePool> maybePool = templatePools.getOptional(poolLocation)
				.filter(isValidPool(poolLocation));
			if (maybePool.isEmpty())
			{
				LOGGER.warn("Empty or non-existent pool: {}", poolLocation);
				continue;
			}
			StructureTemplatePool pool = maybePool.get();
			ResourceLocation fallbackId = pool.getFallback().unwrapKey().get().location();
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
							BlockPos childJigsawPos = childJigsaw.pos();
							BlockPos childJigsawOffset = jigsawNeighborPos.subtract(childJigsawPos);
							BoundingBox childBounds = childElement.getBoundingBox(this.structureManager, childJigsawOffset, childRotation);
							int childBoundsFloorY = childBounds.minY();
							StructureTemplatePool.Projection childProjection = childElement.getProjection();
							boolean childRigid = childProjection == StructureTemplatePool.Projection.RIGID;
							int childJigsawY = childJigsawPos.getY();
							int childDeltaY = jigsawOffsetY - childJigsawY + JigsawBlock.getFrontFacing(parentJigsaw.state()).getStepY();
							boolean mutualRigid = parentRigid && childRigid;
							if (!mutualRigid && firstFreeHeight == -1)
							{
								firstFreeHeight = this.chunkGenerator.getFirstFreeHeight(parentJigsawPos.getX(), parentJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
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
								PoolElementStructurePiece childPiece = new PoolElementStructurePiece(this.structureManager, childElement, adjustedChildJigsawOffset, adjustedChildGroundDelta, childRotation, offsetChildBounds);
								if (!mutualRigid && firstFreeHeight == -1)
								{
									firstFreeHeight = this.chunkGenerator.getFirstFreeHeight(parentJigsawPos.getX(), parentJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
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
