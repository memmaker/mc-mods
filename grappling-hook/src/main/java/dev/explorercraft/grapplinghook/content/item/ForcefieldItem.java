package dev.explorercraft.grapplinghook.content.item;

import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.client.physics.controller.GrapplingHookPhysicsController;
import dev.explorercraft.grapplinghook.content.physics.PhysicsControllers;
import dev.explorercraft.grapplinghook.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ForcefieldItem extends Item {

	public ForcefieldItem(Item.Properties properties) {
		super(properties.stacksTo(1));
	}


	@NotNull
    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand hand) {
    	ItemStack stack = playerIn.getItemInHand(hand);

		if(!worldIn.isClientSide())
			return InteractionResult.CONSUME;

		int playerId = playerIn.getId();
		GrapplingHookPhysicsController oldController = GrappleModClient.get()
				.getClientControllerManager()
				.getController(playerId);

		if (oldController == null || oldController.getType() == PhysicsControllers.AIR_FRICTION) {
			GrappleModClient.get()
					.getClientControllerManager()
					.createControl(PhysicsControllers.FORCEFIELD, -1, playerId, worldIn, null, null);
		} else {
			oldController.disable();
		}
        
    	return InteractionResult.SUCCESS;
	}
    
	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                            Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		// 26.2 hands tooltips to a Consumer; collect into a list so the body below is unchanged.
		List<Component> tooltipComponents = new java.util.ArrayList<>();
		try {
		Options options = Minecraft.getInstance().options;

		tooltipComponents.add(Component.translatable("grappletooltip.repelleritem.desc")
				  .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)
		);
		tooltipComponents.add(Component.literal(""));

		tooltipComponents.add(Component.translatable("grappletooltip.controls.title")
				  .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
		);

		tooltipComponents.add(TextUtils.keybinding("grappletooltip.repelleritemon.desc", options.keyUse));
		tooltipComponents.add(TextUtils.keybinding("grappletooltip.repelleritemoff.desc", options.keyUse));
		tooltipComponents.add(TextUtils.keybinding("grappletooltip.repelleritemslow.desc", options.keyShift));
		tooltipComponents.add(TextUtils.keybinding("grappletooltip.repelleritemmove.desc",
				options.keyUp, options.keyLeft, options.keyDown, options.keyRight)
		);
	
		} finally {
			tooltipComponents.forEach(tooltipAdder);
		}
	}
}
