package net.bandit.reskillable.common;

import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class TaczEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGunFireEvent(GunFireEvent event) {
        if (event.getShooter() instanceof Player player) {
            if (player.isCreative()) {
                return;
            }
            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            ItemStack stack = event.getGunItemStack();
            if (stack.getItem() instanceof GunItemDataAccessor item) {
                ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
                // TACZ:    tacz:<gun type>__<gunid>
                // eg:      tacz:modern_kinetic_gun__bf1_tg1918
                itemKey = ResourceLocation.fromNamespaceAndPath("tacz",
                        "%s__%s".formatted(
                                itemKey.getPath(),
                                item.getGunId(stack).getPath().replaceAll(":", "_")));
                if (!checkRequirements(model, player, itemKey)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
