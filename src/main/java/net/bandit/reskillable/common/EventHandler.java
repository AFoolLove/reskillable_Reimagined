package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {
    private static final Map<UUID, SkillModel> lastDiedPlayerSkillsMap = new ConcurrentHashMap<>();


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative() && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack item = event.getItem();
        if (!model.canUseItem(player, item)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You lack the skill to use this item.").withStyle(ChatFormatting.RED));
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative() && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();

        if (!player.isCreative() && !model.canUseItem(player, item)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Entity entity = event.getTarget();
        ItemStack item = event.getItemStack();

        if (!player.isCreative() && (!model.canUseEntity(player, entity) || !model.canUseItem(player, item))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean canUseMain = model.canUseItem(player, mainHand);
        boolean canUseOff = model.canUseItem(player, offHand);

        if (!canUseMain || !canUseOff || !model.canAttackEntity(player, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeEquipment(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack newItem = event.getTo();
            ItemStack oldItem = event.getFrom();

            if (!model.canUseItem(player, newItem)) {
                player.setItemSlot(event.getSlot(), oldItem);

                player.drop(newItem.copy(), false);

                player.sendSystemMessage(Component.literal("You lack the skill to equip this armor.").withStyle(ChatFormatting.RED));
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDrops(LivingDropsEvent event) {
        if (Configuration.getDisableWool() && event.getEntity() instanceof Sheep) {
            event.getDrops().removeIf(item -> item.getItem().is(ItemTags.WOOL));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            if (Configuration.getDeathReset() && !lastDiedPlayerSkillsMap.containsKey(player.getUUID())) {
                model.resetSkills();
            }
            lastDiedPlayerSkillsMap.put(player.getUUID(), model);

        }
    }


    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        SkillModel oldModel = SkillModel.get(event.getOriginal());
        SkillModel newModel = SkillModel.get(event.getEntity());

        if (oldModel != null && newModel != null) {
            newModel.cloneFrom(oldModel);
        }
    }


    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model != null) {
            SyncToClient.send(player);
            SyncSkillConfig.sendToClient(player, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack item = event.getItem();
        if (!model.canUseItem(player, item)) {
            player.sendSystemMessage(Component.literal("You lack the skill to use this item.").withStyle(ChatFormatting.RED));
            event.setCanceled(true);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        if (!model.canUseItem(player, tool)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You lack the skill to use this tool.").withStyle(ChatFormatting.RED));
        }
    }


    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SkillModel model = SkillModel.get(serverPlayer);
            if (model != null) {
                SyncToClient.send(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SkillModel model = SkillModel.get(serverPlayer);
            if (model != null) {
                model.syncSkills(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public void onUseTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack totem = event.getTotem();
        if (!totem.is(Items.TOTEM_OF_UNDYING)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Requirement[] reqs = Configuration.getRequirements(Items.TOTEM_OF_UNDYING.builtInRegistryHolder().key().location());
        if (reqs == null) {
            return;
        }
        for (Requirement req : reqs) {
            if (model.getSkillLevel(req.skill) < req.level) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("You lack the skill to use the Totem of Undying.").withStyle(ChatFormatting.RED));
                return;
            }
        }
    }
}