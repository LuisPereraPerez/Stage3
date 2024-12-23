package com.example;

import com.example.control.HazelcastManager;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Map;

public class HazelcastVerifier {
    public static void main(String[] args) {
        HazelcastInstance instance = HazelcastManager.getInstance();

        IMap<Integer, String> processedBooks = instance.getMap("processedBooks");
        IMap<Integer, Map<String, String>> metadataMap = instance.getMap("metadataMap");

        System.out.println("Processed Books:");
        processedBooks.forEach((id, content) -> System.out.println("ID: " + id + "\nContent: " + content));

        System.out.println("Metadata:");
        metadataMap.forEach((id, metadata) -> System.out.println("ID: " + id + "\nMetadata: " + metadata));
    }
}