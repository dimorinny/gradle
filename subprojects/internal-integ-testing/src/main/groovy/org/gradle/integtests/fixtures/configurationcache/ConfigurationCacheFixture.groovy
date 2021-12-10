/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.fixtures.configurationcache

import org.gradle.configuration.ApplyScriptPluginBuildOperationType
import org.gradle.configuration.project.ConfigureProjectBuildOperationType
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.BuildOperationsFixture
import org.gradle.integtests.fixtures.executer.ExecutionFailure

class ConfigurationCacheFixture {
    static final String CONFIGURATION_CACHE_MESSAGE = "Configuration cache is an incubating feature."
    static final String ISOLATED_PROJECTS_MESSAGE = "Isolated projects is an incubating feature."
    static final String CONFIGURE_ON_DEMAND_MESSAGE = "Configuration on demand is an incubating feature."

    private final AbstractIntegrationSpec spec
    final BuildOperationsFixture buildOperations
    final ConfigurationCacheBuildOperationsFixture configurationCacheBuildOperations
    final ConfigurationCacheProblemsFixture problems

    ConfigurationCacheFixture(AbstractIntegrationSpec spec) {
        this.spec = spec
        buildOperations = new BuildOperationsFixture(spec.executer, spec.temporaryFolder)
        configurationCacheBuildOperations = new ConfigurationCacheBuildOperationsFixture(buildOperations)
        problems = new ConfigurationCacheProblemsFixture(spec.executer, spec.testDirectory)
    }

    /**
     * Asserts that the configuration cache was not enabled.
     */
    void assertNotEnabled() {
        spec.outputDoesNotContain(CONFIGURATION_CACHE_MESSAGE)
        spec.outputDoesNotContain(ISOLATED_PROJECTS_MESSAGE)
        spec.outputDoesNotContain(CONFIGURE_ON_DEMAND_MESSAGE)
        configurationCacheBuildOperations.assertNoConfigurationCache()
        assertHasNoProblems()
    }

    /**
     * Asserts that the cache entry was written with no problems.
     *
     * Also asserts that the appropriate console logging, reports and build operations are generated.
     */
    void assertStateStored(@DelegatesTo(StoreDetails) Closure closure = {}) {
        def details = new StoreDetails()
        closure.delegate = details
        closure()

        assertStateStored(details)
        assertHasWarningThatIncubatingFeatureUsed()
    }

    void assertStateStored(HasBuildActions details) {
        assertHasStoreReason(details)
        configurationCacheBuildOperations.assertStateStored()

        spec.postBuildOutputContains("Configuration cache entry stored.")

        assertHasNoProblems()
    }

    /**
     * Asserts that the cache entry was stored with the given problems.
     *
     * Also asserts that the appropriate console logging, reports and build operations are generated.
     */
    void assertStateStoredWithProblems(@DelegatesTo(StoreWithProblemsDetails) Closure closure) {
        def details = new StoreWithProblemsDetails()
        closure.delegate = details
        closure()

        assertStateStoredWithProblems(details, details)
        assertHasWarningThatIncubatingFeatureUsed()
    }

    void assertStateStoredWithProblems(HasBuildActions details, HasProblems problemDetails) {
        assertHasStoreReason(details)
        configurationCacheBuildOperations.assertStateStored()

        spec.result.assertHasPostBuildOutput("Configuration cache entry stored with ${details.problemsString}.")

        assertHasProblems(problemDetails)
    }

    /**
     * Asserts that the cache entry was stored but discarded with the given problems.
     *
     * Also asserts that the appropriate console logging, reports and build operations are generated.
     */
    void assertStateStoredAndDiscarded(@DelegatesTo(StoreWithProblemsDetails) Closure closure) {
        def details = new StoreWithProblemsDetails()
        closure.delegate = details
        closure()

        assertStateStoredAndDiscarded(details, details)
        assertHasWarningThatIncubatingFeatureUsed()
    }

    void assertStateStoredAndDiscarded(HasBuildActions details, HasProblems problemDetails) {
        assertHasStoreReason(details)
        configurationCacheBuildOperations.assertStateStored()

        def totalProblems = problemDetails.totalProblems
        def message
        if (totalProblems == 1) {
            message = "Configuration cache entry discarded with 1 problem."
        } else {
            message = "Configuration cache entry discarded with ${totalProblems} problems."
        }
        boolean isFailure = spec.result instanceof ExecutionFailure
        if (isFailure) {
            spec.outputContains(message)
        } else {
            spec.postBuildOutputContains(message)
        }

        assertHasProblems(problemDetails)
    }

    /**
     * Asserts that the cache entry was discarded due to some input change and stored again with no problems.
     *
     * Also asserts that the expected set of projects is configured, the expected models are queried
     * and the appropriate console logging, reports and build operations are generated.
     */
    void assertStateRecreated(@DelegatesTo(StoreRecreateDetails) Closure closure) {
        def details = new StoreRecreateDetails()
        closure.delegate = details
        closure()

        assertStateRecreated(details, details)
    }

    void assertStateRecreated(HasBuildActions details, HasInvalidationReason invalidationDetails) {
        assertHasRecreateReason(details, invalidationDetails)
        configurationCacheBuildOperations.assertStateStored()
        assertHasNoProblems()
    }

    /**
     * Asserts that the cache entry was loaded and used with no problems.
     *
     * Also asserts that the appropriate console logging, reports and build operations are generated.
     */
    void assertStateLoaded() {
        assertStateLoaded(new LoadDetails())
        assertHasWarningThatIncubatingFeatureUsed()
    }

