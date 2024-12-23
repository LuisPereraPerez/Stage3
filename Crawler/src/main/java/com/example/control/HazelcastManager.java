package com.example.control;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastManager {
    private static final HazelcastInstance instance = Hazelcast.newHazelcastInstance();

    public static HazelcastInstance getInstance() {
        return instance;
    }
}
