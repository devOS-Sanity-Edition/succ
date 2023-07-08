package one.devos.nautical.succ;

import com.google.gson.JsonObject;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import one.devos.nautical.succ.TwisterChampionTrigger.Instance;

import org.jetbrains.annotations.NotNull;

public class TwisterChampionTrigger extends SimpleCriterionTrigger<Instance> {
	public static final ResourceLocation ID = Succ.id("twister_champion");

	@Override
	@NotNull
	protected Instance createInstance(JsonObject json, ContextAwarePredicate contextAwarePredicate, DeserializationContext deserializationContext) {
		return new Instance();
	}

	@Override
	@NotNull
	public ResourceLocation getId() {
		return ID;
	}

	public void trigger(ServerPlayer player) {
		super.trigger(player, c -> true);
	}

	public static class Instance extends AbstractCriterionTriggerInstance {
		public Instance() {
			super(ID, ContextAwarePredicate.ANY);
		}
	}
}
