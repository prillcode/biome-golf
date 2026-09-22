package com.prillcode.minecraftgolf.item;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.ClubDefinition;
import com.prillcode.minecraftgolf.club.GolfClubs;

/**
 * Registry holder for the M2 golf club items, plus the M10 client-light
 * representation.
 *
 * <p>Two representations exist for the same logical club:</p>
 * <ul>
 *   <li><b>Custom</b> ({@code minecraft_golf:club_*}) — nicer models, but only a
 *       modded client can resolve them.</li>
 *   <li><b>Fallback</b> — a distinct vanilla item with a custom name and an
 *       identifying {@code custom_data} tag. Visible and distinguishable on any
 *       client, including Bedrock through Geyser.</li>
 * </ul>
 *
 * <p>{@link #clubOf(ItemStack)} recognises both, so shot validation stays
 * representation-agnostic and the server stays authoritative.</p>
 */
public final class GolfItems {

	/** NBT key inside {@code minecraft:custom_data} identifying a fallback club. */
	static final String CLUB_TAG = "minecraft_golf_club";

	/** All registered golf club items keyed by their club id, in catalog order. */
	public static final Map<String, GolfClubItem> CLUB_ITEMS = new LinkedHashMap<>();

	/**
	 * Vanilla base item used for each club's client-light representation. Chosen to
	 * be distinct, non-edible, and not placeable, so a plain right-click does
	 * nothing on a vanilla client. Resolved lazily so this class can be inspected
	 * without bootstrapping the vanilla item registry.
	 */
	private static Map<String, Item> fallbackBase;

	private static boolean registered;

	private GolfItems() {
	}

	/**
	 * Registers every club item. Idempotent; safe to call once from onInitialize.
	 * Populates {@link #CLUB_ITEMS}.
	 */
	public static void registerAll() {
		if (registered) {
			return;
		}
		registered = true;
		for (ClubDefinition club : GolfClubs.ALL) {
			CLUB_ITEMS.put(club.id(), register(club));
		}
		if (CLUB_ITEMS.size() != GolfClubs.ALL.size()) {
			throw new IllegalStateException("club catalog and item registry diverged");
		}
	}

	/** Convenience after registerAll: custom item for a club id, or null. */
	public static GolfClubItem itemFor(String clubId) {
		return CLUB_ITEMS.get(clubId);
	}

	/** Item stack for a club, in the representation the player's client can resolve. */
	public static ItemStack stackFor(ClubDefinition club, boolean moddedClient) {
		return moddedClient ? customStack(club) : fallbackStack(club);
	}

	/** Custom item stack (modded clients only). */
	public static ItemStack customStack(ClubDefinition club) {
		return new ItemStack(itemFor(club.id()));
	}

	private static Map<String, Item> fallbackBase() {
		if (fallbackBase == null) {
			fallbackBase = Map.of(
					"driver", Items.BLAZE_ROD,
					"fairway_wood", Items.STICK,
					"long_iron", Items.IRON_INGOT,
					"mid_iron", Items.COPPER_INGOT,
					"short_iron", Items.GOLD_INGOT,
					"wedge", Items.PRISMARINE_SHARD,
					"putter", Items.FLINT);
		}
		return fallbackBase;
	}

	/** The identifying tag written into a fallback club's {@code custom_data}. */
	static CompoundTag clubTag(String clubId) {
		CompoundTag tag = new CompoundTag();
		tag.putString(CLUB_TAG, clubId);
		return tag;
	}

	/** The club id stored in a fallback club's {@code custom_data}, or "" if absent. */
	static String clubIdFromTag(CompoundTag tag) {
		return tag == null ? "" : tag.getString(CLUB_TAG).orElse("");
	}

	/** Vanilla-visible club stack with a display name and identifying tag. */
	public static ItemStack fallbackStack(ClubDefinition club) {
		Item base = fallbackBase().get(club.id());
		if (base == null) {
			throw new IllegalStateException("no fallback item for club '" + club.id() + "'");
		}
		ItemStack stack = new ItemStack(base);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(club.displayName()));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(clubTag(club.id())));
		return stack;
	}

	/** The logical club a held stack represents, in either representation, or null. */
	public static ClubDefinition clubOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		if (stack.getItem() instanceof GolfClubItem custom) {
			return custom.club();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		String id = clubIdFromTag(data.copyTag());
		if (id.isEmpty()) {
			return null;
		}
		return GolfClubs.byId(id).orElse(null);
	}

	private static GolfClubItem register(ClubDefinition club) {
		ResourceKey<Item> key = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "club_" + club.id()));
		Item.Properties props = new Item.Properties().setId(key);
		return Registry.register(BuiltInRegistries.ITEM, key, new GolfClubItem(props, club));
	}
}
