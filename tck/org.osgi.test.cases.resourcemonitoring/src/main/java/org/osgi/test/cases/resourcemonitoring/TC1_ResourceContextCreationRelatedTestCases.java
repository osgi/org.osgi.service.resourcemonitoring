/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.resourcemonitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.resourcemonitoring.ResourceContext;
import org.osgi.service.resourcemonitoring.ResourceContextEvent;
import org.osgi.service.resourcemonitoring.ResourceContextException;
import org.osgi.service.resourcemonitoring.ResourceMonitor;
import org.osgi.service.resourcemonitoring.ResourceMonitorFactory;
import org.osgi.service.resourcemonitoring.ResourceMonitoringService;
import org.osgi.test.cases.resourcemonitoring.utils.FakeResourceMonitor;
import org.osgi.test.cases.resourcemonitoring.utils.ResourceContextListenerTestImpl;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * @author $Id$
 */
public class TC1_ResourceContextCreationRelatedTestCases {

	@InjectBundleContext
	BundleContext		context;

	/**
	 * ResourceMonitoringService.
	 */
	private ResourceMonitoringService		resourceMonitoringService;

	/**
	 * resource context listener
	 */
	private ResourceContextListenerTestImpl	resourceContextListener;

	@BeforeEach
	public void setUp() throws Exception {

		ServiceReference<ResourceMonitoringService> serviceReference = context
				.getServiceReference(ResourceMonitoringService.class);
		resourceMonitoringService = context
				.getService(serviceReference);

		resourceContextListener = new ResourceContextListenerTestImpl();
		resourceContextListener.start(context);

		// delete all existing resource contexts.
		ResourceContext[] resourceContexts = resourceMonitoringService.listContext();
		for (int i = 0; i < resourceContexts.length; i++) {
			resourceContexts[i].removeContext(null);
		}
	}

	@AfterEach
	public void tearDown() throws Exception {

		resourceContextListener.stop();

		// delete all existing resource contexts.
		ResourceContext[] resourceContexts = resourceMonitoringService.listContext();
		for (int i = 0; i < resourceContexts.length; i++) {
			resourceContexts[i].removeContext(null);
		}
	}

	/**
	 * Test case 1 : Creation of a ResourceContext
	 * 
	 * This test case validates the Resource Context creation and the retrieving of a ResourceContext.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#getMonitors()}
	 */
	@Test
	public void testCreationOfAResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";

		// Lists existing contexts by calling ResourceManager.listContext().
		ResourceContext[] resourceContexts = resourceMonitoringService.listContext();
		// Check that the returned list is not null and is empty.
		assertThat(resourceContexts).as("ResourceContext list must not be null.").isNotNull();
		int initialResourceContextsLength = resourceContexts.length;

		// A ResourceContextListener service was already registered.

