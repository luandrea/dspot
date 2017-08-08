package fr.inria.stamp.coverage;

import fr.inria.diversify.runner.InputConfiguration;
import fr.inria.diversify.runner.InputProgram;
import fr.inria.stamp.test.launcher.TestLauncher;
import fr.inria.stamp.test.listener.TestListener;
import fr.inria.stamp.test.runner.DefaultTestRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static java.util.ResourceBundle.clearCache;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 13/07/17
 */
public class JacocoExecutor {

	private MemoryClassLoader internalClassLoader;

	private IRuntime runtime;

	private Instrumenter instrumenter;

	private InputProgram program;

	private InputConfiguration configuration;

	public JacocoExecutor(InputProgram program, InputConfiguration configuration) {
		this.configuration = configuration;
		this.program = program;
		this.runtime = new LoggerRuntime();
		this.instrumenter = new Instrumenter(this.runtime);
		this.instrumentAll();
	}

	private void instrumentAll() {
		final String classesDirectory = this.program.getProgramDir() + "/" + this.program.getClassesDir();
		try {
			this.internalClassLoader = new MemoryClassLoader(
					new URL[]{new File(classesDirectory).toURI().toURL()},
					ClassLoader.getSystemClassLoader()
			);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		/* instrument all of them */
		final Iterator<File> iterator = FileUtils.iterateFiles(new File(classesDirectory), new String[]{"class"}, true);
		while (iterator.hasNext()) {
			final File next = iterator.next();
			final String fileName = next.getPath().substring(classesDirectory.length());
			final String fullQualifiedName = fileName.replaceAll("/", ".").substring(0, fileName.length() - ".class".length());
			try {
				this.internalClassLoader.addDefinition(fullQualifiedName,
						this.instrumenter.instrument(this.internalClassLoader.getResourceAsStream(fileName), fullQualifiedName));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		clearCache(this.internalClassLoader);
	}

	public CoverageResults executeJacoco(CtType<?> testClass) {
		final String testClassesDirectory = this.program.getProgramDir() + "/" + this.program.getTestClassesDir();
		final RuntimeData data = new RuntimeData();
		final ExecutionDataStore executionData = new ExecutionDataStore();
		final SessionInfoStore sessionInfos = new SessionInfoStore();
		URLClassLoader classLoader;
		try {
			classLoader = new URLClassLoader(new URL[]
					{new File(testClassesDirectory).toURI().toURL()}, this.internalClassLoader);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		final String resource = testClass.getQualifiedName().replace('.', '/') + ".class";
		try {
			this.internalClassLoader.addDefinition(
					testClass.getQualifiedName(),
					IOUtils.toByteArray(classLoader.getResourceAsStream(resource))
			);
			runtime.startup(data);
			final TestListener listener = TestLauncher.run(this.configuration, this.internalClassLoader, testClass);
			data.collect(executionData, sessionInfos, false);
			runtime.shutdown();
			clearCache(this.internalClassLoader);
			return coverageResults(executionData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public Map<String, CoverageResults> executeJacoco(CtType<?> testClass, Collection<String> methodNames) {
		final String testClassesDirectory = this.program.getProgramDir() + "/" + this.program.getTestClassesDir();
		final String classesDirectory = this.program.getProgramDir() + "/" + this.program.getClassesDir();
		URLClassLoader classLoader;
		try {
			classLoader = new URLClassLoader(new URL[]
					{new File(testClassesDirectory).toURI().toURL()}, this.internalClassLoader);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		final String resource = testClass.getQualifiedName().replace('.', '/') + ".class";
		try {
			this.internalClassLoader.addDefinition(
					testClass.getQualifiedName(),
					IOUtils.toByteArray(classLoader.getResourceAsStream(resource))
			);
			final RuntimeData data = new RuntimeData();
			this.runtime.startup(data);
			final JacocoListener jacocoListener = new JacocoListener(data, classesDirectory);
			TestLauncher.run(this.configuration, this.internalClassLoader, testClass, methodNames, jacocoListener);
			this.runtime.shutdown();
			clearCache(this.internalClassLoader);
			return jacocoListener.getCoverageResultsMap();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private CoverageResults coverageResults(ExecutionDataStore executionData) {
		final String classesDirectory = this.program.getProgramDir() + "/" + this.program.getClassesDir();
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

		try {
			analyzer.analyzeAll(new File(classesDirectory));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new CoverageResults(coverageBuilder);
	}

}