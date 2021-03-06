package fr.inria.stamp;

import fr.inria.diversify.dspot.amplifier.TestDataMutator;
import fr.inria.diversify.dspot.selector.JacocoCoverageSelector;
import fr.inria.diversify.utils.AmplificationHelper;
import fr.inria.diversify.dspot.DSpot;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.diversify.utils.sosiefier.InputProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 2/9/17
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static boolean verbose = false;

    public static void main(String[] args) throws Exception {
        final Configuration configuration = JSAPOptions.parse(args);
        if (configuration == null) {
            Main.runExample();
        } else {
            run(configuration);
        }
    }

    public static void run(Configuration configuration) throws Exception {
        InputConfiguration inputConfiguration = new InputConfiguration(configuration.pathToConfigurationFile);
        AmplificationHelper.setSeedRandom(23L);
        InputProgram program = new InputProgram();
        inputConfiguration.setInputProgram(program);
        inputConfiguration.getProperties().setProperty("automaticBuilderName", configuration.automaticBuilderName);
        AmplificationHelper.MAX_NUMBER_OF_TESTS = configuration.maxTestAmplified;
        if (configuration.mavenHome != null) {
            inputConfiguration.getProperties().put("maven.home", configuration.mavenHome);
        }
        if (configuration.pathToOutput != null) {
            inputConfiguration.getProperties().setProperty("outputDirectory", configuration.pathToOutput);
        }
        DSpot dspot = new DSpot(inputConfiguration, configuration.nbIteration, configuration.amplifiers, configuration.selector);

        AmplificationHelper.setSeedRandom(configuration.seed);
        AmplificationHelper.setTimeOutInMs(configuration.timeOutInMs);

        createOutputDirectories(inputConfiguration);
        if ("all".equals(configuration.testClasses.get(0))) {
            amplifyAll(dspot, inputConfiguration);
        } else {
            configuration.testClasses.forEach(testCase -> {
                if (!configuration.namesOfTestCases.isEmpty()) {
                    amplifyOne(dspot, testCase, configuration.namesOfTestCases);
                } else {
                    amplifyOne(dspot, testCase, Collections.emptyList());
                }
            });
        }
    }

    private static void createOutputDirectories(InputConfiguration inputConfiguration) {
        if (!new File(inputConfiguration.getOutputDirectory()).exists()) {

            String[] paths = inputConfiguration.getOutputDirectory().split("/");
            if (!new File(paths[0]).exists()) {
                new File(paths[0]).mkdir();
            }
            new File(inputConfiguration.getOutputDirectory()).mkdir();
        }
    }

    private static void amplifyOne(DSpot dspot, String fullQualifiedNameTestClass, List<String> testCases) {
        long time = System.currentTimeMillis();
        try {
            if (testCases.isEmpty()) {
                dspot.amplifyTest(fullQualifiedNameTestClass);
            } else {
                dspot.amplifyTest(fullQualifiedNameTestClass, testCases);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - time + " ms");
    }

    private static void amplifyAll(DSpot dspot, InputConfiguration configuration) {
        long time = System.currentTimeMillis();
        final File outputDirectory = new File(configuration.getOutputDirectory() + "/");
        if (!outputDirectory.exists()) {
            if (!new File("results").exists()) {
                new File("results").mkdir();
            }
            if (!outputDirectory.exists()) {
                outputDirectory.mkdir();
            }
        }
        try {
            dspot.amplifyAllTests();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - time + " ms");
    }

    static void runExample() {
        try {
            InputConfiguration configuration = new InputConfiguration("src/test/resources/test-projects/test-projects.properties");
            DSpot dSpot = new DSpot(configuration,
                    1,
                    Collections.singletonList(new TestDataMutator()),
                    new JacocoCoverageSelector()
            );
            dSpot.amplifyTest("example.TestSuiteExample");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}