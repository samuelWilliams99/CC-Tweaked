// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.data.recipe.ShapedSpecBuilder;
import dan200.computercraft.data.recipe.ShapelessSpecBuilder;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.ClearColourRecipe;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.recipe.ComputerUpgradeRecipe;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RecipeIngredients;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.recipe.CustomShapelessRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapedRecipe;
import dan200.computercraft.shared.recipe.ImpostorShapelessRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dan200.computercraft.shared.turtle.recipes.TurtleOverlayRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleRecipe;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.ColourUtils;
import net.minecraft.Util;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

final class RecipeProvider {
    private final RecipeIngredients ingredients = PlatformHelper.get().getRecipeIngredients();
    private final TurtleUpgradeDataProvider turtleUpgrades;
    private final PocketUpgradeDataProvider pocketUpgrades;

    private RecipeProvider(TurtleUpgradeDataProvider turtleUpgrades, PocketUpgradeDataProvider pocketUpgrades) {
        this.turtleUpgrades = turtleUpgrades;
        this.pocketUpgrades = pocketUpgrades;
    }

    public static void recipes(RecipeOutput output, TurtleUpgradeDataProvider turtleUpgrades, PocketUpgradeDataProvider pocketUpgrades) {
        new RecipeProvider(turtleUpgrades, pocketUpgrades).build(output);
    }

