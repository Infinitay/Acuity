package com.acuitybotting.db.arango.utils;

import com.arangodb.springframework.repository.ArangoRepository;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 6/29/2018.
 */
public class ArangoUtils {

    public static void saveAll(ArangoRepository repository, int size, Collection<?> collection){
        final AtomicInteger counter = new AtomicInteger(0);
        collection.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size)).values().parallelStream().forEach(repository::saveAll);
    }
}
