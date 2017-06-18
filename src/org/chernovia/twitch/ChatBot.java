package org.chernovia.twitch;

import org.jibble.pircbot.PircBot;

public class ChatBot extends PircBot {
	
	ChatListener listener;
	String irc_chan;
	
	public ChatBot(String name, String host, String auth, String chan, ChatListener l) {
		listener = l;
		setName(name);
        setVerbose(false);
        try { connect(host,6667,auth); }
        catch (Exception augh) { augh.printStackTrace(); }
        irc_chan = chan; joinChannel(irc_chan);
        setVerbose(true);
	}
	
	public void onMessage(String channel, String sender, 
	String login, String hostname, String message) {
		listener.newMessage(channel, sender, login, hostname, message);
	}

}
