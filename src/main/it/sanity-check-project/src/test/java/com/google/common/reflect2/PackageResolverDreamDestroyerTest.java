package com.google.common.reflect2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PackageResolverDreamDestroyerTest {

    @Test
    public void testMainDreamDestroyer(){
        PackageResolveDreamDestroyer PRDD = new PackageResolveDreamDestroyer();
        PRDD.main(new String[0]);
    }
}
