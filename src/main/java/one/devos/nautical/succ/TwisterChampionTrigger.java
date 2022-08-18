package one.devos.nautical.succ;

import com.google.gson.JsonObject;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate.Composite;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import one.devos.nautical.succ.TwisterChampionTrigger.Instance;

public class TwisterChampionTrigger extends SimpleCriterionTrigger<Instance> {
	public static final ResourceLocation ID = Succ.id("twister_champion");

	@Override
	protected Instance createInstance(JsonObject obj, Composite playerPredicate, DeserializationContext predicateDeserializer) {
		return new Instance();
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	public void trigger(ServerPlayer player) {
		super.trigger(player, c -> true);
	}

	public static class Instance extends AbstractCriterionTriggerInstance {
		public Instance() {
			super(ID, Composite.ANY);
		}
	}
}
