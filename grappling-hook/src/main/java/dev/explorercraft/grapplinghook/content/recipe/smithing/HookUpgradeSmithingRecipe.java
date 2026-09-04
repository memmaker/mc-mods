package dev.explorercraft.grapplinghook.content.recipe.smithing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.CustomizationProperty;
import dev.explorercraft.grapplinghook.content.item.GrapplehookItem;
import dev.explorercraft.grapplinghook.content.registry.internal.ModItems;
import dev.explorercraft.grapplinghook.content.registry.internal.ModRecipeSerializers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 26.2 turned SmithingTransformRecipe's result into an ItemStackTemplate and made RecipeSerializer
 * a record, so this extends SimpleSmithingRecipe directly rather than the transform recipe: the
 * result here is computed from the input hook's existing customizations, not a fixed stack.
 */
public class HookUpgradeSmithingRecipe extends SimpleSmithingRecipe {

    private final Ingredient template;
    private final Ingredient addition;
    private final List<CustomizationProperty<?>> applies;
    private final List<CustomizationProperty<?>> excludesIfAny;

    public HookUpgradeSmithingRecipe(Ingredient template, Ingredient addition,
                                     List<CustomizationProperty<?>> applies,
                                     List<CustomizationProperty<?>> excludesIfAny) {
        super(new Recipe.CommonInfo(false));
        this.template = template;
        this.addition = addition;
        this.applies = List.copyOf(applies);
        this.excludesIfAny = List.copyOf(excludesIfAny);
    }

    /** What the recipe book shows as the outcome: a stock hook with this upgrade already applied. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ItemStack buildPreviewResult() {
        GrapplehookItem hook = ModItems.GRAPPLING_HOOK.get();
        ItemStack preview = hook.getDefaultInstance();
        HookCustomization custom = new HookCustomization();
        for (CustomizationProperty<?> property : this.applies) {
            custom.set((CustomizationProperty) property, Boolean.TRUE);
        }
        hook.applyCustomizations(preview, custom);
        return preview;
    }

    public Ingredient templateIngredientRaw() { return template; }
    public Ingredient additionIngredientRaw() { return addition; }
    public List<CustomizationProperty<?>> applies() { return applies; }
    public List<CustomizationProperty<?>> excludesIfAny() { return excludesIfAny; }

    @Override
    public Optional<Ingredient> templateIngredient() { return Optional.of(template); }

    @Override
    public Ingredient baseIngredient() { return Ingredient.of(ModItems.GRAPPLING_HOOK.get()); }

    @Override
    public Optional<Ingredient> additionIngredient() { return Optional.of(addition); }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!this.template.test(input.template())) return false;
        if (!input.base().is(ModItems.GRAPPLING_HOOK.get())) return false;
        if (!this.addition.test(input.addition())) return false;

        GrapplehookItem hook = ModItems.GRAPPLING_HOOK.get();
        HookCustomization custom = hook.getCustomizationsOrDefault(input.base());
        for (CustomizationProperty<?> blocker : this.excludesIfAny) {
            if (Boolean.TRUE.equals(custom.get(blocker))) return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack base = input.base().copyWithCount(1);
        GrapplehookItem hook = ModItems.GRAPPLING_HOOK.get();
        HookCustomization custom = HookCustomization.copyAllFrom(hook.getCustomizationsOrDefault(base));
        for (CustomizationProperty<?> property : this.applies) {
            custom.set((CustomizationProperty) property, Boolean.TRUE);
        }
        hook.applyCustomizations(base, custom);
        return base;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(this.template, this.baseIngredient(), this.addition));
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new SmithingRecipeDisplay(
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(this.template.items().iterator().next().value().getDefaultInstance())),
                Ingredient.optionalIngredientToDisplay(Optional.of(this.baseIngredient())),
                Ingredient.optionalIngredientToDisplay(Optional.of(this.addition)),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(this.buildPreviewResult())),
                new SlotDisplay.ItemSlotDisplay(net.minecraft.world.item.Items.SMITHING_TABLE)
        ));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.SMITHING; }

    @Override
    public RecipeSerializer<HookUpgradeSmithingRecipe> getSerializer() {
        return ModRecipeSerializers.HOOK_UPGRADE;
    }

    private static final Codec<List<CustomizationProperty<?>>> PROPERTY_LIST_CODEC = CustomizationProperty.KEY_CODEC.listOf();

    public static final MapCodec<HookUpgradeSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("template").forGetter(HookUpgradeSmithingRecipe::templateIngredientRaw),
            Ingredient.CODEC.fieldOf("addition").forGetter(HookUpgradeSmithingRecipe::additionIngredientRaw),
            PROPERTY_LIST_CODEC.fieldOf("applies").forGetter(HookUpgradeSmithingRecipe::applies),
            PROPERTY_LIST_CODEC.optionalFieldOf("excludes_if_any", List.of()).forGetter(HookUpgradeSmithingRecipe::excludesIfAny)
    ).apply(inst, HookUpgradeSmithingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<CustomizationProperty<?>>> PROPERTY_LIST_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(PROPERTY_LIST_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, HookUpgradeSmithingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, HookUpgradeSmithingRecipe::templateIngredientRaw,
                    Ingredient.CONTENTS_STREAM_CODEC, HookUpgradeSmithingRecipe::additionIngredientRaw,
                    PROPERTY_LIST_STREAM_CODEC, HookUpgradeSmithingRecipe::applies,
                    PROPERTY_LIST_STREAM_CODEC, HookUpgradeSmithingRecipe::excludesIfAny,
                    HookUpgradeSmithingRecipe::new
            );
}
