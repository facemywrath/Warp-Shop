package facemywrath.warpshop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

public class ItemCreator {

	private ItemMeta meta;
	private ItemStack itemStack;

	public ItemCreator(ItemStack itemStack) {
		this.itemStack = itemStack;
		this.meta = itemStack.getItemMeta();
	}

	public static ItemCreator copyOf(ItemStack itemStack) {
		return new ItemCreator(new ItemStack(itemStack));
	}

	public ItemCreator(Material material) {
		this(new ItemStack(material));
	}

	public ItemCreator type(Material material) {
		this.itemStack.setType(material);
		return this;
	}

	public ItemCreator setSkullOwner(String name) {
		if(meta instanceof SkullMeta)
		{
			((SkullMeta)meta).setOwner(name);
		}
		return this;
	}

	public ItemCreator name(String name) {
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		return this;
	}

	public ItemCreator itemMeta(ItemMeta meta) {
		this.meta = meta;
		return this;
	}

	public TextComponent getTextComponent(String text) {

		TextComponent component = new TextComponent(text); 
		net.minecraft.server.v1_12_R1.ItemStack nms = CraftItemStack.asNMSCopy(this.build());
		 NBTTagCompound tag = new NBTTagCompound();
		 nms.save(tag);
		HoverEvent hover_event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(tag.toString()).create());	
		component.setHoverEvent(hover_event);
		return component;
	}	

	public ItemCreator durability(short dura) {
		this.itemStack.setDurability(dura);
		return this;
	}

	public ItemCreator data(MaterialData data) {
		this.itemStack.setData(data);
		return this;
	}

	public ItemCreator lore(String... lore) {
		lore(Arrays.asList(lore));
		return this;
	}

	public ItemCreator lore(List<String> lore) {
		List<String> tempLore = new ArrayList<>();
		for(String str : lore)
		{
			tempLore.add(ChatColor.translateAlternateColorCodes('&', str));
		}
		this.meta.setLore(tempLore);
		return this;
	}

	public ItemCreator enchantments(Map<Enchantment, Integer> enchantments, boolean safe) {
		if (safe)
			itemStack.addEnchantments(enchantments);
		else
			itemStack.addUnsafeEnchantments(enchantments);
		return this;
	}

	public ItemCreator enchantment(Enchantment enchantment, int level, boolean safe) {
		if (safe)
			itemStack.addEnchantment(enchantment, level);
		else
			itemStack.addUnsafeEnchantment(enchantment, level);
		return this;
	}

	public ItemCreator addItemFlags(ItemFlag... flags) {
		this.meta.addItemFlags(flags);
		return this;
	}

	public ItemCreator amount(int amount) {
		this.itemStack.setAmount(amount);
		return this;
	}

	public ItemStack build() {
		itemStack.setItemMeta(meta);
		return itemStack;
	}

}