package org.kambanaria.spoonerism;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.gui.SpoonModelTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

class Spoonerism {
    String IN_DIR = "src/test/java/";
    String OUT_DIR = "src/out/java/";
    // Main API
    SpoonAPI spoonUniverse;
    // place to put temporary results between invocations
    HashSet<CtClass> testingClasses;
    String baseTestingClassPackage;
    CtClass<?> baseTestingClass;

    Spoonerism readClasses() {
        // Command line launcher
        // others for maven, gradle
        spoonUniverse = new Launcher();

        // call it many times if needed
        spoonUniverse.addInputResource(IN_DIR);

        // build and wait
        spoonUniverse.buildModel();
        return this;
    }

    Spoonerism display() {
        // ↓ bad
        // System.out.println(spoonUniverse);

        // ↓ moderately good
        SpoonModelTree tree = new SpoonModelTree(
                spoonUniverse.getFactory());

        /* You get the same via command line:
        java -jar spoon-core-…-with-dependencies.jar -g -i src/test/java
        */
        return this;
    }

    Spoonerism enumerateTestingClasses() {
        TypeFilter<CtClass> isClass =
                new TypeFilter<CtClass>(CtClass.class);

        Factory factory = spoonUniverse.getFactory();
        CtTypeReference<?> juTestRef =
                factory.Type() .createReference("org.junit.Test");
        // can also use class literal
        // factory.Type().createReference(org.junit.Test.class);

        TypeFilter<CtClass> isWithTestAnnotatedMethods =
                new TypeFilter<CtClass>(CtClass.class) {
                    @Override
                    public boolean matches(CtClass ctClass) {
                        return super.matches(ctClass) &&
                                !ctClass
                                    .getMethodsAnnotatedWith(juTestRef)
                                    .isEmpty();
                    }
                };

        TypeFilter<CtClass> isRealTestingClass = new TypeFilter<CtClass>(CtClass.class) {
            @Override
            public boolean matches(CtClass ctClass) {
                if (!super.matches(ctClass)) {
                    return false;
                }
                CtTypeReference<?> current = ctClass.getReference();
                do {
                    if (!current.getTypeDeclaration()
                            .getMethodsAnnotatedWith(juTestRef).isEmpty()) {
                        return true;
                    }
                } while ((current = current.getSuperclass()) != null);
                return false;
            }
        };
        testingClasses = new HashSet<>(spoonUniverse.getModel().getRootPackage()
                .getElements(isRealTestingClass));
        return this;
    }

    Spoonerism determineBaseTestingClassPackage(){
        // What is the name of the new superclass? BaseTest
        // What is the package of the new superclass?
        CtClass firstClass = testingClasses.iterator().next();
        String qualifiedName = firstClass.getPackage().getQualifiedName();
        List<String> commonComponents = Arrays.asList(
                qualifiedName.split("[.]"));
        for (CtClass ctClass:testingClasses) {
            List<String> currentComponents = Arrays.asList(
                    ctClass.getPackage().getQualifiedName().split("[.]"));
            int max = Math.min(currentComponents.size(), commonComponents.size());
            for (int i = 0; i < max; i++ ) {
                if (!currentComponents.get(i).equals(commonComponents.get(i))) {
                    commonComponents = commonComponents.subList(0, i);
                    break;
                }
            }
        }
        baseTestingClassPackage = String.join(".", commonComponents);
        return this;
    }

    Spoonerism createBaseTestingClass() {
        String baseClassFqn = baseTestingClassPackage + ".BaseTest";
        baseTestingClass = spoonUniverse.getFactory().createClass(baseClassFqn);
        return this;
    }

    Spoonerism extendTestingClasses() {
        CtTypeReference<?> baseTestingClassRef =
                baseTestingClass.getReference();
        for (CtClass ctClass: testingClasses) {
            CtTypeReference<?> superClass = ctClass.getSuperclass();
            if (ctClass.getSuperclass() == null) {
                ctClass.setSuperclass(baseTestingClassRef);
            }
        }
        return this;
    }

    Spoonerism writeTransformedClasses () {
        Environment env = spoonUniverse.getEnvironment();
        env.setAutoImports(true);
        env.setCommentEnabled(true);

        spoonUniverse.setSourceOutputDirectory(OUT_DIR);
        spoonUniverse.prettyprint();
        return this;
    }
}
