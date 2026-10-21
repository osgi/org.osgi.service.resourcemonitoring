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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.resourcemonitoring.ResourceContext;
import org.osgi.service.resourcemonitoring.ResourceContextEvent;
import org.osgi.service.resourcemonitoring.ResourceContextException;
import org.osgi.service.resourcemonitoring.ResourceMonitoringService;
import org.osgi.test.cases.resourcemonitoring.utils.ResourceContextListenerTestImpl;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * @author $Id$
 */
public class TC3_ResourceContextBundleManagementTestCases {

	@InjectBundleContext
	BundleContext		context;

	/**
	 * ResourceMonitoringService
	 */
	private ResourceMonitoringService		resourceMonitoringService;

	/**
	 * resource context listener
	 */
	private ResourceContextListenerTestImpl	resourceContextListener;

	@BeforeEach
	public void setUp() throws Exception {

		ServiceReference<ResourceMonitoringService> resourceMonitoringServiceSr = context
				.getServiceReference(ResourceMonitoringService.class);
		if (resourceMonitoringServiceSr != null) {
			resourceMonitoringService = context
					.getService(resourceMonitoringServiceSr);
		}
		resourceContextListener = new ResourceContextListenerTestImpl();
		resourceContextListener.start(context);

		// delete all existing ResourceContext
		ResourceContext[] existingContexts = resourceMonitoringService.listContext();
		for (int i = 0; i < existingContexts.length; i++) {
			ResourceContext currentResourceContext = existingContexts[i];
			currentResourceContext.removeContext(null);
		}
	}

	@AfterEach
	public void tearDown() throws Exception {

		resourceContextListener.stop();

		// delete all existing ResourceContext
		ResourceContext[] existingContexts = resourceMonitoringService.listContext();
		for (int i = 0; i < existingContexts.length; i++) {
			ResourceContext currentResourceContext = existingContexts[i];
			currentResourceContext.removeContext(null);
		}
	}

