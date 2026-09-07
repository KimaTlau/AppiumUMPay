package com.umpay.mobile.utility;

import io.cucumber.java.Scenario;

import java.util.List;

/**
 * What a failed scenario should say for itself.
 *
 * The suite already attached a picture of the screen the scenario died on. What it could
 * not answer was the question that follows: what do I do to see this myself. On a device
 * that matters more than it does on a desktop, because nobody can go back and look - the
 * session is gone and the app is closed.
 *
 * There is no section for what the server said. A mobile app's traffic does not pass
 * through anything this test can see, so collecting it would mean standing a proxy between
 * the device and the network. The Playwright web suites carry that section; this one says
 * plainly that it cannot, so a reader knows it was never collected.
 */
public final class FailureReport {

	private FailureReport() {
	}

	/** The whole account of a failure. */
	public static String of(Scenario scenario) {

		StringBuilder out = new StringBuilder();

		out.append("========================================================================\n")
				.append("FAILED: ").append(scenario.getName()).append('\n')
				.append("========================================================================\n\n");

		out.append(reproduction(scenario)).append('\n');

		out.append("SCREENSHOT\n")
				.append("  Attached to this scenario in the report, taken as the scenario left\n")
				.append("  the screen.\n\n");

		out.append("API CALLS\n")
				.append("  Not collected. A device's traffic does not pass through the test, so\n")
				.append("  this suite reports what the screen did and not what the server said.\n\n");

		out.append("RUN THIS ONE AGAIN\n")
				.append("  mvn test -Dcucumber.filter.name=\"").append(scenario.getName()).append("\"\n");

		return out.toString();
	}

	/** The steps of the scenario, as a person would follow them on the device. */
	private static String reproduction(Scenario scenario) {

		List<String> steps = FeatureSteps.of(scenario);

		StringBuilder out = new StringBuilder("STEPS TO REPRODUCE\n");

		out.append("  Feature: ")
				.append(scenario.getUri().getPath().replaceAll(".*/", ""))
				.append(", line ").append(scenario.getLine()).append("\n\n");

		if (steps.isEmpty()) {
			out.append("  (the feature file could not be read - see the console)\n");
			return out.toString();
		}

		int number = 1;

		for (String step : steps) {
			out.append("  ").append(number++).append(". ").append(step).append('\n');
		}

		return out.toString();
	}
}
