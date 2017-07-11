package me.jadenPete.TNTFun;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/*
 * Found this on https://bukkit.org/threads/send-hotbar-messages.440664/,
 * since there STILL isn't an API to send hotbar messages to players.
 */

public class HotbarMessager {
	/**
	 * These are the Class instances. Use these to get fields or methods for
	 * classes.
	 */
	private static Class<?> CRAFTPLAYERCLASS, PACKET_PLAYER_CHAT_CLASS, ICHATCOMP, CHATMESSAGE, PACKET_CLASS;

	/**
	 * These are the constructors for those classes. You need these to create new
	 * objects.
	 */
	private static Constructor<?> PACKET_PLAYER_CHAT_CONSTRUCTOR, CHATMESSAGE_CONSTRUCTOR;

	/**
	 * This is the server version. This is how we know the server version.
	 */
	private static final String SERVER_VERSION;
	static {
		/**
		 * This gets the server version.
		 */
		String name = Bukkit.getServer().getClass().getName();
		name = name.substring(name.indexOf("craftbukkit.") + "craftbukkit.".length());
		name = name.substring(0, name.indexOf("."));

		SERVER_VERSION = name;

		try {
			/**
			 * This here sets the class fields.
			 */
			CRAFTPLAYERCLASS = Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION + ".entity.CraftPlayer");
			PACKET_PLAYER_CHAT_CLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".PacketPlayOutChat");
			PACKET_CLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".Packet");
			ICHATCOMP = Class.forName("net.minecraft.server." + SERVER_VERSION + ".IChatBaseComponent");
			PACKET_PLAYER_CHAT_CONSTRUCTOR = PACKET_PLAYER_CHAT_CLASS.getConstructor(ICHATCOMP, byte.class);
			CHATMESSAGE = Class.forName("net.minecraft.server." + SERVER_VERSION + ".ChatMessage");
			CHATMESSAGE_CONSTRUCTOR = CHATMESSAGE.getDeclaredConstructor(String.class, Object[].class);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Sends the hotbar message 'message' to the player 'player'
	 *
	 * @param player
	 * @param message
	 */
	public static void sendHotBarMessage(Player player, String message){
		try {
			// This creates the IChatComponentBase instance
			Object icb = CHATMESSAGE_CONSTRUCTOR.newInstance(message, new Object[0]);

			// This creates the packet
			Object packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb, (byte) 2);

			// This casts the player to a craftplayer
			Object craftplayerInst = CRAFTPLAYERCLASS.cast(player);

			// This get's the method for craftplayer's handle
			Method methodHandle = CRAFTPLAYERCLASS.getMethod("getHandle");

			// This invokes the method above.
			Object methodhHandle = methodHandle.invoke(craftplayerInst);

			// This gets the player's connection
			Object playerConnection = methodhHandle.getClass().getField("playerConnection").get(methodhHandle);

			// This sends the packet.
			playerConnection.getClass().getMethod("sendPacket", PACKET_CLASS).invoke(playerConnection, packet);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
