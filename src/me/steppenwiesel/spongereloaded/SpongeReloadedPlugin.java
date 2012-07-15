package me.steppenwiesel.spongereloaded;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class SpongeReloadedPlugin extends JavaPlugin implements Listener {

	private final int idSponge = Material.SPONGE.getId();
	/**
	 * the WorldConfig objects, mapped by the world's names
	 * @see {@link World}, {@link World#getName()}
	 */
	private Map<String, WorldConfig> wconf;
	public final boolean DEBUG = true;

	@Override
	public void onEnable() {
		super.onEnable();
		Bukkit.getPluginManager().registerEvents(this, this);

		// create / check config file
		if (!(new File(this.getDataFolder(), "config.yml").exists())) {
			saveDefaultConfig();
			log("Generated default configuration file.");
		} else
			updateConfig();

		// crafting
		if (getConfig().getBoolean("craftable")) {
			String[] defaultRecipe = new String[] {
					"aba", "bab", "aba", "", "a|SAND", "b|STRING"
			};

			RecipeCreator recipeCreator = null;

			try {
				recipeCreator = new RecipeCreator(new File(this.getDataFolder(), "recipe.cfg"), getLogger(), defaultRecipe);
			} catch (RecipeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ShapedRecipe recipe = recipeCreator.getRecipe();
			if (recipe != null)
				Bukkit.addRecipe(recipe);
		}

		// world configurations
		wconf = new HashMap<>();
		log("Loading world configuration.");
		for (final World i : Bukkit.getWorlds()) {
			wconf.put(i.getName(), new WorldConfig(i, this));
		}

		log("Enabled " + this.getDescription().getName() + " v" + this.getDescription().getVersion());
	}

	@Override
	public void onDisable() {
		super.onDisable();
		// remove world configuration objects from RAM
		wconf = null;
		log("Disabled " + this.getDescription().getName() + " v" + this.getDescription().getVersion());
	}

	private void updateConfig() {
		FileConfiguration config = getConfig();
		final String configVersion = config.getString("config_version", "null");
		if (configVersion.equals("1.3") || configVersion.equals("1.4"));
		else {
			// update everything but range
			final int radius = config.getInt("range", config.getInt("radius", 2));
			new File(this.getDataFolder(), "config.yml").delete();
			saveDefaultConfig();
			reloadConfig();
			// 'config' contains an invalid pointer due to the reload
			config = getConfig();
			config.set("radius", radius);
			saveConfig();
			log("Updated config.yml to version " + config.getString("config_version"));
		}
	}

	/**
	 * generate an 'info' message in the console.
	 * @param text to show
	 */
	private void log(final String text) {
		this.getLogger().info(text);
	}

	/*
	 * BEGIN EVENTHANDLER
	 */

	/**
	 * react to the placement of sponges and water/lava blocks.
	 * @param event
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		final Block block = event.getBlock();
		final WorldConfig wconf = this.wconf.get(block.getWorld().getName());
		if (block.getTypeId() == idSponge)
			wconf.removeSuckables(block, true);
		else if (wconf.isSuckable(block))
			event.setCancelled(wconf.spongeInRange(block));
	}

	/**
	 * react to players emptying a bucket.
	 * @param event
	 */
	@EventHandler
	public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
		final Material bucket = event.getBucket();
		final WorldConfig wconf = this.wconf.get(event.getBlockClicked().getWorld().getName());
		if (wconf.bucketContainsSuckable(bucket)) {
			final Block block = event.getBlockClicked().getRelative(event.getBlockFace());
			event.setCancelled(wconf.spongeInRange(block));
		}
	}

	/**
	 * react to a player using a lighter
	 * @param event
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		final ItemStack item = event.getItem();
		if (item == null || item.getTypeId() != Material.FLINT_AND_STEEL.getId()) return;
		final Block newBlock = event.getClickedBlock().getRelative(event.getBlockFace());
		event.setCancelled(wconf.get(newBlock.getWorld().getName()).spongeInRange(newBlock));
	}

	/**
	 * react to spreading fire
	 * @param event
	 */
	@EventHandler
	public void onBlockSpread(final BlockSpreadEvent event) {
		final Block block = event.getBlock();
		final WorldConfig wconf = this.wconf.get(block.getWorld().getName());
		event.setCancelled(wconf.spongeInRange(block));
	}

	/**
	 * react to flowing water/lava.
	 * @param event
	 */
	@EventHandler
	public void onBlockFromTo(final BlockFromToEvent event) {
		final Block from = event.getBlock();
		final Block to = event.getToBlock();
		final WorldConfig wconf = this.wconf.get(to.getWorld().getName());
		if (wconf.isSuckable(from)) {
			event.setCancelled(wconf.spongeInRange(to));
		}
	}

	/**
	 * react to a sponge that was broken by a player.
	 * @param event
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() != idSponge) return;
		final WorldConfig wconf = this.wconf.get(block.getWorld().getName());
		wconf.callPhysics(block);
	}

	/**
	 * react to a sponge that burnt down.
	 * @param event
	 */
	@EventHandler
	public void onBlockBurn(final BlockBurnEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() != idSponge) return;
		final WorldConfig wconf = this.wconf.get(block.getWorld().getName());
		wconf.callPhysics(block);
	}

	/**
	 * routine block physics check that was initiated by Bukkit.
	 * @param event
	 */
	@EventHandler
	public void onBlockPhysics(final BlockPhysicsEvent event) {
		final Block block = event.getBlock();
		final WorldConfig wconf = this.wconf.get(block.getWorld().getName());
		if (block.getTypeId() == idSponge)
			wconf.removeSuckables(block, false);
		else if (wconf.isSuckable(block) && wconf.spongeInRange(block))
			block.setTypeIdAndData(0, (byte) 0, false);
	}

	/**
	 * react to a sponge that was moved by an extending piston.
	 * @param event
	 */
	@EventHandler
	public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
		// check if sponge(s) is/are affected, call physics if so
		final WorldConfig wconf = this.wconf.get(event.getBlock().getWorld().getName());
		for (final Block i : event.getBlocks()) {
			if (i.getTypeId() == idSponge) {
				wconf.callPhysics(i);
			}
		}
	}

	/**
	 * react to a sponge that was moved by a retracting piston.
	 * @param event
	 */
	@EventHandler
	public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
		final Block oldBlock = event.getBlock().getRelative(event.getDirection(), 2);
		final Block newBlock = event.getBlock().getRelative(event.getDirection(), 1);
		if (!event.isSticky() || oldBlock.getTypeId() != idSponge) return;

		// update sponge
		final WorldConfig wconf = this.wconf.get(event.getBlock().getWorld().getName());
		wconf.removeSuckables(newBlock, true);
		wconf.callPhysics(oldBlock);
	}

	/**
	 * react to endermen stealing a sponge.
	 * @param event
	 */
	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
		final Block block = event.getBlock();
		if (block.getTypeId() == idSponge)
			wconf.get(block.getWorld().getName()).callPhysics(block);
	}

	/*
	 * END EVENTHANDLER
	 */
}
