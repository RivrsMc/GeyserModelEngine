package re.imc.geysermodelengine.listener.packets;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.type.Mount;

import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.utils.Pair;

public class MountPacketListener extends SimplePacketListenerAbstract {

    private final GeyserModelEngine plugin;

    public MountPacketListener(GeyserModelEngine plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        final Player player = (Player) event.getPlayer();
        if ((!event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE) && !event.getPacketType().equals(PacketType.Play.Client.ENTITY_ACTION))
            || !FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;

        if (event.getPacketType().equals(PacketType.Play.Client.STEER_VEHICLE)) {
            WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

            Pair<ActiveModel, Mount> seat = this.plugin.getDrivers().get(player);
            if (seat == null)
                return;

            float pitch = player.getPitch();
            if (seat.first().getModeledEntity().getBase().isFlying()) {
                if (pitch < -30)
                    packet.setJump(true);
                else if (pitch > 45)
                    packet.setJump(false);
            } else {
                if (player.getInventory().getHeldItemSlot() == 0) {
                    packet.setJump(true);
                    player.getInventory().setHeldItemSlot(3);
                }
                if (pitch > 89 || player.getInventory().getHeldItemSlot() == 1)
                    packet.setUnmount(true);
                if (player.getInventory().getHeldItemSlot() == 8)
                    packet.setJump(true);
            }
        } else if (event.getPacketType().equals(PacketType.Play.Client.ENTITY_ACTION)) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            Pair<ActiveModel, Mount> seat = this.plugin.getDrivers().get(player);
            if (seat == null)
                return;

            if (packet.getAction().equals(WrapperPlayClientEntityAction.Action.START_SNEAKING)) {
                player.sendActionBar("action.hint.exit.vehicle");
                ModelEngineAPI.getMountPairManager().tryDismount(player);
            }
        }
    }
}
