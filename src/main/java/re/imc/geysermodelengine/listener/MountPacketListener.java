package re.imc.geysermodelengine.listener;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;

import io.rivrs.bedrockcore.api.BedrockAPI;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.utils.Pair;

public class MountPacketListener extends SimplePacketListenerAbstract {

    private final GeyserModelEngine plugin;

    public MountPacketListener(GeyserModelEngine plugin) {
        super(PacketListenerPriority.MONITOR);
        this.plugin = plugin;
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || !BedrockAPI.isBedrockPlayer(player))
            return;

        // Steer Vehicle
        if (event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            Pair<ActiveModel, Mount> seat = this.plugin.getDrivers().get(player);
            if (seat == null)
                return;

            WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);
            float pitch = player.getPitch();
            if (seat.first().getModeledEntity().getBase().isFlying()) {
                if (pitch < -30)
                    packet.setJump(true);
                else if (pitch > 45)
                    packet.setUnmount(true);
                return;
            }

            if (player.getInventory().getHeldItemSlot() == 0) {
                packet.setUnmount(true);
                player.getInventory().setHeldItemSlot(3);
            } else if (pitch > 89 || player.getInventory().getHeldItemSlot() == 1) {
                packet.setUnmount(true);
            } else if (player.getInventory().getHeldItemSlot() == 8) {
                packet.setUnmount(true);
            }

            event.markForReEncode(true);
            return;
        }

        // Player Action
        if (event.getPacketType().equals(PacketType.Play.Client.ENTITY_ACTION)) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getEntityId() != player.getEntityId() || !packet.getAction().equals(WrapperPlayClientEntityAction.Action.START_SNEAKING))
                return;

            Pair<ActiveModel, Mount> seat = this.plugin.getDrivers().get(player);
            if (seat == null)
                return;

            player.sendActionBar("action.hint.exit.vehicle");
            ModelEngineAPI.getMountPairManager().tryDismount(player);
        }
    }
}