package com.google.common.reflect2;

import org.junit.jupiter.api.Test;

public class PackageResolutionClassTest {

    @Test
    public void testMainDreamDestroyer() {
        PackageResolutionClass PRC = new PackageResolutionClass();
        PRC.main(new String[0]);
    }
}
