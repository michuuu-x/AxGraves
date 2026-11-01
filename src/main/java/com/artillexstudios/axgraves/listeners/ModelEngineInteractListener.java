package com.artillexstudios.axgraves.listeners;

import com.artillexstudios.axapi.packet.wrapper.serverbound.ServerboundInteractWrapper;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// only ever registered when ModelEngine is present, see AxGraves#enable
public class ModelEngineInteractListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(@NotNull BaseEntityInteractEvent event) {
        if (event.getAction() == BaseEntityInteractEvent.Action.ATTACK) return;

        ServerboundInteractWrapper.InteractionHand hand = switch (event.getSlot()) {
            case HAND -> ServerboundInteractWrapper.InteractionHand.MAIN_HAND;
            case OFF_HAND -> ServerboundInteractWrapper.InteractionHand.OFF_HAND;
            default -> null;
        };
        if (hand == null) return;

        UUID uuid = event.getBaseEntity().getUUID();

        for (Grave grave : SpawnedGraves.getGraves()) {
            if (grave.getModelEngineAnchor() == null) continue;
            if (!grave.getModelEngineAnchor().getUniqueId().equals(uuid)) continue;

            grave.interact(event.getPlayer(), hand);
            return;
        }
    }
}
