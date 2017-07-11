package me.jadenPete.TNTFun;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/*
 * This class is responsible for:
 *   - Providing usefull functions to Events..
 *   - Providing functions to manage the game to Events.
 *   - Providing functions that communicate with WorldGuard.
 *   
 * It doesn't really do anything on it's own.
 */

public class Util {
	// This variable allows us to access non-static
	// variables and methods from the main class.
	public static Main plugin;
	
	// To access the plugin's config.yml.
	public static FileConfiguration config;

	// World that the plugin runs in.
	public static World world;
	
	// To access the world's regions.
	public static RegionManager regions;
	
	// The main game's region.
	public static ProtectedRegion region;
	
	public static Thread countdownThread;
	public static boolean gameStarted = false;
	public static List<String> players = new Vector<String>();
	
	// Initialize the variables that require access
	// to non-static objects/methods located in Main.
	public Util(Main instance){
		plugin = instance;
		config = plugin.getConfig();
		world = Bukkit.getWorld(config.getString("regions.world"));
		regions = getWorldGuard().getRegionContainer().get(world);
	}
	
	// Load the WorldGuard plugin.
	private static WorldGuardPlugin getWorldGuard(){
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

	    // WorldGuard may not be loaded.
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null;
	    }

	    return (WorldGuardPlugin) plugin;
	}
	
	// Register the main game's region through WorldGuard.
	public static void registerRegion(){
		// Get the coordinates of the vectors.
		List<Integer> v1Coords = config.getIntegerList("regions.main.vector1");
		List<Integer> v2Coords = config.getIntegerList("regions.main.vector2");
		
		// Convert the coordinates to BlockVectors.
		BlockVector vector1 = new BlockVector(v1Coords.get(0), v1Coords.get(1), v1Coords.get(2));
		BlockVector vector2 = new BlockVector(v2Coords.get(0), v2Coords.get(1), v2Coords.get(2));
		
		// Combine the vectors to a cuboid WorldGuard region.
		region = new ProtectedCuboidRegion("main", vector1, vector2);
		
		// If the region doesn't exist, register it with WorldGuard.
		if(regions.getRegion("main") != null){
			regions.addRegion(region);
		}
		
		// Set the necessary protection flags.
		region.setFlag(DefaultFlag.BUILD, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.BUILD.getRegionGroupFlag(), RegionGroup.ALL);
		region.setFlag(DefaultFlag.USE, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.PLACE_VEHICLE, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.PLACE_VEHICLE.getRegionGroupFlag(), RegionGroup.ALL);
		region.setFlag(DefaultFlag.ITEM_DROP, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.POTION_SPLASH, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.ENDERPEARL, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.ENDERPEARL.getRegionGroupFlag(), RegionGroup.ALL);
		
		region.setFlag(DefaultFlag.PVP, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.MOB_DAMAGE, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.MOB_SPAWNING, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.CREEPER_EXPLOSION, StateFlag.State.DENY);
		region.setFlag(DefaultFlag.LIGHTNING, StateFlag.State.DENY);
	}
	
	// Countdown and initialize the game in a seperate Thread.
	public static Runnable countdown = new Runnable(){
		@Override
		public void run(){
			// For every second before the game starts, check that the
			// players haven't left and send them hotbar messages.
			for(int a = config.getInt("options.countdown"); a >= 0; a--){
				List<String> invalidPlayers = new ArrayList<String>();
				List<String> currentPlayers = players;
				
				for(String player : currentPlayers){
					if(Bukkit.getPlayer(player) != null){
						if(a > 0){
							HotbarMessager.sendHotBarMessage(Bukkit.getPlayer(player),
															 config.getString("messages.countdown")
															 .replace("%s", String.valueOf(a)));
							
							try {
								Thread.sleep(1000);
							} catch(InterruptedException e){
								Thread.currentThread().interrupt();
							}
						} else {
							HotbarMessager.sendHotBarMessage(Bukkit.getPlayer(player), "");
						}
					} else {
						invalidPlayers.add(player);
					}
				}
				
				// Remove players who left.
				for(String player : invalidPlayers){
					players.remove(player);
				}
				
				// If there aren't enough players, stop the game.
				if(players.size() < config.getInt("options.min-players")){
					Thread.currentThread().interrupt();
				}
			}
			
			// Lock the players in the game region.
			region.setFlag(DefaultFlag.ENTRY, StateFlag.State.ALLOW);
			region.setFlag(DefaultFlag.ENTRY.getRegionGroupFlag(), RegionGroup.MEMBERS);
			
			region.setFlag(DefaultFlag.EXIT, StateFlag.State.DENY);
			region.setFlag(DefaultFlag.EXIT.getRegionGroupFlag(), RegionGroup.ALL);
			
			// The game has started, tell all the players.
			gameStarted = true;
			
			for(String player : players){
				Bukkit.getPlayer(player).sendMessage(config.getString("messages.started"));
			}
		}
	};
	
	// The game is over, reset the arena, anounce the winner, and allow new people in.
	public static void resetGame(Player winner){
		// Notify everyone in the world of the winner.
		for(Player p : Bukkit.getOnlinePlayers()){
		    if(p.getWorld().getName().equals(world.getName())){
		        p.sendMessage(config.getString("messages.player-won").replace("%p", winner.getName()));
		    }
		}
		
		// Load the layer coordinates into cuboid regions and reset them.
		List<List<Integer>> layer1Coords = new ArrayList<List<Integer>>();
		List<List<Integer>> layer2Coords = new ArrayList<List<Integer>>();
		List<List<Integer>> layer3Coords = new ArrayList<List<Integer>>();
		
		Region layer1;
		Region layer2;
		Region layer3;
		
		layer1Coords.add(config.getIntegerList("regions.layer1.vector1"));
		layer1Coords.add(config.getIntegerList("regions.layer1.vector2"));
		
		layer2Coords.add(config.getIntegerList("regions.layer2.vector1"));
		layer2Coords.add(config.getIntegerList("regions.layer2.vector2"));
		
		layer3Coords.add(config.getIntegerList("regions.layer3.vector1"));
		layer3Coords.add(config.getIntegerList("regions.layer3.vector2"));
		
		layer1 = new CuboidRegion(new BlockVector(layer1Coords.get(0).get(0), layer1Coords.get(0).get(1), layer1Coords.get(0).get(2)),
								  new BlockVector(layer1Coords.get(1).get(0), layer1Coords.get(1).get(1), layer1Coords.get(1).get(2)));
		
		layer2 = new CuboidRegion(new BlockVector(layer2Coords.get(0).get(0), layer2Coords.get(0).get(1), layer2Coords.get(0).get(2)),
				  				  new BlockVector(layer2Coords.get(1).get(0), layer2Coords.get(1).get(1), layer2Coords.get(1).get(2)));
		
		layer3 = new CuboidRegion(new BlockVector(layer3Coords.get(0).get(0), layer3Coords.get(0).get(1), layer3Coords.get(0).get(2)),
				 				  new BlockVector(layer3Coords.get(1).get(0), layer3Coords.get(1).get(1), layer3Coords.get(1).get(2)));
		
		for(BlockVector block : layer1){
			Block bukkitBlock = new Location(world, block.getX(), block.getY(), block.getZ()).getBlock();
			
			bukkitBlock.setType(Material.TNT);
		}
		
		for(BlockVector block : layer2){
			Block bukkitBlock = new Location(world, block.getX(), block.getY(), block.getZ()).getBlock();
			
			bukkitBlock.setType(Material.TNT);
		}
		
		for(BlockVector block : layer3){
			Block bukkitBlock = new Location(world, block.getX(), block.getY(), block.getZ()).getBlock();
			
			bukkitBlock.setType(Material.TNT);
		}
		
		// Allow new players into the game region.
		gameStarted = false;
		
		region.setFlag(DefaultFlag.ENTRY.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
		region.setFlag(DefaultFlag.EXIT, StateFlag.State.ALLOW);
	}
	
	// Remove a player from the game.
	public static void removePlayer(Player player){
		String name = player.getName();
		
		// Make sure that the player is in the game.
		if(players.contains(name)){
			players.remove(name);
			region.getMembers().removePlayer(name);
			
			// Tell all the remaining players that the player has left.
			for(String p : players){
				Bukkit.getPlayer(p).sendMessage(config.getString("messages.player-left")
												.replace("%p", name).replace("%n", String.valueOf(players.size()))
												.replace("%m", config.getString("options.max-players")));
			}
			
			// If the game is no longer playable, end it.
			if(players.size() < config.getInt("options.min-players")){
				if(gameStarted){
					resetGame(player);
				} else if(countdownThread.isAlive()){
					countdownThread.interrupt();
				}
			// If a spot has been freed, allow more players.
			} else if(players.size() == config.getInt("options.max-players") - 1){
				region.setFlag(DefaultFlag.ENTRY, StateFlag.State.ALLOW);
				region.setFlag(DefaultFlag.ENTRY.getRegionGroupFlag(), RegionGroup.ALL);
			}
		}
	}
	
	// Since Bukkit's getBlock() doesn't return every block that a
	// player is standing on, I wrote this method to do exactly that.
	// It's written like shit but I couldn't find a better way to do it.
	public static ArrayList<Block> getBlocksBelow(Player player){
		ArrayList<Block> blocksBelow = new ArrayList<Block>();
		Location location = player.getLocation();
		double x = location.getX();
		double z = location.getZ();
		
		// Add the block below the player to the List.
		Location block = new Location(world, x, location.getY() - 1, z);
		blocksBelow.add(block.getBlock());
		
		// If the player is on the edge of the block, add the necessary blocks to the List.
		if(x - Math.floor(x) <= 0.3){
			block.setX(Math.floor(x) - 0.5);
			blocksBelow.add(block.getBlock());
			block = new Location(world, x, location.getY() - 1, z);
		} else if(x - Math.floor(x) >= 0.7){
			block.setX(Math.ceil(x) + 0.5);
			blocksBelow.add(block.getBlock());
			block = new Location(world, x, location.getY() - 1, z);
		}
		
		if(z - Math.floor(z) <= 0.3){
			block.setZ(Math.floor(z) - 0.5);
			blocksBelow.add(block.getBlock());
		} else if(z - Math.floor(z) >= 0.7){
			block.setZ(Math.ceil(z) + 0.5);
			blocksBelow.add(block.getBlock());
		}
		
		if(x - Math.floor(x) <= 0.3 && z - Math.floor(z) <= 0.3){
			block.setX(Math.floor(x) - 0.5);
			block.setZ(Math.floor(z) - 0.5);
			
			blocksBelow.add(block.getBlock());
		} else if(x - Math.floor(x) >= 0.7 && z - Math.floor(x) >= 0.7){
			block.setX(Math.ceil(x) + 0.5);
			block.setZ(Math.ceil(z) + 0.5);
			
			blocksBelow.add(block.getBlock());
		} else if(x - Math.floor(x) <= 0.3 && z - Math.floor(z) >= 0.7){
			block.setX(Math.floor(x) - 0.5);
			block.setZ(Math.ceil(z) + 0.5);
			
			blocksBelow.add(block.getBlock());
		} else if(x - Math.floor(x) >= 0.7 && z - Math.floor(z) <= 0.3){
			block.setX(Math.ceil(x) + 0.5);
			block.setZ(Math.floor(z) - 0.5);
			
			blocksBelow.add(block.getBlock());
		}
		
		// Return the List.
		return blocksBelow;
	}
}
