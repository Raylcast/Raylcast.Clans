package raylcast.clans;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class CustomItems {
    private final Plugin Plugin;

    private final int EarthShard = 44892395;
    private final int FireShard = 44892396;
    private final int EnderShard = 44892397;
    private final int ThunderShard = 44892398;
    private final int ElementShard = 44892399;

    private final NamespacedKey ElementCrystalKey;
    private final ShapedRecipe ElementCrystalRecipe;
    private final ItemStack ElementCrystalItem;

    private final NamespacedKey EarthCrystalKey;
    private final ShapedRecipe EarthCrystalRecipe;
    private final ItemStack EarthCrystalItem;

    private final NamespacedKey FireCrystalKey;
    private final ShapedRecipe FireCrystalRecipe;
    private final ItemStack FireCrystalItem;

    private final NamespacedKey EnderCrystalKey;
    private final ShapedRecipe EnderCrystalRecipe;
    private final ItemStack EnderCrystalItem;

    private final NamespacedKey ThunderCrystalKey;
    private final ShapedRecipe ThunderCrystalRecipe;
    private final ItemStack ThunderCrystalItem;

    public CustomItems(Plugin plugin){
        Plugin = plugin;

        ElementCrystalItem = getCrystalItem(ElementShard, 1);
        ElementCrystalKey = new NamespacedKey(Plugin, "clans.element_crystal");
        ElementCrystalRecipe = getElementCrystalRecipe();

        EarthCrystalItem = getCrystalItem(EarthShard, 1);
        EarthCrystalKey = new NamespacedKey(Plugin, "clans.earth_crystal");
        EarthCrystalRecipe = getEarthCrystalRecipe();

        FireCrystalItem = getCrystalItem(FireShard, 1);
        FireCrystalKey = new NamespacedKey(Plugin, "clans.fire_crystal");
        FireCrystalRecipe = getFireCrystalRecipe();

        EnderCrystalItem = getCrystalItem(EnderShard, 1);
        EnderCrystalKey = new NamespacedKey(Plugin, "clans.ender_crystal");
        EnderCrystalRecipe = getEnderCrystalRecipe();

        ThunderCrystalItem = getCrystalItem(ThunderShard, 1);
        ThunderCrystalKey = new NamespacedKey(Plugin, "clans.thunder_crystal");
        ThunderCrystalRecipe = getThunderCrystalRecipe();
    }

    public ShapedRecipe[] getRecipes(){
        return new ShapedRecipe[] {
            ElementCrystalRecipe,
            EarthCrystalRecipe,
            FireCrystalRecipe,
            EnderCrystalRecipe,
            ThunderCrystalRecipe
        };
    }

    private ItemStack getCrystalItem(int customModelData, int amount){
        var item = new ItemStack(Material.GOLD_NUGGET, amount);
        var meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        return item;
    }

    private ShapedRecipe getElementCrystalRecipe(){
        var recipe = new ShapedRecipe(ElementCrystalKey, ElementCrystalItem);

        recipe.shape("ABC","DEF","GHI");

        recipe.setIngredient('A', Material.ENDER_PEARL);
        recipe.setIngredient('B', Material.FLINT_AND_STEEL);
        recipe.setIngredient('C', Material.BONE_MEAL);

        recipe.setIngredient('D', Material.PRISMARINE_SHARD);
        recipe.setIngredient('E', Material.DRAGON_EGG);
        recipe.setIngredient('F', Material.NAUTILUS_SHELL);

        recipe.setIngredient('G', Material.DRAGON_BREATH);
        recipe.setIngredient('H', Material.NETHERITE_INGOT);
        recipe.setIngredient('I', Material.BELL);

        return recipe;
    }

    private ShapedRecipe getEarthCrystalRecipe(){
        var recipe = new ShapedRecipe(EarthCrystalKey, EarthCrystalItem);

        recipe.shape("ABA","CDC","EFE");

        recipe.setIngredient('A', Material.FLOWERING_AZALEA);
        recipe.setIngredient('B', Material.ENCHANTED_GOLDEN_APPLE);

        recipe.setIngredient('C', Material.LARGE_AMETHYST_BUD);
        recipe.setIngredient('D', ElementCrystalItem);

        recipe.setIngredient('E', Material.HONEY_BLOCK);
        recipe.setIngredient('F', Material.POWDER_SNOW_BUCKET);

        return recipe;
    }

    private ShapedRecipe getFireCrystalRecipe(){
        var recipe = new ShapedRecipe(FireCrystalKey, FireCrystalItem);

        recipe.shape("ABA","CDC","EFE");

        recipe.setIngredient('A', Material.GLOWSTONE);
        recipe.setIngredient('B', Material.BEACON);

        recipe.setIngredient('C', Material.BLAZE_ROD);
        recipe.setIngredient('D', ElementCrystalItem);

        recipe.setIngredient('E', Material.GHAST_TEAR);
        recipe.setIngredient('F', Material.GOLD_BLOCK);

        return recipe;
    }

    private ShapedRecipe getEnderCrystalRecipe(){
        var recipe = new ShapedRecipe(EnderCrystalKey, EnderCrystalItem);

        recipe.shape("ABA","CDC","EFE");

        recipe.setIngredient('A', Material.ENDER_PEARL);
        recipe.setIngredient('B', Material.DRAGON_HEAD);

        recipe.setIngredient('C', Material.SHULKER_SHELL);
        recipe.setIngredient('D', ElementCrystalItem);

        recipe.setIngredient('E', Material.ENDER_EYE);
        recipe.setIngredient('F', Material.CHORUS_FRUIT);

        return recipe;
    }

    private ShapedRecipe getThunderCrystalRecipe(){
        var recipe = new ShapedRecipe(ThunderCrystalKey, ThunderCrystalItem);

        recipe.shape("ABA","CDC","EFE");

        recipe.setIngredient('A', Material.CONDUIT);
        recipe.setIngredient('B', Material.TRIDENT);

        recipe.setIngredient('C', Material.GLOW_INK_SAC);
        recipe.setIngredient('D', ElementCrystalItem);

        recipe.setIngredient('E', Material.EXPERIENCE_BOTTLE);
        recipe.setIngredient('F', Material.PUFFERFISH_BUCKET);

        return recipe;
    }
}
