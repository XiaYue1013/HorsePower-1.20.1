package se.gory_moon.horsepower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of {@code Utils.getCreatureClasses()}: which mobs may power the horse blocks, plus the "claim" marker that
 * guarantees one mob only ever works for one machine.
 */
public final class Workers {

    public static final TagKey<EntityType<?>> WORKERS_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(HorsePower.MODID, "workers"));
    private static final String CLAIM_TAG = HorsePower.MODID + ":machine";

    private static List<? extends String> cachedList = null;
    private static Set<EntityType<?>> cachedTypes = Set.of();

    private Workers() {}

    private static Set<EntityType<?>> configuredTypes() {
        List<? extends String> list = Configs.general.grindstoneMobList.get();
        if (list != cachedList) {
            Set<EntityType<?>> types = new HashSet<>();
            for (String entry : list) {
                ResourceLocation id = ResourceLocation.tryParse(entry.trim());
                EntityType<?> type = id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
                if (type == null)
                    HorsePower.LOGGER.error("Error in config, could not find ({}) mob, mod for entity might not be installed", entry);
                else
                    types.add(type);
            }
            cachedTypes = types;
            cachedList = list;
        }
        return cachedTypes;
    }

    public static boolean isValidWorker(Entity entity) {
        if (!(entity instanceof PathfinderMob mob) || !mob.isAlive())
            return false;
        if (Configs.general.useHorseInterface.get() && entity instanceof AbstractHorse)
            return true;
        EntityType<?> type = entity.getType();
        return type.is(WORKERS_TAG) || configuredTypes().contains(type);
    }

    public static void claim(PathfinderMob mob, Level level, BlockPos machine) {
        CompoundTag tag = new CompoundTag();
        tag.put("pos", NbtUtils.writeBlockPos(machine));
        tag.putString("dim", level.dimension().location().toString());
        mob.getPersistentData().put(CLAIM_TAG, tag);
    }

    public static boolean isClaimedBy(PathfinderMob mob, Level level, BlockPos machine) {
        CompoundTag tag = getClaim(mob);
        return tag != null && NbtUtils.readBlockPos(tag.getCompound("pos")).equals(machine) &&
                level.dimension().location().toString().equals(tag.getString("dim"));
    }

    public static void unclaim(PathfinderMob mob, Level level, BlockPos machine) {
        if (isClaimedBy(mob, level, machine))
            mob.getPersistentData().remove(CLAIM_TAG);
    }

    public static boolean hasClaim(PathfinderMob mob) {
        return getClaim(mob) != null;
    }

    @Nullable
    private static CompoundTag getClaim(PathfinderMob mob) {
        CompoundTag data = mob.getPersistentData();
        return data.contains(CLAIM_TAG, 10) ? data.getCompound(CLAIM_TAG) : null;
    }
}
