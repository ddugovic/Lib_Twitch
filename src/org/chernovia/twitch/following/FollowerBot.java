package org.chernovia.twitch.following;

import java.util.*;

import org.chernovia.twitch.gson.Follower;

public class FollowerBot extends Thread {

	boolean preview = true;
	long interval = 30000;
	String channel; FollowerListener listener; boolean RUNNING = false;
	Vector<String> followerList;
	
	public FollowerBot(String chan,FollowerListener l) {
		channel = chan; listener = l;
		followerList = new Vector<String>();
		List<Follower> followers = FollowerAlert.getLastFollows(channel,24);
		int i=0;
		for (Follower f: followers) {
			followerList.add(f.user.name);
			Calendar date = 
			javax.xml.bind.DatatypeConverter.parseDateTime(f.created_at);
			System.out.println(++i + ". " + f.user.name + ": " + date.getTime());
		}
		System.out.println("***");
		//if (preview) listener.newFollower(f);
	}
	
	@Override
	public void run() {
		RUNNING = true;
		while (RUNNING) {
			try { sleep(interval); }
			catch (InterruptedException ignore) {}
			List<Follower> followers = FollowerAlert.getLastFollows(channel,6);
			for (Follower f: followers) {
				Calendar date = 
				javax.xml.bind.DatatypeConverter.parseDateTime(f.created_at);
				System.out.println(f.user.name + ": " + date.getTime());
				if (!followerList.contains(f.user.name)) {
					followerList.add(f.user.name); listener.newFollower(f);
				}
			}
			System.out.println("*** " + followerList.size() + " followers ***");
		}
	}
}