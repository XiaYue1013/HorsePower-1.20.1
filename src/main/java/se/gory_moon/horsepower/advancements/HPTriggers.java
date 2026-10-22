package se.gory_moon.horsepower.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import se.gory_moon.horsepower.HorsePower;

/**
 * Port of the 1.12.2 advancement {@code Manager} and its three triggers (attaching a worker to a machine).
 */
public final class HPTriggers {

    public static final SimpleTrigger USE_GRINDSTONE = new SimpleTrigger(new ResourceLocation(HorsePower.MODID, "use_horse_grindstone"));
    public static final SimpleTrigger USE_CHOPPER = new SimpleTrigger(new ResourceLocation(HorsePower.MODID, "use_chopping_block"));
    public static final SimpleTrigger USE_PRESS = new SimpleTrigger(new ResourceLocation(HorsePower.MODID, "use_horse_press"));

    private HPTriggers() {}

    public static void register() {
        CriteriaTriggers.register(USE_GRINDSTONE);
        CriteriaTriggers.register(USE_CHOPPER);
        CriteriaTriggers.register(USE_PRESS);
    }

    public static class SimpleTrigger extends SimpleCriterionTrigger<SimpleTrigger.Instance> {
        private final ResourceLocation id;

        public SimpleTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
            return new Instance(id, player);
        }

        public void trigger(ServerPlayer player) {
            trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ResourceLocation id, ContextAwarePredicate player) {
                super(id, player);
            }
        }
    }
}