	/**
	 * Test case 1 : adding a bundle to a ResourceContext.
	 * 
	 * This test cases tests the adding of a bundle to a ResourceContext.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)}
	 */
	@Test
	public void testAddingABundleToAResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContext
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);

		// add bundle 1 to this context
		resourceContext.addBundle(bundleId);

		// check bundle 1 has been associated to this context
		long[] bundleIds = resourceContext.getBundleIds();
		assertThat(bundleIds.length).as("BundleIds list mismatch.").isOne();
		assertThat(bundleIds[0]).as("BundleId mismatch.").isEqualTo(bundleId);

		// check a ResourceContextEvent has been sent
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("LastEvent must not be null.").isNotNull();
		assertThat(lastEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_ADDED);
		assertThat(lastEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext);
		assertThat(lastEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);
	}

	/**
	 * Test case 2 : adding a bundle previously associated with a ResourceContext.
	 * 
	 * This test case tests the adding of a bundle previously added to another ResourceContext.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)}
	 */
	@Test
	public void testAddingABundlePreviouslyAssociatedWithAResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";
		final String name2 = "context2";
		final long bundleId = context.getBundle().getBundleId();

		// Create ResourceContexts.
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, null);

		// Add bundleId 1 to resourceContext.
		resourceContext.addBundle(bundleId);
		long[] bundleIds = resourceContext.getBundleIds();
		assertThat(bundleIds.length).as("BundleIds list mismatch: the resourceContext bundleIds list is expected to contain 1 bundleId.").isOne();
		assertThat(bundleIds[0]).as("BundleId mismatch: the bundleIds list must only contain the bundleId " + bundleId).isEqualTo(bundleId);

		// Try to add bundleId 1 to resourceContext2.
		try {
			resourceContext2.addBundle(bundleId);
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// Check that bundleId 1 is not associated with resourceContext2.
		assertThat(resourceContext2.getBundleIds().length).as("BundleIds list mismatch: the resourceContext2 bundleIds list is expected to be empty.").isZero();

		// Check that bundleId 1 is still associated with resourceContext1.
		bundleIds = resourceContext.getBundleIds();
		assertThat(bundleIds.length).as("BundleIds list mismatch: the resourceContext bundleIds list is expected to contain 1 bundleId.").isOne();
		assertThat(bundleIds[0]).as("BundleId mismatch: the bundleIds list must only contain the bundleId " + bundleId).isEqualTo(bundleId);
	}

	/**
	 * Test case 3 : removing a bundle from a ResourceContext.
	 * 
	 * This test case validates the removing of a bundle from a ResourceContext (and checks that a {@link ResourceContextEvent} is
	 * received).
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)} {@link ResourceContext#removeBundle(long)}
	 */
	@Test
	public void testRemoveBundleFromAResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContext
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);

		// add bundle 1
		resourceContext.addBundle(bundleId);

		// remove bundleId from resourceContext
		resourceContext.removeBundle(bundleId);

		// check list of bundles
		assertThat(resourceContext.getBundleIds()).as("BundleIds list must not be null.").isNotNull();
		assertThat(resourceContext.getBundleIds().length).as("BundleIds list mismatch.").isZero();

		// check a ResourceContextEvent has been received
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("LastEvent must not be null.").isNotNull();
		assertThat(lastEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(lastEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext);
		assertThat(lastEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);

		// try to remove it again ==> expect a ResourceContextException
		try {
			resourceContext.removeBundle(bundleId);
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}
	}

	/**
	 * Test case 4 : removing a bundle with a destination ResourceContext.
	 * 
	 * This test case tests the removing of a bundle from a context with a destination context (expects one ResourceContextEvent from
	 * removing the bundle and one more for adding it into another ResourceContext).
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)} {@link ResourceContext#removeBundle(long)}
	 */
	@Test
	public void testRemoveABundleWithADestinationResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name1 = "context1";
		final String name2 = "context2";
		final long bundleId = context.getBundle().getBundleId();

		// create two Resource Context
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, null);

		// add bundleId to context1
		resourceContext1.addBundle(bundleId);

		// remove bundleId from context1 with destination = context2
		resourceContext1.removeBundle(bundleId, resourceContext2);

		// check context1 has no associated bundles
		assertThat(resourceContext1.getBundleIds().length).as("BundleIds list mismatch.").isZero();

		// check context2 has one associated bundle (i.e bundleId)
		long[] context2BundleIds = resourceContext2.getBundleIds();
		assertThat(context2BundleIds.length).as("BundleIds list mismatch.").isOne();
		assertThat(context2BundleIds[0]).as("BundleId mismatch.").isEqualTo(bundleId);

		// check a ResourceContextEvent has been received about the removed
		// bundle (should be the last last received event)
		ResourceContextEvent removedEvent = resourceContextListener.getLastLastEvent();
		assertThat(removedEvent).as("RemovedEvent must not be null.").isNotNull();
		assertThat(removedEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(removedEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);
		assertThat(removedEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext1);

		// check a ResourceContextEvent has been received about the newly added
		// bundleId
		ResourceContextEvent addedEvent = resourceContextListener.getLastEvent();
		assertThat(addedEvent).as("AddedEvent must not be null.").isNotNull();
		assertThat(addedEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_ADDED);
		assertThat(addedEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);
		assertThat(addedEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext2);
	}

	/**
	 * Test case 5 : removing a bundle with a null destination ResourceContext.
	 * 
	 * This test case validates the removing of a bundle from a ResourceContext with a null destination. This test case is similar to
	 * {@link TC3_ResourceContextBundleManagementTestCases#testRemoveBundleFromAResourceContext()} (Expect a REMOVED_BUNDLE event).
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)} {@link ResourceContext#removeBundle(long)}
	 */
	@Test
	public void testRemoveABundleWithANullDestinationResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContext
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);

		// add bundleId to resourceContext
		resourceContext.addBundle(bundleId);

		// remove it with destination null
		resourceContext.removeBundle(bundleId, null);

		// check list of bundle
		long[] bundleIds = resourceContext.getBundleIds();
		assertThat(bundleIds).as("BundleIds list must not be null.").isNotNull();
		assertThat(bundleIds.length).as("BundlesIds list mismatch.").isZero();

		// check that a REMOVED_BUNDLE event has been received
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("LastEvent must not be null.").isNotNull();
		assertThat(lastEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(lastEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext);
		assertThat(lastEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);
	}

	/**
	 * Test case 6 : removing a bundle with a destination context which has been previously deleted.
	 * 
	 * This test case checks the removing of a bundle from a ResourceContext with a destination Resource Context which has been removed
	 * (Expect a ResourceContextException when invoking {@link ResourceContext#removeBundle(long, ResourceContext)}).
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)} {@link ResourceContext#removeBundle(long)}
	 */
	@Test
	public void testRemovingABundleWithADestinationContextWhichHasBeenPreviouslyDeleted() throws IllegalArgumentException,
			ResourceContextException {
		final String name1 = "context1";
		final String name2 = "context2";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContexts
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, null);

		// add bundleId to resourceContext1
		resourceContext1.addBundle(bundleId);

		// delete resourceContext2
		resourceContext2.removeContext(null);

		// try to remove bundleId from resourceContext1 and associate it with
		// deleted ResourceContext2 ==> expect a ResourceContextException
		try {
			resourceContext1.removeBundle(bundleId, resourceContext2);
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check bundleId has been removed from resourceContext1
		long[] resourceContext1bundleIds = resourceContext1.getBundleIds();
		assertThat(resourceContext1bundleIds.length).as("BundleIds list mismatch.").isZero();

		// check a REMOVED_BUNDLE ResourceContextEvent event has been received
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("LastEvent must not be null.").isNotNull();
		assertThat(lastEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(lastEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext1);
		assertThat(lastEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);
	}

}