		// Creates a new ResourceContext named "context1".
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);

		// Check that the returned ResourceContext object is not null.
		assertThat(resourceContext).as("The resourceContext must not be null.").isNotNull();

		// Checks that the name of the newly created ResourceContext is
		// "context1".
		assertThat(resourceContext.getName()).as("Name mismatch.").isEqualTo(name);

		// Checks that the newly created ResourceContext has no associated
		// ResourceMonitor objects.
		assertThat(resourceContext.getMonitors()).as("Monitors list must not be null.").isNotNull();
		assertThat(resourceContext.getMonitors().length).as("Monitors list must be empty.").isZero();

		// Checks that the newly created ResourceContext has no associated
		// bundles.
		assertThat(resourceContext.getBundleIds()).as("BundleIds list must not be null.").isNotNull();
		assertThat(resourceContext.getBundleIds().length).as("BundleIds list must be empty.").isZero();

		// Checks that the ResourceContextListener service receives a
		// RESOURCE_CONTEXT_CREATED ResourceContextEvent.
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("Last received event must not be null.").isNotNull();
		assertThat(lastEvent.getContext()).as("Event resource context mismatch.").isEqualTo(resourceContext);
		assertThat(lastEvent.getType()).as("Event type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_CREATED);

		// Retrieves all existing Resource Contexts.
		resourceContexts = resourceMonitoringService.listContext();

		// Check that the created ResourceContext is in the returned array and
		// the length of the array is equal to initialResourceContextsLength +
		// 1.
		assertThat(resourceContexts).as("ResourceContext list must not be null.").isNotNull();
		assertThat(resourceContexts.length).as("ResourceContext list length must be equal to " + initialResourceContextsLength + 1).isEqualTo(initialResourceContextsLength + 1);
		assertThat(resourceContexts[0]).as("ResourceContext mismatch.").isEqualTo(resourceContext);

		// Retrieves the ResourceContext using ResourceContext.getContext(name
		// of context)
		ResourceContext retrievedResourceContext = resourceMonitoringService.getContext(name);

		// Checks that the returned ResourceContext object is equal to the
		// created ResourceContext.
		assertThat(retrievedResourceContext).as("ResourceContext must not be null.").isNotNull();
		assertThat(retrievedResourceContext).as("ResourceContext mismatch.").isEqualTo(resourceContext);
	}

	/**
	 * Test case 2 : Creation of a ResourceContext with an existing ResourceContext name
	 * 
	 * This test case tests that it is no possible to create two ResourceContext with the same name.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 */
	@Test
	public void testCreationOfAResourceContextWithAnExistingResourceContextName() throws IllegalArgumentException {
		final String name = "context1";
		// A ResourceContextListener service was already registered.

		// Keep resourceMonitoringService.listContext().length.
		int initialResourceContextsLength = resourceMonitoringService.listContext().length;

		// Creates a new ResourceContext named "context1".
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);
		// Checks the returned ResourceContext object is not null.
		assertThat(resourceContext).as("ResourceContext must not be null.").isNotNull();

		// Retrieves the RESOURCE_CONTEXT_CREATED event.
		ResourceContextEvent event = resourceContextListener.getLastEvent();

		// Retrieves all existing Resource Contexts, and Check the created
		// ResourceContext is in the returned array and the length of the array
		// is initialResourceContextsLength + 1.
		assertThat(resourceMonitoringService.listContext().length).as("ResourceMonitoringService.listContext() mismatch.").isEqualTo(initialResourceContextsLength + 1);

		// Try to creates again a ResourceContext named "context1", i.e. with
		// the same name.
		try {
			resourceMonitoringService.createContext(name, null);
			fail("A IllegalArgumentException is expected here.");
		} catch (IllegalArgumentException e) {
			// Checks that a IllegalArgumentException is thrown.
			assertThat(e).as("A IllegalArgumentException is expected.").isInstanceOf(IllegalArgumentException.class);
		}

		// Checks no ResourceContextEvent has been sent (i.e the "last event"
		// must still be the same).
		assertThat(resourceContextListener.getLastEvent()).as("Event mismatch.").isEqualTo(event);

		// Checks that there is still the same number of ResourceContext in the
		// list.
		assertThat(resourceMonitoringService.listContext().length).as("ResourceMonitoringService.listContext().length mismatch.").isEqualTo(initialResourceContextsLength + 1);
	}

	/**
	 * Test case 3 : Creation of a ResourceContext based on a template ResourceContext
	 * 
	 * This test case validates the Resource Context creation with a template Resource Context.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addResourceMonitor(ResourceMonitor)}, {@link ResourceContext#getMonitors()}
	 */
	@Test
	public void testCreationOfAResourceContextBasedOnATemplateResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name1 = "context1";
		final String name2 = "context2";

		// Creates a template ResourceContext named "context1".
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);

		// Checks the created ResourceContext is not null.
		assertThat(resourceContext1).as("ResourceContext must not be null.").isNotNull();

		// Add a FakeResourceMonitor simulating a CPU ResourceMonitor to
		// context1.
		resourceContext1.addResourceMonitor(new FakeResourceMonitor(ResourceMonitoringService.RES_TYPE_CPU, resourceContext1));

		// Creates a new ResourceContext named "context2" with "context1" as
		// template.
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, resourceContext1);

		// Checks the newly created ResourceContext is not null.
		assertThat(resourceContext2).as("ResourceContext must not be null.").isNotNull();
		// Checks the name of the newly created ResourceContext is « context2 ».
		assertThat(resourceContext2.getName()).as("Name mismatch.").isEqualTo(name2);
		// Checks that context2 has no associated bundles.
		assertThat(resourceContext2.getBundleIds().length).as("BundleIds list mismatch.").isZero();

		// Checks that context2 has the same ResourceMonitors as context1.
		ResourceMonitor< ? >[] rc1Monitors = resourceContext1.getMonitors();
		ResourceMonitor< ? >[] rc2Monitors = resourceContext2.getMonitors();
		assertThat(rc2Monitors.length).as("ResourceMonitors list mismatch.").isEqualTo(rc1Monitors.length);
		assertThat(rc2Monitors[0].getResourceType()).as("ResourceType mismatch.").isEqualTo(ResourceMonitoringService.RES_TYPE_CPU);
	}

	/**
	 * Test case 4 : Creation of a ResourceContext based on a template ResourceContext previously deleted.
	 * 
	 * This test case tests the ResourceContext creation with a template ResourceContext which has been previously deleted.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addResourceMonitor(ResourceMonitor)}, {@link ResourceContext#removeContext(ResourceContext)}
	 *             , {@link ResourceContext#getMonitors()}
	 */
	@Test
	public void testCreationOfAResourceContextBasedOnATemplateResourceContextPreviouslyDeleted() throws IllegalArgumentException,
			ResourceContextException {
		final String name1 = "context1";
		final String name2 = "context2";
		// Creates a template ResourceContext named "context1".
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);

		// Checks the created ResourceContext is not null.
		assertThat(resourceContext1).as("ResourceContext must not be null.").isNotNull();

		// Add a FakeResourceMonitor simulating a CPU ResourceMonitor to
		// context1.
		resourceContext1.addResourceMonitor(new FakeResourceMonitor(ResourceMonitoringService.RES_TYPE_CPU, resourceContext1));

		// Delete the ResourceContext context1.
		resourceContext1.removeContext(null);

		// Creates a new ResourceContext named "context2" with "context1" as
		// template.
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, resourceContext1);

		// Checks the newly created ResourceContext is not null.
		assertThat(resourceContext2).as("ResourceContext must not be null.").isNotNull();
		// Checks the name of the newly created ResourceContext is "context2".
		assertThat(resourceContext2.getName()).as("Name mismatch.").isEqualTo(name2);
		// Checks that context2 has no associated bundles.
		assertThat(resourceContext2.getBundleIds().length).as("BundleIds list mismatch.").isZero();
		// Checks that context2 has no ResourceMonitor.
		ResourceMonitor< ? >[] rc2Monitors = resourceContext2.getMonitors();
		assertThat(rc2Monitors.length).as("ResourceMonitors list mismatch.").isZero();
	}

	/**
	 * Test case 5 : Retrieving a ResourceContext based on a bundle identifier.
	 * 
	 * This test case validates the retrieving a ResourceContext based on a bundle.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)}
	 */
	@Test
	public void testRetrievingAResourceContextBasedOnABundleIdentifier() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";
		final long bundleId = context.getBundle().getBundleId();
		// Retrieve the ResourceContext associated with bundle id 1.
		ResourceContext resourceContext = resourceMonitoringService.getContext(bundleId);
		// Check that the returned ResourceContext object is null.
		assertThat(resourceContext).as("ResourceContext must be null.").isNull();

		// Create the ResourceContext named "context1".
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name, null);
		// Associates bundleId 1 with context1
		resourceContext1.addBundle(bundleId);

		// Retrieves the ResourceContext associated with bundleId 1.
		resourceContext = resourceMonitoringService.getContext(bundleId);
		// Checks the returned ResourceContext object is not null.
		assertThat(resourceContext).as("ResourceContext must not be null.").isNotNull();
		// Checks the returned ResourceContext object is the same as context1.
		assertThat(resourceContext).as("ResourceContext mismatch.").isEqualTo(resourceContext1);

		// Try to retrieve the ResourceContext associated with a bundle that
		// doesn't exist (i.e. a bundle with bundleId -1).
		resourceContext = resourceMonitoringService.getContext(-1);
		// Checks the returned ResourceContext object is null.
		assertThat(resourceContext).as("ResourceContext must be null.").isNull();
	}

	/**
	 * Test case 6 : Supported types of resources.
	 * 
	 * This test case checks the retrieving of available Resource Monitor Factory.
	 * 
	 * @throws InvalidSyntaxException
	 *             , see {@link BundleContext#getServiceReferences(Class, String)}
	 */
	@Test
	public void testSupportedTypesOfResources() throws InvalidSyntaxException {

		// Retrieve the array of supported types by calling
		// ResourceManager.getSupportedTypes().
		String[] supportedTypes = resourceMonitoringService.getSupportedTypes();
		// Checks that the array is not null.
		assertThat(supportedTypes).as("SupportedTypes must not be null.").isNotNull();

		// Retrieve all ServiceReference of ResourceMonitorFactory services
		// through the BundleContext
		@SuppressWarnings({
				"unchecked", "rawtypes"
		})
		Collection<ServiceReference<ResourceMonitorFactory< ? >>> factorySrs = (Collection) context
				.getServiceReferences(ResourceMonitorFactory.class, null);

		// Check that the number of ServiceReference is not null.
		assertThat(factorySrs).as("The collection of ServiceReference of ResourceMonitorFactory services must not be null.").isNotNull();
		// Check that the number of ServiceReference is equal to the number of
		// type returned by getSupportedTypes.
		assertThat(factorySrs).as("the number of ServiceReference must be equal to the number of type returned by getSupportedTypes.").hasSize(supportedTypes.length);

		// Iterate over the ServiceReference collection and retrieve each
		// ResourceMonitorFactory service.
		List<String> computedSupportedTypes = new ArrayList<>();
		for (Iterator<ServiceReference<ResourceMonitorFactory< ? >>> it = factorySrs
				.iterator(); it.hasNext();) {
			ResourceMonitorFactory< ? > rmf = context
					.getService(it.next());
			// add the type in the list
			computedSupportedTypes.add(rmf.getType());
		}

		// Checks that all supported types are available by comparing each found
		// ServiceReference.
		assertThat(computedSupportedTypes.size()).as("SupportedTypes list mismatch.").isEqualTo(supportedTypes.length);
		// Check that each supported types is in the list
		for (int i = 0; i < supportedTypes.length; i++) {
			assertThat(computedSupportedTypes.contains(supportedTypes[i])).as("The given supportedType must appear in the SupportedTypes list.").isTrue();
		}
	}

}
