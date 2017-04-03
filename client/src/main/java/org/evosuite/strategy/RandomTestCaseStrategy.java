/**
 * Copyright (C) 2010-2016 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.strategy;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.stoppingconditions.StoppingCondition;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.statistics.StatisticsSender;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Iteratively generate random tests. If adding the random test leads to improved fitness, keep it,
 * otherwise drop it again.
 * 
 * @author Gordon Fraser
 */
public class RandomTestCaseStrategy extends RandomStrategy {

  private static final Logger logger = LoggerFactory.getLogger(RandomTestCaseStrategy.class);

  @SuppressWarnings("unchecked")
  @Override
  protected TestSuiteChromosome generateRandom() {
    LoggingUtils.getEvoLogger().info("* Using random test case generation");

    List<TestSuiteFitnessFunction> fitnessFunctions = this.getFitnessFunctions();
    StoppingCondition stoppingCondition = this.getStoppingCondition();

    ChromosomeFactory<TestChromosome> factory =
        (ChromosomeFactory<TestChromosome>) getChromosomeFactory();

    TestSuiteChromosome suite = this.setUpSuiteChromosome(fitnessFunctions);

    ClientServices.getInstance().getClientNode().changeState(ClientState.SEARCH);
    while (!isFinished(suite, stoppingCondition)) {
      TestChromosome test = factory.getChromosome();
      TestSuiteChromosome clone = suite.clone();
      clone.addTest(test);

      for (TestSuiteFitnessFunction fitness_function : fitnessFunctions) {
        // evaluate augmented test suite
        fitness_function.getFitness(clone);
      }
      logger.debug("Old fitness: {}, new fitness: {}", suite.getFitness(), clone.getFitness());

      if (clone.compareTo(suite) < 0) {
        suite = clone;
        StatisticsSender.executedAndThenSendIndividualToMaster(clone);
      }
    }

    LoggingUtils.getEvoLogger().info("* Search Budget:");
    LoggingUtils.getEvoLogger().info("\t- " + stoppingCondition.toString());

    return suite;
  }
}
