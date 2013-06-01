package com.lavacraftserver.HarryPotterSpells.Spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.lavacraftserver.HarryPotterSpells.HPS;
import com.lavacraftserver.HarryPotterSpells.Spells.Spell.spell;
import com.lavacraftserver.HarryPotterSpells.Utils.Targeter;

@spell (
		name="Stupefy",
		description="Stuns the target",
		range=50,
		goThroughWalls=false,
		cooldown=60
)
public class Stupefy extends Spell {

	@Override
	public boolean cast(Player p) {
		if (Targeter.getTarget(p, this.getRange(), this.canBeCastThroughWalls()) instanceof LivingEntity) {
			
			LivingEntity le = Targeter.getTarget(p, this.getRange(), this.canBeCastThroughWalls());
			
			int verticalKnockback = HPS.Plugin.getConfig().getInt("spells.stupefy.vertical-knockback", 2);
			double horizontalKnockback = HPS.Plugin.getConfig().getDouble("spells.stupefy.horizontal-knockback", 0.5);
			int damage = HPS.Plugin.getConfig().getInt("spells.stupefy.damage", 2);
			
			String confusionDurationString = HPS.Plugin.getConfig().getString("spells.stupefy.confusion-duration");
			int confusionDuration = 0;
			String weaknessDurationString = HPS.Plugin.getConfig().getString("spells.stupefy.weakness-duration");
			int weaknessDuration = 0;
			
			if (confusionDurationString.endsWith("t")) {
				String ticks = confusionDurationString.substring(0, confusionDurationString.length() - 1);
				confusionDuration = Integer.parseInt(ticks);
			} else {
				confusionDuration = Integer.parseInt(confusionDurationString) * 20;
			}
			
			if (weaknessDurationString.endsWith("t")) {
				String ticks = weaknessDurationString.substring(0, weaknessDurationString.length() - 1);
				weaknessDuration = Integer.parseInt(ticks);
			} else {
				weaknessDuration = Integer.parseInt(weaknessDurationString) * 20;
			}

			le.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, confusionDuration, 1));
			le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 1));
			
			Vector unitVector = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
			le.setVelocity(unitVector.multiply(verticalKnockback));
			le.setVelocity(le.getVelocity().setY(horizontalKnockback));
			
			le.damage(damage);
			
			return true;
			
		} else {
			HPS.PM.warn(p, "This can only be used on a player or a mob.");
			return false;
		}
	}
}