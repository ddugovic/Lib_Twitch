package org.chernovia.twitch.following;

import org.chernovia.twitch.gson.Follower;

public interface FollowerListener {
	public void newFollower(Follower f);
}
