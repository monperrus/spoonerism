package org.kambanaria.spoonerism;


public class Example {
    public static void main(String[] args) {
        new Spoonerism()
                .readClasses()
                .display()
                .enumerateTestingClasses()
                .determineBaseTestingClassPackage()
                .createBaseTestingClass()
                .extendTestingClasses()
                .writeTransformedClasses();
    }
}