    private void build(RecipeOutput add) {
        basicRecipes(add);
        diskColours(add);
        pocketUpgrades(add);
        turtleUpgrades(add);
        turtleOverlays(add);

        addSpecial(add, new PrintoutRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new DiskRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new ColourableRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new ClearColourRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new TurtleUpgradeRecipe(CraftingBookCategory.MISC));
        addSpecial(add, new PocketComputerUpgradeRecipe(CraftingBookCategory.MISC));
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param output The callback to add recipes.
     */
    private void diskColours(RecipeOutput output) {
        for (var colour : Colour.VALUES) {
            ShapelessSpecBuilder
                .shapeless(RecipeCategory.REDSTONE, DiskItem.createFromIDAndColour(-1, null, colour.getHex()))
                .requires(ingredients.redstone())
                .requires(Items.PAPER)
                .requires(DyeItem.byColor(ofColour(colour)))
                .group("computercraft:disk")
                .unlockedBy("has_drive", inventoryChange(ModRegistry.Blocks.DISK_DRIVE.get()))
                .build(ImpostorShapelessRecipe::new)
                .save(output, new ResourceLocation(ComputerCraftAPI.MOD_ID, "disk_" + (colour.ordinal() + 1)));
        }
    }

    private static List<TurtleItem> turtleItems() {
        return List.of(ModRegistry.Items.TURTLE_NORMAL.get(), ModRegistry.Items.TURTLE_ADVANCED.get());
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades(RecipeOutput add) {
        for (var turtleItem : turtleItems()) {
            var base = turtleItem.create(-1, null, -1, null, null, 0, null);

            var nameId = turtleItem.getFamily().name().toLowerCase(Locale.ROOT);

            for (var upgrade : turtleUpgrades.getGeneratedUpgrades()) {
                ShapedSpecBuilder
                    .shaped(RecipeCategory.REDSTONE, turtleItem.create(-1, null, -1, null, UpgradeData.ofDefault(upgrade), -1, null))
                    .group(String.format("%s:turtle_%s", ComputerCraftAPI.MOD_ID, nameId))
                    .pattern("#T")
                    .define('T', base.getItem())
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", inventoryChange(base.getItem(), upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        add,
                        new ResourceLocation(ComputerCraftAPI.MOD_ID, String.format("turtle_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ))
                    );
            }
        }
    }

    private static List<PocketComputerItem> pocketComputerItems() {
        return List.of(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void pocketUpgrades(RecipeOutput add) {
        for (var pocket : pocketComputerItems()) {
            var base = pocket.create(-1, null, -1, null);
            if (base.isEmpty()) continue;

            var nameId = pocket.getFamily().name().toLowerCase(Locale.ROOT);

            for (var upgrade : pocketUpgrades.getGeneratedUpgrades()) {
                ShapedSpecBuilder
                    .shaped(RecipeCategory.REDSTONE, pocket.create(-1, null, -1, UpgradeData.ofDefault(upgrade)))
                    .group(String.format("%s:pocket_%s", ComputerCraftAPI.MOD_ID, nameId))
                    .pattern("#")
                    .pattern("P")
                    .define('P', base.getItem())
                    .define('#', upgrade.getCraftingItem().getItem())
                    .unlockedBy("has_items", inventoryChange(base.getItem(), upgrade.getCraftingItem().getItem()))
                    .build(ImpostorShapedRecipe::new)
                    .save(
                        add,
                        new ResourceLocation(ComputerCraftAPI.MOD_ID, String.format("pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ))
                    );
            }
        }
    }

    private void turtleOverlays(RecipeOutput add) {
        turtleOverlay(add, "turtle_trans_overlay", x -> x
            .unlockedBy("has_dye", inventoryChange(itemPredicate(ingredients.dye())))
            .requires(ColourUtils.getDyeTag(DyeColor.LIGHT_BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PINK))
            .requires(ColourUtils.getDyeTag(DyeColor.WHITE))
            .requires(Items.STICK)
        );

        turtleOverlay(add, "turtle_rainbow_overlay", x -> x
            .unlockedBy("has_dye", inventoryChange(itemPredicate(ingredients.dye())))
            .requires(ColourUtils.getDyeTag(DyeColor.RED))
            .requires(ColourUtils.getDyeTag(DyeColor.ORANGE))
            .requires(ColourUtils.getDyeTag(DyeColor.YELLOW))
            .requires(ColourUtils.getDyeTag(DyeColor.GREEN))
            .requires(ColourUtils.getDyeTag(DyeColor.BLUE))
            .requires(ColourUtils.getDyeTag(DyeColor.PURPLE))
            .requires(Items.STICK)
        );
    }

    private void turtleOverlay(RecipeOutput add, String overlay, Consumer<ShapelessSpecBuilder> build) {
        for (var turtleItem : turtleItems()) {
            var base = turtleItem.create(-1, null, -1, null, null, 0, null);

            var nameId = turtleItem.getFamily().name().toLowerCase(Locale.ROOT);
            var group = "%s:turtle_%s_overlay".formatted(ComputerCraftAPI.MOD_ID, nameId);

            var builder = ShapelessSpecBuilder.shapeless(RecipeCategory.REDSTONE, base)
                .group(group)
                .unlockedBy("has_turtle", inventoryChange(base.getItem()));
            build.accept(builder);
            builder
                .requires(base.getItem())
                .build(s -> new TurtleOverlayRecipe(s, new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/" + overlay)))
                .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_%s_overlays/%s".formatted(nameId, overlay)));
        }
    }


    private void basicRecipes(RecipeOutput add) {
        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.CABLE.get(), 6)
            .pattern(" # ")
            .pattern("#R#")
            .pattern(" # ")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_redstone", inventoryChange(itemPredicate(ingredients.redstone())))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', ingredients.redstone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ingredients.redstone()), itemPredicate(ingredients.goldIngot())))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(s -> new ComputerUpgradeRecipe(s, ComputerFamily.ADVANCED))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "computer_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.COMPUTER_COMMAND.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('R', Blocks.COMMAND_BLOCK)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_components", inventoryChange(Blocks.COMMAND_BLOCK))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_NORMAL.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.ironIngot())
            .define('C', ModRegistry.Items.COMPUTER_NORMAL.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(s -> Util.getOrThrow(TurtleRecipe.of(s), IllegalArgumentException::new))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("#I#")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.COMPUTER_ADVANCED.get())
            .define('I', ingredients.woodenChest())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_NORMAL.get()))
            .build(s -> Util.getOrThrow(TurtleRecipe.of(s), IllegalArgumentException::new))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.TURTLE_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern(" B ")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.TURTLE_NORMAL.get())
            .define('B', ingredients.goldBlock())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.TURTLE_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(s -> new ComputerUpgradeRecipe(s, ComputerFamily.ADVANCED))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.DISK_DRIVE.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.MONITOR_NORMAL.get())
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.MONITOR_ADVANCED.get(), 4)
            .pattern("###")
            .pattern("#G#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.stone())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_apple", inventoryChange(Items.GOLDEN_APPLE))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#A#")
            .pattern("#G#")
            .define('#', ingredients.goldIngot())
            .define('A', Items.GOLDEN_APPLE)
            .define('G', ingredients.glassPane())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_apple", inventoryChange(Items.GOLDEN_APPLE))
            .save(add);

        ShapedSpecBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get())
            .pattern("###")
            .pattern("#C#")
            .pattern("# #")
            .define('#', ingredients.goldIngot())
            .define('C', ModRegistry.Items.POCKET_COMPUTER_NORMAL.get())
            .unlockedBy("has_components", inventoryChange(itemPredicate(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()), itemPredicate(ingredients.goldIngot())))
            .build(s -> new ComputerUpgradeRecipe(s, ComputerFamily.ADVANCED))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "pocket_computer_advanced_upgrade"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.PRINTER.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("#D#")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .define('D', ingredients.dye())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.SPEAKER.get())
            .pattern("###")
            .pattern("#N#")
            .pattern("#R#")
            .define('#', ingredients.stone())
            .define('N', Blocks.NOTE_BLOCK)
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Items.WIRED_MODEM.get())
            .pattern("###")
            .pattern("#R#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('R', ingredients.redstone())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_cable", inventoryChange(ModRegistry.Items.CABLE.get()))
            .save(add);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .requires(ModRegistry.Items.WIRED_MODEM.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_modem_full_from"));
        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.WIRED_MODEM.get())
            .requires(ModRegistry.Blocks.WIRED_MODEM_FULL.get())
            .unlockedBy("has_modem", inventoryChange(WIRED_MODEM))
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_modem_full_to"));

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.stone())
            .define('E', ingredients.enderPearl())
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .save(add);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.REDSTONE, ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED.get())
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .define('#', ingredients.goldIngot())
            .define('E', Items.ENDER_EYE)
            .unlockedBy("has_computer", inventoryChange(COMPUTER))
            .unlockedBy("has_wireless", inventoryChange(ModRegistry.Blocks.WIRELESS_MODEM_NORMAL.get()))
            .save(add);

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.DECORATIONS, playerHead("Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c"))
            .requires(ingredients.head())
            .requires(ModRegistry.Items.MONITOR_NORMAL.get())
            .unlockedBy("has_monitor", inventoryChange(ModRegistry.Items.MONITOR_NORMAL.get()))
            .build(CustomShapelessRecipe::new)
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "skull_cloudy"));

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.DECORATIONS, playerHead("dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb"))
            .requires(ingredients.head())
            .requires(ModRegistry.Items.COMPUTER_ADVANCED.get())
            .unlockedBy("has_computer", inventoryChange(ModRegistry.Items.COMPUTER_ADVANCED.get()))
            .build(CustomShapelessRecipe::new)
            .save(add, new ResourceLocation(ComputerCraftAPI.MOD_ID, "skull_dan200"));

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_PAGES.get())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 2)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .build(ImpostorShapelessRecipe::new)
            .save(add);

        ShapelessSpecBuilder
            .shapeless(RecipeCategory.REDSTONE, ModRegistry.Items.PRINTED_BOOK.get())
            .requires(ingredients.leather())
            .requires(ModRegistry.Items.PRINTED_PAGE.get(), 1)
            .requires(ingredients.string())
            .unlockedBy("has_printer", inventoryChange(ModRegistry.Blocks.PRINTER.get()))
            .build(ImpostorShapelessRecipe::new)
            .save(add);
    }

    private static DyeColor ofColour(Colour colour) {
        return DyeColor.byId(15 - colour.ordinal());
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(TagKey<Item> stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(itemPredicate(stack));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(ItemLike... stack) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(stack);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(ItemPredicate... items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(items);
    }

    private static ItemPredicate itemPredicate(ItemLike item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(TagKey<Item> item) {
        return ItemPredicate.Builder.item().of(item).build();
    }

    private static ItemPredicate itemPredicate(Ingredient ingredient) {
        var json = Util.getOrThrow(Ingredient.CODEC_NONEMPTY.encodeStart(JsonOps.INSTANCE, ingredient), JsonParseException::new);
        if (!(json instanceof JsonObject object)) throw new IllegalStateException("Unknown ingredient " + json);

        if (object.has("item")) {
            var item = Util.getOrThrow(ItemStack.ITEM_WITH_COUNT_CODEC.parse(JsonOps.INSTANCE, object), JsonParseException::new);
            return itemPredicate(item.getItem());
        } else if (object.has("tag")) {
            return itemPredicate(TagKey.create(Registries.ITEM, new ResourceLocation(GsonHelper.getAsString(object, "tag"))));
        } else {
            throw new IllegalArgumentException("Unknown ingredient " + json);
        }
    }

    private static ItemStack playerHead(String name, String uuid) {
        var item = new ItemStack(Items.PLAYER_HEAD);
        var owner = NbtUtils.writeGameProfile(new CompoundTag(), new GameProfile(UUID.fromString(uuid), name));
        item.getOrCreateTag().put(PlayerHeadItem.TAG_SKULL_OWNER, owner);
        return item;
    }

    private static void addSpecial(RecipeOutput add, Recipe<?> recipe) {
        add.accept(RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, recipe.getSerializer()), recipe, null);
    }
}
