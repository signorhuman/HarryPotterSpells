package com.hpspells.core.spell;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.reflections.Reflections;

import com.google.common.collect.Iterables;
import com.hpspells.core.CoolDown;
import com.hpspells.core.HPS;
import com.hpspells.core.api.event.SpellPostCastEvent;
import com.hpspells.core.api.event.SpellPreCastEvent;
import com.hpspells.core.configuration.ConfigurationManager.ConfigurationType;
import com.hpspells.core.configuration.PlayerSpellConfig;

/**
 * A class that manages spells and holds lots of spell related utilities
 */
public class SpellManager {
	private HashMap<String, HashMap<Spell, Integer>> cooldowns = new HashMap<String, HashMap<Spell, Integer>>();
	private Comparator<Spell> spellComparator = new Comparator<Spell>() {

		@Override
		public int compare(Spell o1, Spell o2) {
			return o1.getName().compareTo(o2.getName());
		}

	};
	private SortedSet<Spell> spellList = new TreeSet<Spell>(spellComparator);
	private Map<String, Integer> currentSpell = new HashMap<String, Integer>();
	private HPS HPS;

	public final Permission NO_COOLDOWN_ALL_1 = new Permission("HarryPotterSpells.nocooldown", PermissionDefault.OP), NO_COOLDOWN_ALL_2 = new Permission("HarryPotterSpells.nocooldown.*");

	/**
	 * Constructs the {@link SpellManager}, adding all core spells to the Spell List
	 * @param plugin an instance of {@link HPS}
	 */
	public SpellManager(HPS instance) {
	    this.HPS = instance;
	    HPS.getServer().getPluginManager().addPermission(NO_COOLDOWN_ALL_1);
        HPS.getServer().getPluginManager().addPermission(NO_COOLDOWN_ALL_2);

		Reflections ref = Reflections.collect();
		for (Class<?> clazz : ref.getTypesAnnotatedWith(Spell.SpellInfo.class)) {
			Spell spell;
			if (clazz == Spell.class || !Spell.class.isAssignableFrom(clazz))
				continue;
			try {
				spell = (Spell) clazz.getConstructor(HPS.class).newInstance(HPS);

				if(Listener.class.isAssignableFrom(clazz)) {
				    HPS.getServer().getPluginManager().registerEvents((Listener) spell, HPS);
				}

			} catch (Exception e) {
				HPS.PM.log(Level.WARNING, HPS.Localisation.getTranslation("errSpells", clazz.getSimpleName()));
				HPS.PM.debug(e);
				continue;
			}
			spellList.add(spell);
		}
	}

	/**
	 * Gets the current spell position a player is on
	 * 
	 * @param player the player
	 * @return the current spell position they are on
	 */
	public Integer getCurrentSpellPosition(Player player) {
        PlayerSpellConfig psc = (PlayerSpellConfig) HPS.ConfigurationManager.getConfig(ConfigurationType.PLAYER_SPELL_CONFIG);
		List<String> spellsTheyKnow = psc.getStringListOrEmpty(player.getName());

		if (spellsTheyKnow.isEmpty())
			return null;

		if (!currentSpell.containsKey(player.getName()))
			return 0;

		return currentSpell.get(player.getName());
	}

	/**
	 * Gets the current spell a player is on
	 * 
	 * @param player the player
	 * @return the current spell they are on
	 */
	public Spell getCurrentSpell(Player player) {
        PlayerSpellConfig psc = (PlayerSpellConfig) HPS.ConfigurationManager.getConfig(ConfigurationType.PLAYER_SPELL_CONFIG);
		Integer cur = getCurrentSpellPosition(player);
		List<String> spells = psc.getStringListOrEmpty(player.getName());
		if (spells.isEmpty())
			return null;
		return cur == null ? null : getSpell(Iterables.get(new TreeSet<String>(spells), cur.intValue()));
	}

	/**
	 * Sets the current spell position a player is on
	 * 
	 * @param player the player
	 * @param id the new current spell position the player is on
	 * @return the spell they have changed to
	 * @throws IllegalArgumentException if the id parameter is invalid
	 */
	public Spell setCurrentSpell(Player player, int id) throws IllegalArgumentException {
        PlayerSpellConfig psc = (PlayerSpellConfig) HPS.ConfigurationManager.getConfig(ConfigurationType.PLAYER_SPELL_CONFIG);
		List<String> spellsTheyKnow = psc.getStringListOrEmpty(player.getName());
		if (spellsTheyKnow == null || id >= spellsTheyKnow.size() || id < 0)
			throw new IllegalArgumentException("id was invalid");
		currentSpell.put(player.getName(), id);
		return getCurrentSpell(player);
	}

