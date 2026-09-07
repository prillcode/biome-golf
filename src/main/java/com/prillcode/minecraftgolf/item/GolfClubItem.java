package com.prillcode.minecraftgolf.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.prillcode.minecraftgolf.club.ClubDefinition;

/**
 * A registered Minecraft item representing one golf club (ARCHITECTURE.md §12).
 *
 * <p>Right-click is the M3 three-click swing gesture. The client times the
 * meters and sends one finalized shot request; the server validates and
 * launches through {@code ShotService}. Melee attacks remain independent and
 * must never trigger a golf shot.</p>
 */
public class GolfClubItem extends Item {

	private final ClubDefinition club;

	public GolfClubItem(Properties properties, ClubDefinition club) {
		super(properties);
		this.club = club;
	}

	public ClubDefinition club() {
		return club;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		// Do not launch from vanilla item use. It runs once per click on the server,
		// which would bypass the finalized M3 power/accuracy payload.
		return InteractionResult.SUCCESS;
	}

	@Override
	public float getAttackDamageBonus(net.minecraft.world.entity.Entity target, float base,
			net.minecraft.world.damagesource.DamageSource source) {
		// Turn the club into a simple melee weapon. This path never launches a ball.
		return (float) club.meleeDamage();
	}
}
