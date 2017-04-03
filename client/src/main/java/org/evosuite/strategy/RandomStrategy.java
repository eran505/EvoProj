package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.archive.ArchiveTestChromosomeFactory;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.stoppingconditions.MaxTestsStoppingCondition;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.factories.AllMethodsTestChromosomeFactory;
import org.evosuite.testcase.factories.JUnitTestCarvedChromosomeFactory;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.evosuite.testsuite.factories.TestSuiteChromosomeFactory;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author José Campos
 */
public abstract class RandomStrategy extends TestGenerationStrategy {

  @Override
  public TestSuiteChromosome generateTests() {

    if (!this.canGenerateTestsForSUT()) {
      LoggingUtils.getEvoLogger()
          .info("* Found no testable methods in the target class " + Properties.TARGET_CLASS);
      return new TestSuiteChromosome();
    }

    List<TestFitnessFactory<? extends TestFitnessFunction>> goalFactories = TestGenerationStrategy.getFitnessFactories();
    List<TestFitnessFunction> goals = new ArrayList<TestFitnessFunction>();
    LoggingUtils.getEvoLogger().info("* Total number of test goals: ");
    for (TestFitnessFactory<? extends TestFitnessFunction> goalFactory : goalFactories) {
      goals.addAll(goalFactory.getCoverageGoals());
      LoggingUtils.getEvoLogger()
          .info("  - " + goalFactory.getClass().getSimpleName().replace("CoverageFactory", "") + " "
              + goalFactory.getCoverageGoals().size());
    }
    ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals,
        goals.size());

    // randomly generate test cases/suites
    TestSuiteChromosome suite = this.generateRandom();

    // Search is finished, send statistics
    sendExecutionStatistics();

    // TODO: Check this: Fitness_Evaluations = getNumExecutedTests?
    ClientServices.getInstance().getClientNode().trackOutputVariable(
        RuntimeVariable.Fitness_Evaluations, MaxTestsStoppingCondition.getNumExecutedTests());

    return suite;
  }

  protected TestSuiteChromosome setUpSuiteChromosome(List<TestSuiteFitnessFunction> fitnessFunctions) {

    TestSuiteChromosome suite = new TestSuiteChromosome();
    for (TestSuiteFitnessFunction fitnessFunction : fitnessFunctions) {
      suite.addFitness(fitnessFunction); // add fitness function
      fitnessFunction.getFitness(suite); // evaluate fitness function
    }

    return suite;
  }

  /**
   * Randomly generate test case/suite.
   */
  protected abstract TestSuiteChromosome generateRandom();

  protected ChromosomeFactory<?> getChromosomeFactory() {
    ChromosomeFactory<TestChromosome> factory = null;
    switch (Properties.TEST_FACTORY) {
      case ALLMETHODS:
        factory = new AllMethodsTestChromosomeFactory();
        break;
      case RANDOM:
        factory = new RandomLengthTestFactory();
        break;
      case ARCHIVE:
        factory = new ArchiveTestChromosomeFactory();
        break;
      case JUNIT:
        factory = new JUnitTestCarvedChromosomeFactory(new RandomLengthTestFactory());
        break;
      default:
        throw new RuntimeException("Unsupported test factory: " + Properties.TEST_FACTORY);
    }

    switch (Properties.STRATEGY) {
      case RANDOM_TEST_CASE:
      case RANDOM_N_TEST_CASES:
        return factory;
      case RANDOM_TEST_SUITE:
        return new TestSuiteChromosomeFactory(factory);
      default:
        throw new RuntimeException("Unsupported random strategy: " + Properties.STRATEGY);
    }
  }
}
