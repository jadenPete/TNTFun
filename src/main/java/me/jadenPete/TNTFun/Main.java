package me.jadenPete.TNTFun;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/*
 * This class is responsible for:
 *   - Starting and stopping the plugin.
 *     
 * It does not do anything on it's own.
 * It's sole purpose is to manage the plugin
 * and setup the Events class to operate.
 */

public class Main extends JavaPlugin {
	FileConfiguration config = getConfig();
	
	// Fired when the plugin is first enabled.
	@Override
	public void onEnable(){
		// If the configuration file doesn't exist, copy the default one.
		saveDefaultConfig();

		// Run the Util class constructor, which uses an
		// instance of the main class to access non-static methods.
		new Util(this);
		
		// Initialize the main game region
		Util.registerRegion();
		
		// Handle events in the Events class
		getServer().getPluginManager().registerEvents(new Events(), this);
	}
	
	// Fired when the plugin is disabled.
	// Empty because there really isn't anything to do.
	@Override
	public void onDisable(){
		
	}
}
