package com.prillcode.minecraftgolf.item;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.ClubDefinition;
import com.prillcode.minecraftgolf.club.GolfClubs;

/**
 * Registry holder for the M2 golf club items.
 *
 * <p>One {@link GolfClubItem} per {@link ClubDefinition} in {@link GolfClubs}.
 * Registration happens explicitly from {@link MinecraftGolf#onInitialize} (the
 * same pattern as {@code GolfBallEntities}) so the mod is dedicated-server-safe
 * and logs each registered item. All item code lives in {@code src/main}; no
 * client import is reachable here. Client presentation (item model/texture) is
 * layered purely through resource files, never through Java.</p>
 */
public final class GolfItems {

	/** All registered golf club items keyed by their club id, in catalog order. */
	public static final Map<String, GolfClubItem> CLUB_ITEMS = new LinkedHashMap<>();

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

	/** Convenience after registerAll: item for a club id, or null. */
	public static GolfClubItem itemFor(String clubId) {
		return CLUB_ITEMS.get(clubId);
	}

	private static GolfClubItem register(ClubDefinition club) {
		ResourceKey<Item> key = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "club_" + club.id()));
		Item.Properties props = new Item.Properties().setId(key);
		return Registry.register(BuiltInRegistries.ITEM, key, new GolfClubItem(props, club));
	}
}
