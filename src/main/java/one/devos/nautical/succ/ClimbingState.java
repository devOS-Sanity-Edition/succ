package one.devos.nautical.succ;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;

import org.quiltmc.qsl.networking.api.PacketByteBufs;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClimbingState {
	public final UUID playerUuid;
	@Nullable // non-null when climbing
	public Direction facing;
	public final Map<SuctionCupLimb, ClimbingSuctionCupEntity> entities = new HashMap<>();

	public ClimbingState(UUID uuid) {
		this.playerUuid = uuid;
	}

	public ClimbingState(FriendlyByteBuf buf) {
		this.playerUuid = buf.readUUID();
		int ordinal = buf.readVarInt();
		this.facing = ordinal == -1 ? null : Direction.values()[ordinal];
		// entities are added to their states as they load on the client, see ClimbingSuctionCupEntity.readExtraPacketData()
	}

	public boolean isClimbing() {
		return facing != null;
	}

	public FriendlyByteBuf syncAllToNetwork() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeEnum(StateChangeType.ADD_OR_REPLACE);
		writeClimbStatus(buf);
		writeCupEntities(buf);
		return buf;
	}

	public FriendlyByteBuf syncClimbStatusToNetwork() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeEnum(StateChangeType.CLIMB_STATUS);
		writeClimbStatus(buf);
		return buf;
	}

	public void writeClimbStatus(FriendlyByteBuf buf) {
		buf.writeUUID(this.playerUuid);
		buf.writeVarInt(this.facing == null ? -1 : this.facing.ordinal());
	}

	public void writeCupEntities(FriendlyByteBuf buf) {
		entities.forEach((limb, entity) -> {
			buf.writeEnum(limb);
			buf.writeVarInt(entity.getId());
		});
	}
}
