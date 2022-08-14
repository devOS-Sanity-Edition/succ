package one.devos.nautical.succ;

import net.minecraft.network.FriendlyByteBuf;

import org.quiltmc.qsl.networking.api.PacketByteBufs;

import java.util.UUID;

public class ClimbingState {
	public final UUID playerUuid;
	public boolean climbing;

	public ClimbingState(UUID uuid) {
		this.playerUuid = uuid;
	}

	public ClimbingState(FriendlyByteBuf buf) {
		this.playerUuid = buf.readUUID();
		this.climbing = buf.readBoolean();
	}

	public FriendlyByteBuf toNetwork() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(this.playerUuid);
		buf.writeBoolean(this.climbing);
		return buf;
	}
}