	/**
	 * Sets the current spell a player is on
	 * 
	 * @param player the player
	 * @param spell the new spell the player is on
	 * @return the spell they have changed to for chaining
	 * @throws IllegalArgumentException if the spell parameter is invalid
	 */
	public Spell setCurrentSpell(Player player, Spell spell) throws IllegalArgumentException {
        PlayerSpellConfig psc = (PlayerSpellConfig) HPS.ConfigurationManager.getConfig(ConfigurationType.PLAYER_SPELL_CONFIG);
		Integer spellIndex = getIndex(new TreeSet<String>(psc.getStringListOrEmpty(player.getName())), spell.getName());
		if (spellIndex == null)
			throw new IllegalArgumentException("player does not know that spell");
		setCurrentSpell(player, spellIndex);
		return getCurrentSpell(player);
	}

	/**
	 * Gets a spell by name
	 * 
	 * @param name the spell to get
	 * @return the spell or {@code null} if not found
	 */
	public Spell getSpell(String name) {
		for (Spell spell : spellList)
			if (spell.getName().equalsIgnoreCase(name))
				return spell;
		return null;
	}

	/**
	 * Adds a spell to the spell list
	 * 
	 * @param spell the spell
	 */
	public void addSpell(Spell spell) {
		spellList.add(spell);
	}

	/**
	 * Gets a list of all spells <br>
	 * <b>Note:</b> this returns a {@link SortedSet}. If you do not need to use
	 * a sorted set then cast it as a {@link Set}!
	 * 
	 * @return the list
	 */
	public SortedSet<Spell> getSpells() {
		return spellList;
	}

	/**
	 * Checks if a string corrosponds to a spell
	 * 
	 * @param name the name to test
	 * @return {@code true} if the spell exists
	 */
	public boolean isSpell(String name) {
		return getSpell(name) != null;
	}

	/**
	 * Casts a spell cleverly; checking permissions, sending effects ect
	 * 
	 * @param player the player who is casting
	 * @param spell the spell that they are casting
	 */
	public void cleverCast(Player player, Spell spell) {
		if (!player.hasPermission("HarryPotterSpells.use") || !spell.playerKnows(player) || !player.getInventory().contains(Material.STICK))
			return;

        PlayerSpellConfig psc = (PlayerSpellConfig) HPS.ConfigurationManager.getConfig(ConfigurationType.PLAYER_SPELL_CONFIG);

		List<String> spellList = psc.getStringListOrEmpty(player.getName());
		if (spellList == null || spellList.isEmpty()) {
			HPS.PM.tell(player, "You don't know any spells.");
			return;
		}

		if (HPS.getConfig().getBoolean("spell-particle-toggle")) {
			Location l = player.getLocation();
			l.setY(l.getY() + 1);
			player.getWorld().playEffect(l, Effect.ENDER_SIGNAL, 0);
		}

		SpellPreCastEvent sce = new SpellPreCastEvent(spell, player);
		Bukkit.getServer().getPluginManager().callEvent(sce);
		if (!sce.isCancelled()) {
			Boolean cast = true;
			String playerName = player.getName();
			if (cooldowns.containsKey(playerName) && cooldowns.get(playerName).containsKey(spell)) { // TODO
																										// move
																										// to
																										// another
																										// class
				HPS.PM.dependantMessagingTell((CommandSender) player, HPS.Localisation.getTranslation("cldWait", cooldowns.get(playerName).get(spell).toString()));
				cast = false;
			}
			boolean successful = false;
			if (cast) {
				successful = spell.cast(player);
			}
			Bukkit.getServer().getPluginManager().callEvent(new SpellPostCastEvent(spell, player, successful));

			if (cast && successful && spell.getCoolDown(player) > 0) {
				setCoolDown(playerName, spell, spell.getCoolDown(player));
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HPS, new CoolDown(HPS, playerName, spell), 20L);
			}
		}
	}

	/**
	 * Gets the cooldown of a certain spell for a certain player
	 * 
	 * @param playerName
	 * @param spell
	 * @return
	 */
	public Integer getCoolDown(String playerName, Spell spell) {
		return cooldowns.get(playerName).get(spell);
	}

	/**
	 * Sets the cooldown of a certain spell for a certain player
	 * 
	 * @param playerName
	 * @param spell
	 * @param cooldown
	 */
	public void setCoolDown(String playerName, Spell spell, Integer cooldown) {
		if (cooldowns.containsKey(playerName) && cooldowns.get(playerName).containsKey(spell)) {
			if (cooldown == null) {
				cooldowns.get(playerName).remove(spell);
			} else {
				cooldowns.get(playerName).put(spell, cooldown);
			}
		} else if (cooldowns.containsKey(playerName) && !cooldowns.get(playerName).containsKey(spell)) {
			if (cooldown == null) {
				return;
			} else {
				cooldowns.get(playerName).put(spell, cooldown);
			}
		} else {
			if (cooldown == null) {
				return;
			} else {
				cooldowns.put(playerName, new HashMap<Spell, Integer>());
				cooldowns.get(playerName).put(spell, cooldown);
			}
		}
	}

	/*
	 * START PRIVATE UTILITIES
	 */

	private Integer getIndex(Set<? extends Object> set, Object value) {
		int result = 0;
		for (Object entry : set) {
			if (entry.equals(value))
				return result;
			result++;
		}
		return null;
	}

}