    void assertStateLoaded(LoadDetails details) {
        spec.outputContains("Reusing configuration cache.")
        spec.postBuildOutputContains("Configuration cache entry reused.")

        configurationCacheBuildOperations.assertStateLoaded()

        assertNothingConfigured()

        assertHasNoProblems()
    }

    /**
     * Asserts that the cache entry was loaded and used with the given problems.
     *
     * Also asserts that the appropriate console logging, reports and build operations are generated.
     */
    void assertStateLoadedWithProblems(@DelegatesTo(LoadWithProblemsDetails) Closure closure) {
        def details = new LoadWithProblemsDetails()
        closure.delegate = details
        closure()

        assertHasWarningThatIncubatingFeatureUsed()
        spec.outputContains("Reusing configuration cache.")
        spec.postBuildOutputContains("Configuration cache entry reused with ${details.problemsString}.")

        configurationCacheBuildOperations.assertStateLoaded()

        assertNothingConfigured()

        assertHasProblems(details)
    }

    private void assertHasProblems(HasProblems problemDetails) {
        boolean isFailure = spec.result instanceof ExecutionFailure
        if (isFailure) {
            problems.assertFailureHasProblems(spec.failure) {
                applyProblemsTo(problemDetails, delegate)
            }
        } else {
            problems.assertResultHasProblems(spec.result) {
                applyProblemsTo(problemDetails, delegate)
            }
        }
    }

    private void applyProblemsTo(HasProblems details, HasConfigurationCacheProblemsSpec spec) {
        spec.withTotalProblemsCount(details.totalProblems)
        spec.problemsWithStackTraceCount = details.problemsWithStackTrace
        spec.withUniqueProblems(details.problems.collect {
            it.message.replace('/', File.separator)
        })
    }

    private assertHasNoProblems() {
        problems.assertResultHasProblems(spec.result) {
        }
    }

    private void assertHasWarningThatIncubatingFeatureUsed() {
        spec.outputContains(CONFIGURATION_CACHE_MESSAGE)
        spec.outputDoesNotContain(ISOLATED_PROJECTS_MESSAGE)
        spec.outputDoesNotContain(CONFIGURE_ON_DEMAND_MESSAGE)
    }

    private void assertHasStoreReason(HasBuildActions details) {
        if (details.runsTasks) {
            spec.outputContains("Calculating task graph as no configuration cache is available for tasks:")
        } else {
            spec.outputContains("Creating tooling model as no configuration cache is available for the requested model")
        }
    }

    private void assertHasRecreateReason(HasBuildActions details, HasInvalidationReason invalidationDetails) {
        // Inputs can be discovered in parallel, so require that any one of the changed inputs is reported

        def reasons = []
        invalidationDetails.changedFiles.each { file ->
            reasons.add("file '${file.replace('/', File.separator)}'")
        }
        if (invalidationDetails.changedGradleProperty) {
            reasons.add("the set of Gradle properties")
        }
        if (invalidationDetails.changedSystemProperty != null) {
            reasons.add("system property '$invalidationDetails.changedSystemProperty'")
        }
        if (invalidationDetails.changedTask != null) {
            reasons.add("an input to task '${invalidationDetails.changedTask}'")
        }

        def messages = reasons.collect { reason ->
            if (details.runsTasks) {
                "Calculating task graph as configuration cache cannot be reused because $reason has changed."
            } else {
                "Creating tooling model as configuration cache cannot be reused because $reason has changed."
            }
        }

        def found = messages.any { message -> spec.output.contains(message) }
        assert found: "could not find expected invalidation reason in output. expected: ${messages}"
    }

    private void assertNothingConfigured() {
        def configuredProjects = buildOperations.all(ConfigureProjectBuildOperationType)
        // A synthetic "project configured" operation is fired for each root project for build scans
        assert configuredProjects.every { it.details.projectPath == ':' }

        def scripts = buildOperations.all(ApplyScriptPluginBuildOperationType)
        assert scripts.empty
    }

    static class ProblemDetails {
        final String message
        final int count
        final boolean hasStackTrace

        ProblemDetails(String message, int count, boolean hasStackTrace) {
            this.message = message
            this.count = count
            this.hasStackTrace = hasStackTrace
        }
    }

    trait HasProblems {
        final List<ProblemDetails> problems = []

        void problem(String message, int count = 1, boolean hasStackTrace = true) {
            problems.add(new ProblemDetails(message, count, hasStackTrace))
        }

        void serializationProblem(String message, int count = 1) {
            problems.add(new ProblemDetails(message, count, false))
        }

        int getTotalProblems() {
            return problems.inject(0) { a, b -> a + b.count }
        }

        int getProblemsWithStackTrace() {
            return problems.inject(0) { a, b -> a + (b.hasStackTrace ? b.count : 0) }
        }

        String getProblemsString() {
            def count = totalProblems
            return count == 1 ? "1 problem" : "$count problems"
        }
    }

    trait HasBuildActions {
        boolean runsTasks = true
    }

    trait HasInvalidationReason {
        List<String> changedFiles = []
        boolean changedGradleProperty
        String changedSystemProperty
        String changedTask

        void fileChanged(String name) {
            changedFiles.add(name)
        }

        void taskInputChanged(String name) {
            changedTask = name
        }

        void gradlePropertyChanged() {
            changedGradleProperty = true
        }

        void systemPropertyChanged(String name) {
            changedSystemProperty = name
        }
    }

    static class StoreDetails implements HasBuildActions {
    }

    static class StoreWithProblemsDetails extends StoreDetails implements HasProblems {
    }

    static class StoreRecreateDetails extends StoreDetails implements HasInvalidationReason {
    }

    static class LoadDetails {
    }

    static class LoadWithProblemsDetails extends LoadDetails implements HasProblems {
    }
}
