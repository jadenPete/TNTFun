package me.jadenPete.TNTFun;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;

/*
 * This class is responsible for:
 *   - Managing the game through move, leave, and join events.
 *   - Managing the game by calling functions in Util.
 *   
 *   It requires the Util class to function.
 */

public class Events implements Listener {
	// When a player moves.
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		String name = player.getName();
		Location location = player.getLocation();
		
		// Check that the player is in the same world as the game.
		if(location.getWorld() == Util.world){
			// Check if the player is in the game region.
			if(Util.region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())){
				// If the player has just entered, teleport them to
				// the starting position and notify other players.
				if(!Util.players.contains(name)){
					List<Integer> mainCoords = Util.config.getIntegerList("regions.startPos");
					Location mainLocation = new Location(Util.world, mainCoords.get(0), mainCoords.get(1), mainCoords.get(2));
					
					mainLocation.setYaw(180);
					mainLocation.setPitch(0);
					
					// Add the player to the game.
					Util.players.add(name);
					Util.region.getMembers().addPlayer(name);
					player.teleport(mainLocation);
					
					// Notify other players.
					for(String p : Util.players){
						Bukkit.getPlayer(p).sendMessage(Util.config.getString("messages.player-joined")
														.replace("%p", name)
														.replace("%n", String.valueOf(Util.players.size()))
														.replace("%m", Util.config.getString("options.max-players")));
					}
					
					// If the game has enough players, start the countdown/initializer in a new thread.
					if(Util.players.size() == Util.config.getInt("options.min-players")){
						Util.countdownThread = new Thread(Util.countdown);
						Util.countdownThread.start();
					}
					
					// If the game has enough players, deny access to any more.
					if(Util.players.size() == Util.config.getInt("options.max-players")) {
						Util.region.setFlag(DefaultFlag.ENTRY, StateFlag.State.ALLOW);
						Util.region.setFlag(DefaultFlag.ENTRY.getRegionGroupFlag(), RegionGroup.MEMBERS);
					}
				// If the game is already started.
				} else if(Util.gameStarted){
					// Check if the player hasn't died.
					if(location.getBlockY() > 0){
						// Save some time and CPU by checking if the player is standing on a layer.
						if(location.getBlockY() - 1 == Util.config.getIntegerList("regions.layer1.vector2").get(1) ||
						   location.getBlockY() - 1 == Util.config.getIntegerList("regions.layer2.vector2").get(1) ||
						   location.getBlockY() - 1 == Util.config.getIntegerList("regions.layer3.vector2").get(1)){
							// Erase all the blocks beneath the player with a 1 second delay (20 ticks).
							for(Block block : Util.getBlocksBelow(player)){
								// Check that the block is TNT.
								if(block.getType() == Material.TNT){
									new BukkitRunnable(){
					                    @Override
					                    public void run(){
					    					block.setType(Material.AIR);
					    				}
					                }.runTaskLater(Util.plugin, 20);
								}
							}
						}
					// Otherwise remove them from the game and notify other players.
					} else {
						Util.players.remove(name);
						Util.region.getMembers().removePlayer(name);
						
						player.teleport(Util.world.getSpawnLocation());
						
						// If they were the last player, stop the game.
						if(Util.players.size() == 0){
							Util.resetGame(player);
						} else {
							player.sendMessage(Util.config.getString("messages.died"));
							
							for(String p : Util.players){
								Bukkit.getPlayer(p).sendMessage(Util.config.getString("messages.player-died").replace("%p", name));
							}
						}
					}
				}
			// Otherwise remove the player from the game.
			// (if they are in the game, of course).
			} else {
				Util.removePlayer(player);
			}
		}
	}
	
	// When a player joins.
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		Location location = player.getLocation();
		
		// If they are in the game region, teleport them back to the world spawn.
		if(player.getWorld() == Util.world && Util.region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())){			
			player.teleport(Util.world.getSpawnLocation());
		}
	}
	
	// When a player quits.
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		// Remove them from the game (if they are in the game, of course).
		Util.removePlayer(event.getPlayer());
	}
}
