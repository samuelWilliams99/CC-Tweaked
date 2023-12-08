// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.recipe.CustomShapedRecipe;
import dan200.computercraft.shared.recipe.ShapedRecipeSpec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * A recipe which "upgrades" a {@linkplain IComputerItem computer}, converting it from one {@linkplain ComputerFamily
 * family} to another.
 */
public final class ComputerUpgradeRecipe extends ComputerConvertRecipe {
    private final ComputerFamily family;

    public ComputerUpgradeRecipe(ShapedRecipeSpec recipe, ComputerFamily family) {
        super(recipe);
        this.family = family;
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        return item.withFamily(stack, family);
    }

    @Override
    public RecipeSerializer<ComputerUpgradeRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get();
    }

    public static class Serializer implements RecipeSerializer<ComputerUpgradeRecipe> {
        private static final Codec<ComputerUpgradeRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShapedRecipeSpec.CODEC.forGetter(CustomShapedRecipe::toSpec),
            StringRepresentable.fromEnum(ComputerFamily::values).fieldOf("family").forGetter(x -> x.family)
        ).apply(instance, ComputerUpgradeRecipe::new));

        @Override
        public Codec<ComputerUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public ComputerUpgradeRecipe fromNetwork(FriendlyByteBuf buf) {
            var recipe = ShapedRecipeSpec.fromNetwork(buf);
            var family = buf.readEnum(ComputerFamily.class);
            return new ComputerUpgradeRecipe(recipe, family);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ComputerUpgradeRecipe recipe) {
            recipe.toSpec().toNetwork(buf);
            buf.writeEnum(recipe.family);
        }
    }
}
