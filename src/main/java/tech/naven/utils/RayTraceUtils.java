package tech.naven.utils;

import tech.naven.utils.rotation.Rotation;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RayTraceUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final double EPSILON = 1.0E-7D;
   public static Vec3 eyePos = null;

   public static HitResult rayCast(float partialTicks, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         double distance = mc.player.blockInteractionRange();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(double range, float partialTicks, boolean hitFluids, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         objectMouseOver = pick(range, partialTicks, hitFluids, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(float partialTicks, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         double distance = mc.player.blockInteractionRange();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
      }

      return objectMouseOver;
   }

   public static Vec3 calculateViewVector(float pXRot, float pYRot) {
      float f = pXRot * (float) (Math.PI / 180.0);
      float f1 = -pYRot * (float) (Math.PI / 180.0);
      float f2 = Mth.cos(f1);
      float f3 = Mth.sin(f1);
      float f4 = Mth.cos(f);
      float f5 = Mth.sin(f);
      return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
   }

   public static HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids, float pYRot, float pXRot) {
      Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
      Vec3 vec31 = calculateViewVector(pXRot, pYRot);
      Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
      return mc.player.level().clip(new ClipContext(vec3, vec32, Block.OUTLINE, pHitFluids ? Fluid.ANY : Fluid.NONE, mc.player));
   }

   public static HitResult rayTraceBlocks(
      Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Entity var6
   ) {
      Block var7;
      if (ignoreBlockWithoutBoundingBox) {
         var7 = Block.COLLIDER;
      } else {
         var7 = returnLastUncollidableBlock ? Block.VISUAL : Block.OUTLINE;
      }

      Fluid var8 = stopOnLiquid ? Fluid.ANY : Fluid.NONE;
      ClipContext var9 = new ClipContext(vec31, vec32, var7, var8, var6);
      return mc.level.clip(var9);
   }

   public static EntityHitResult calculateIntercept(AABB instance, Vec3 var1, Vec3 var2) {
      Optional<Vec3> e = instance.clip(var1, var2);
      return e.<EntityHitResult>map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
   }

   public static void updateEyePos() {
      if (mc.player == null) {
         return;
      }

      eyePos = mc.player.getEyePosition(1.0F);
   }

   private static Vec3 eye() {
      if (eyePos != null) {
         return eyePos;
      }

      if (mc.player == null) {
         return Vec3.ZERO;
      }

      eyePos = mc.player.getEyePosition(1.0F);
      return eyePos;
   }

   private static Direction getHitFace(Vec3 hitPos, AABB box) {
      if (Math.abs(hitPos.x - box.minX) < EPSILON) {
         return Direction.WEST;
      }
      if (Math.abs(hitPos.x - box.maxX) < EPSILON) {
         return Direction.EAST;
      }
      if (Math.abs(hitPos.y - box.minY) < EPSILON) {
         return Direction.DOWN;
      }
      if (Math.abs(hitPos.y - box.maxY) < EPSILON) {
         return Direction.UP;
      }
      if (Math.abs(hitPos.z - box.minZ) < EPSILON) {
         return Direction.NORTH;
      }
      if (Math.abs(hitPos.z - box.maxZ) < EPSILON) {
         return Direction.SOUTH;
      }

      return null;
   }

   public static boolean didHitBlockFace(Rotation rotation, BlockPos targetPos, Direction expectedFace, boolean strict) {
      return didHitBlockFace(mc.player, rotation.getYaw(), rotation.getPitch(), targetPos, expectedFace, strict, RayTraceUtils::isIgnoredBlock);
   }

   public static boolean didHitBlockFace(Player player, float yaw, float pitch, BlockPos targetPos, Direction expectedFace, boolean strict) {
      return didHitBlockFace(player, yaw, pitch, targetPos, expectedFace, strict, RayTraceUtils::isIgnoredBlock);
   }

   public static boolean didHitBlockFace(
      Rotation rotation, BlockPos targetPos, Direction expectedFace, boolean strict, Predicate<BlockState> ignorePredicate
   ) {
      return didHitBlockFace(mc.player, rotation.getYaw(), rotation.getPitch(), targetPos, expectedFace, strict, ignorePredicate);
   }

   public static boolean didHitBlockFace(
      Player player,
      float yaw,
      float pitch,
      BlockPos targetPos,
      Direction expectedFace,
      boolean strict,
      Predicate<BlockState> ignorePredicate
   ) {
      if (player == null || expectedFace == null) {
         return false;
      }

      BlockHitResult result = getFacedBlock(yaw, pitch, ignorePredicate);
      if (result == null
         || targetPos.getX() != result.getBlockPos().getX()
         || targetPos.getY() != result.getBlockPos().getY()
         || targetPos.getZ() != result.getBlockPos().getZ()
         || expectedFace != result.getDirection() && strict) {
         return false;
      }

      return true;
   }

   public static BlockHitResult getFacedBlock(float yaw, float pitch) {
      return getFacedBlock(yaw, pitch, RayTraceUtils::isIgnoredBlock);
   }

   public static BlockHitResult getFacedBlock(float yaw, float pitch, Predicate<BlockState> ignorePredicate) {
      if (mc.player == null || mc.level == null) {
         return null;
      }

      double reachDistance = mc.player.blockInteractionRange();
      if (yaw == 0.0F && pitch == 0.0F) {
         return null;
      }

      Vec3 startPos = eye();
      Vec3 direction = calculateViewVector(pitch, yaw);
      Vec3 endPos = startPos.add(direction.x * reachDistance, direction.y * reachDistance, direction.z * reachDistance);
      if (direction.x == 0.0) {
         direction = new Vec3(EPSILON, direction.y, direction.z);
      }
      if (direction.y == 0.0) {
         direction = new Vec3(direction.x, EPSILON, direction.z);
      }
      if (direction.z == 0.0) {
         direction = new Vec3(direction.x, direction.y, EPSILON);
      }

      BlockPos currentPos = BlockPos.containing(startPos.x, startPos.y, startPos.z);
      int stepX = (int)Math.signum(direction.x);
      int stepY = (int)Math.signum(direction.y);
      int stepZ = (int)Math.signum(direction.z);

      double nextBoundaryX = stepX > 0 ? (double)(currentPos.getX() + 1) : (double)currentPos.getX();
      double nextBoundaryY = stepY > 0 ? (double)(currentPos.getY() + 1) : (double)currentPos.getY();
      double nextBoundaryZ = stepZ > 0 ? (double)(currentPos.getZ() + 1) : (double)currentPos.getZ();

      double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
      double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
      double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

      double tDeltaX = (double)stepX / direction.x;
      double tDeltaY = (double)stepY / direction.y;
      double tDeltaZ = (double)stepZ / direction.z;

      while (startPos.distanceTo(Vec3.atCenterOf(currentPos)) <= reachDistance) {
         if (!mc.level.isEmptyBlock(currentPos)) {
            BlockState state = mc.level.getBlockState(currentPos);
            if (!ignorePredicate.test(state)) {
               VoxelShape shape;
               FluidState fluidState = mc.level.getFluidState(currentPos);
               if (fluidState.isSource() && fluidState.getType() == Fluids.WATER) {
                  shape = Shapes.block();
               } else {
                  shape = state.getCollisionShape(mc.level, currentPos, CollisionContext.of(mc.player));
               }

               if (!shape.isEmpty()) {
                  for (AABB localBox : shape.toAabbs()) {
                     AABB box = localBox.move(currentPos.getX(), currentPos.getY(), currentPos.getZ());
                     Optional<Vec3> intercept = box.clip(startPos, endPos);
                     if (intercept.isPresent()) {
                        Vec3 hitVec = intercept.get();
                        Direction side = getHitFaceFromBox(hitVec, box);
                        boolean isInside = box.contains(startPos);
                        return new BlockHitResult(hitVec, side, currentPos, isInside);
                     }
                  }
               }
            }
         }

         if (tMaxX < tMaxY) {
            if (tMaxX < tMaxZ) {
               currentPos = currentPos.offset(stepX, 0, 0);
               tMaxX += tDeltaX;
            } else {
               currentPos = currentPos.offset(0, 0, stepZ);
               tMaxZ += tDeltaZ;
            }
         } else if (tMaxY < tMaxZ) {
            currentPos = currentPos.offset(0, stepY, 0);
            tMaxY += tDeltaY;
         } else {
            currentPos = currentPos.offset(0, 0, stepZ);
            tMaxZ += tDeltaZ;
         }
      }

      return null;
   }

   public static boolean isIgnoredBlock(BlockState state) {
      return state.getBlock() instanceof BushBlock
         || state.getBlock() instanceof SnowLayerBlock
         || state.getBlock() instanceof AirBlock
         || state.getBlock() instanceof LiquidBlock;
   }

   private static Direction getHitFaceFromBox(Vec3 hit, AABB box) {
      final double eps = 1.0E-7;
      if (Math.abs(hit.x - box.minX) <= eps) {
         return Direction.WEST;
      }
      if (Math.abs(hit.x - box.maxX) <= eps) {
         return Direction.EAST;
      }
      if (Math.abs(hit.y - box.minY) <= eps) {
         return Direction.DOWN;
      }
      if (Math.abs(hit.y - box.maxY) <= eps) {
         return Direction.UP;
      }
      if (Math.abs(hit.z - box.minZ) <= eps) {
         return Direction.NORTH;
      }
      if (Math.abs(hit.z - box.maxZ) <= eps) {
         return Direction.SOUTH;
      }

      double dxMin = Math.abs(hit.x - box.minX);
      double dxMax = Math.abs(hit.x - box.maxX);
      double dyMin = Math.abs(hit.y - box.minY);
      double dyMax = Math.abs(hit.y - box.maxY);
      double dzMin = Math.abs(hit.z - box.minZ);
      double dzMax = Math.abs(hit.z - box.maxZ);

      double m = dxMin;
      Direction d = Direction.WEST;
      if (dxMax < m) {
         m = dxMax;
         d = Direction.EAST;
      }
      if (dyMin < m) {
         m = dyMin;
         d = Direction.DOWN;
      }
      if (dyMax < m) {
         m = dyMax;
         d = Direction.UP;
      }
      if (dzMin < m) {
         m = dzMin;
         d = Direction.NORTH;
      }
      if (dzMax < m) {
         m = dzMax;
         d = Direction.SOUTH;
      }

      return d;
   }

   public static BlockHitResult getFacedContainerBlock(float yaw, float pitch) {
      if (mc.player == null || mc.level == null) {
         return null;
      }

      double reachDistance = mc.player.blockInteractionRange();
      Vec3 startPos = eye();
      Vec3 direction = calculateViewVector(pitch, yaw);
      Vec3 endPos = startPos.add(direction.x * reachDistance, direction.y * reachDistance, direction.z * reachDistance);
      if (direction.x == 0.0) {
         direction = new Vec3(EPSILON, direction.y, direction.z);
      }
      if (direction.y == 0.0) {
         direction = new Vec3(direction.x, EPSILON, direction.z);
      }
      if (direction.z == 0.0) {
         direction = new Vec3(direction.x, direction.y, EPSILON);
      }

      BlockPos currentPos = BlockPos.containing(startPos.x, startPos.y, startPos.z);
      int stepX = (int)Math.signum(direction.x);
      int stepY = (int)Math.signum(direction.y);
      int stepZ = (int)Math.signum(direction.z);

      double nextBoundaryX = stepX > 0 ? (double)(currentPos.getX() + 1) : (double)currentPos.getX();
      double nextBoundaryY = stepY > 0 ? (double)(currentPos.getY() + 1) : (double)currentPos.getY();
      double nextBoundaryZ = stepZ > 0 ? (double)(currentPos.getZ() + 1) : (double)currentPos.getZ();

      double tMaxX = (nextBoundaryX - startPos.x) / direction.x;
      double tMaxY = (nextBoundaryY - startPos.y) / direction.y;
      double tMaxZ = (nextBoundaryZ - startPos.z) / direction.z;

      double tDeltaX = (double)stepX / direction.x;
      double tDeltaY = (double)stepY / direction.y;
      double tDeltaZ = (double)stepZ / direction.z;

      while (startPos.distanceTo(Vec3.atCenterOf(currentPos)) <= reachDistance) {
         if (!mc.level.isEmptyBlock(currentPos) && isContainerBlock(mc.level.getBlockState(currentPos))) {
            AABB currentBox = new AABB(
               (double)currentPos.getX(),
               (double)currentPos.getY(),
               (double)currentPos.getZ(),
               (double)currentPos.getX() + 1.0,
               (double)currentPos.getY() + 1.0,
               (double)currentPos.getZ() + 1.0
            );
            Optional<Vec3> intercept = currentBox.clip(startPos, endPos);
            if (intercept.isPresent()) {
               Vec3 hitVec = intercept.get();
               Direction side = getHitFace(hitVec, currentBox);
               boolean isInside = currentBox.contains(startPos);
               return new BlockHitResult(hitVec, side, currentPos, isInside);
            }
         }

         if (tMaxX < tMaxY) {
            if (tMaxX < tMaxZ) {
               currentPos = currentPos.offset(stepX, 0, 0);
               tMaxX += tDeltaX;
            } else {
               currentPos = currentPos.offset(0, 0, stepZ);
               tMaxZ += tDeltaZ;
            }
         } else if (tMaxY < tMaxZ) {
            currentPos = currentPos.offset(0, stepY, 0);
            tMaxY += tDeltaY;
         } else {
            currentPos = currentPos.offset(0, 0, stepZ);
            tMaxZ += tDeltaZ;
         }
      }

      return null;
   }

   private static boolean isContainerBlock(BlockState state) {
      return state.getBlock() instanceof ChestBlock
         || state.getBlock() instanceof EnderChestBlock
         || state.getBlock() instanceof ShulkerBoxBlock;
   }

   public static double getDistance(float yaw, float pitch, Entity target) {
      Vec3 dir = calculateViewVector(pitch, yaw).normalize();
      AABB targetBox = target.getBoundingBox();
      return intersectRayAabb(eye(), dir, targetBox);
   }

   public static boolean didHitEntity(float yaw, float pitch, double range, Entity target) {
      return overBox(yaw, pitch, range, target.getBoundingBox());
   }

   public static boolean overBox(float yawDeg, float pitchDeg, double range, AABB box) {
      if (box == null || mc.level == null) {
         return false;
      }

      Vec3 start = eye();
      Vec3 dir = calculateViewVector(pitchDeg, yawDeg).normalize();
      if (dir.lengthSqr() < 1.0E-12) {
         return false;
      }

      double tBox = intersectRayAabb(start, dir, box);
      if (!Double.isFinite(tBox) || tBox < 0.0 || tBox > range) {
         return false;
      }

      return !isOccludedBefore(mc.level, start, dir, tBox, range, mc.player);
   }

   private static boolean isOccludedBefore(Level world, Vec3 origin, Vec3 dir, double tStop, double range, Entity viewer) {
      int x = Mth.floor(origin.x);
      int y = Mth.floor(origin.y);
      int z = Mth.floor(origin.z);

      int stepX = dir.x > 0.0 ? 1 : (dir.x < 0.0 ? -1 : 0);
      int stepY = dir.y > 0.0 ? 1 : (dir.y < 0.0 ? -1 : 0);
      int stepZ = dir.z > 0.0 ? 1 : (dir.z < 0.0 ? -1 : 0);

      double tMaxX = nextBoundaryT(origin.x, dir.x, x, stepX);
      double tMaxY = nextBoundaryT(origin.y, dir.y, y, stepY);
      double tMaxZ = nextBoundaryT(origin.z, dir.z, z, stepZ);

      double tDeltaX = stepX != 0 ? 1.0 / Math.abs(dir.x) : Double.POSITIVE_INFINITY;
      double tDeltaY = stepY != 0 ? 1.0 / Math.abs(dir.y) : Double.POSITIVE_INFINITY;
      double tDeltaZ = stepZ != 0 ? 1.0 / Math.abs(dir.z) : Double.POSITIVE_INFINITY;
      double tLimit = Math.min(tStop, range);
      if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
         return true;
      }

      while (true) {
         if (tMaxX < tMaxY) {
            if (tMaxX < tMaxZ) {
               if (tMaxX > tLimit) {
                  break;
               }
               x += stepX;
               if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                  return true;
               }
               tMaxX += tDeltaX;
            } else {
               if (tMaxZ > tLimit) {
                  break;
               }
               z += stepZ;
               if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
                  return true;
               }
               tMaxZ += tDeltaZ;
            }
         } else if (tMaxY < tMaxZ) {
            if (tMaxY > tLimit) {
               break;
            }
            y += stepY;
            if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
               return true;
            }
            tMaxY += tDeltaY;
         } else {
            if (tMaxZ > tLimit) {
               break;
            }
            z += stepZ;
            if (blockOccludesHere(world, origin, dir, new BlockPos(x, y, z), tStop, viewer)) {
               return true;
            }
            tMaxZ += tDeltaZ;
         }
      }

      return false;
   }

   private static double nextBoundaryT(double o, double d, int cell, int step) {
      if (step == 0) {
         return Double.POSITIVE_INFINITY;
      }

      double boundary = step > 0 ? (double)(cell + 1) : (double)cell;
      return (boundary - o) / d;
   }

   private static boolean blockOccludesHere(Level world, Vec3 origin, Vec3 dir, BlockPos pos, double tEntity, Entity viewer) {
      BlockState state = world.getBlockState(pos);
      if (state.isAir()) {
         return false;
      }

      VoxelShape shape = state.getShape(world, pos, viewer != null ? CollisionContext.of(viewer) : CollisionContext.empty());
      if (shape.isEmpty()) {
         return false;
      }

      for (AABB local : shape.toAabbs()) {
         AABB box = local.move(pos.getX(), pos.getY(), pos.getZ());
         double tBox = intersectRayAabb(origin, dir, box);
         if (Double.isFinite(tBox) && tBox >= 0.0 && tBox < tEntity) {
            return true;
         }
      }

      return false;
   }

   public static double intersectRayAabb(Vec3 o, Vec3 d, AABB b) {
      double tMin = 0.0;
      double tMax = Double.POSITIVE_INFINITY;

      if (!axisSlab(o.x, d.x, b.minX, b.maxX, Holder.tmp)) {
         return Double.POSITIVE_INFINITY;
      }
      tMin = Math.max(tMin, Holder.tmp[0]);
      tMax = Math.min(tMax, Holder.tmp[1]);
      if (tMax < tMin) {
         return Double.POSITIVE_INFINITY;
      }

      if (!axisSlab(o.y, d.y, b.minY, b.maxY, Holder.tmp)) {
         return Double.POSITIVE_INFINITY;
      }
      tMin = Math.max(tMin, Holder.tmp[0]);
      tMax = Math.min(tMax, Holder.tmp[1]);
      if (tMax < tMin) {
         return Double.POSITIVE_INFINITY;
      }

      if (!axisSlab(o.z, d.z, b.minZ, b.maxZ, Holder.tmp)) {
         return Double.POSITIVE_INFINITY;
      }
      tMin = Math.max(tMin, Holder.tmp[0]);
      tMax = Math.min(tMax, Holder.tmp[1]);
      if (tMax < tMin) {
         return Double.POSITIVE_INFINITY;
      }

      return tMin;
   }

   private static boolean axisSlab(double o, double d, double min, double max, double[] out) {
      if (Math.abs(d) < 1.0E-12) {
         if (o < min || o > max) {
            return false;
         }
         out[0] = Double.NEGATIVE_INFINITY;
         out[1] = Double.POSITIVE_INFINITY;
         return true;
      }

      double inv = 1.0 / d;
      double t1 = (min - o) * inv;
      double t2 = (max - o) * inv;
      if (t1 > t2) {
         double tmp = t1;
         t1 = t2;
         t2 = tmp;
      }

      out[0] = t1;
      out[1] = t2;
      return true;
   }

   private static final class Holder {
      static final double[] tmp = new double[2];
   }
}
