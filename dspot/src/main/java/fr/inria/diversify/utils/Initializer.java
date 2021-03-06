package fr.inria.diversify.utils;

import fr.inria.diversify.automaticbuilder.AutomaticBuilder;
import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.diversify.dspot.support.DSpotCompiler;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.diversify.utils.sosiefier.InputProgram;
import fr.inria.diversify.utils.sosiefier.ProcessorUtil;
import org.apache.commons.io.FileUtils;
import spoon.Launcher;
import spoon.SpoonModelBuilder;

import java.io.File;
import java.io.IOException;

import static fr.inria.diversify.utils.AmplificationHelper.PATH_SEPARATOR;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 15/09/17
 */
public class Initializer {

	//TODO the BranchCoverageSelector will be removed and replaced by Jacoco. The boolean will be removed at this moment.
	public static void initialize(InputConfiguration configuration, @Deprecated boolean isBranchCoverageSelector)
			throws IOException, InterruptedException {
		AutomaticBuilderFactory.reset();
		InputProgram program = InputConfiguration.initInputProgram(configuration);
		program.setProgramDir(DSpotUtils.computeProgramDirectory.apply(configuration));
		configuration.setInputProgram(program);
		AutomaticBuilder builder = AutomaticBuilderFactory.getAutomaticBuilder(configuration);
		String dependencies = builder.buildClasspath(program.getProgramDir());
		dependencies += PATH_SEPARATOR + "target/dspot/dependencies/";

		if (configuration.getProperty("additionalClasspathElements") != null) {
			dependencies += PATH_SEPARATOR + program.getProgramDir() + configuration.getProperty("additionalClasspathElements");
		}

		File output = new File(program.getProgramDir() + "/" + program.getClassesDir());
		File outputTest = new File(program.getProgramDir() + "/" + program.getTestClassesDir());
		try {
			FileUtils.cleanDirectory(output);
			FileUtils.cleanDirectory(outputTest);
		} catch (Exception ignored) {
			//the target directory does not exist, do not need to clean it
		}

		boolean status;
		//We need to use separate factory here, because the BranchProcessor will process test also
		//TODO this is used only with the BranchCoverageSelector
		if (isBranchCoverageSelector) {
			Launcher spoonModel = DSpotCompiler.getSpoonModelOf(program.getAbsoluteSourceCodeDir(), dependencies);
			DSpotUtils.addBranchLogger(program, spoonModel.getFactory());
			ProcessorUtil.writeInfoFile(program.getProgramDir());
			SpoonModelBuilder modelBuilder = spoonModel.getModelBuilder();
			modelBuilder.setBinaryOutputDirectory(output);
			status = modelBuilder.compile(SpoonModelBuilder.InputType.CTTYPES);
		} else {
			status = DSpotCompiler.compile(program.getAbsoluteSourceCodeDir(), dependencies, output);
		}
		boolean statusTest = DSpotCompiler.compile(program.getAbsoluteTestSourceCodeDir(),
				output.getAbsolutePath() + PATH_SEPARATOR + dependencies, outputTest);

		DSpotUtils.copyResources(configuration);

		if (! (status && statusTest)) {
			throw new RuntimeException("Error during compilation");
		}
	}

	public static void compileTest(InputConfiguration configuration) {
		InputProgram program = configuration.getInputProgram();
		String dependencies = AutomaticBuilderFactory.getAutomaticBuilder(configuration)
				.buildClasspath(program.getProgramDir());
		dependencies += PATH_SEPARATOR + "target/dspot/dependencies/";
		File output = new File(program.getProgramDir() + "/" + program.getClassesDir());
		File outputTest = new File(program.getProgramDir() + "/" + program.getTestClassesDir());
		try {
			FileUtils.cleanDirectory(outputTest);
		} catch (Exception ignored) {
			//the target directory does not exist, do not need to clean it
		}
		boolean statusTest = DSpotCompiler.compile(program.getAbsoluteTestSourceCodeDir(),
				output.getAbsolutePath() + PATH_SEPARATOR + dependencies, outputTest);
		if (!statusTest) {
			throw new RuntimeException("Error during compilation");
		}
	}





}
