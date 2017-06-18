package org.chernovia.twitch.gson;

import java.util.List;

public class Followers {
    private List<Follower> follows;
    public int _total;
    public NextLink _links;
    public List<Follower> getFollows() { return follows; }
}