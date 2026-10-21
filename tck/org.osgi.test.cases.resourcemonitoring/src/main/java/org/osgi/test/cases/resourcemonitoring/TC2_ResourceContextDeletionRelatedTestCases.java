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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.resourcemonitoring.ResourceContext;
import org.osgi.service.resourcemonitoring.ResourceContextEvent;
import org.osgi.service.resourcemonitoring.ResourceContextException;
import org.osgi.service.resourcemonitoring.ResourceMonitoringService;
import org.osgi.test.cases.resourcemonitoring.utils.FakeResourceMonitor;
import org.osgi.test.cases.resourcemonitoring.utils.ResourceContextListenerTestImpl;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * @author $Id$
 */
public class TC2_ResourceContextDeletionRelatedTestCases {

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
	 * Test case 1 : deletion of a ResourceContext.
	 * 
	 * This test case validates the deletion of a ResourceContext with no ResourceContext as destination.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#removeContext(ResourceContext)}
	 */
	@Test
	public void testDeletionOfAResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name = "context1";

		// create the resource context.
		ResourceContext resourceContext = resourceMonitoringService.createContext(name, null);

		// delete this new resourceContext.
		resourceContext.removeContext(null);

		// check existing ResourceContext.
		assertThat(resourceMonitoringService.listContext().length).as("ResourceContext list must be empty.").isZero();
		assertThat(resourceMonitoringService.getContext(name)).as("ResourceMonitoringService must not contain a ResourceContext named: " + name).isNull();

		// check the resource context name is still accessible.
		assertThat(resourceContext.getName()).as("Name mismatch.").isEqualTo(name);

		// check the resource context bundleIds array.
		assertThat(resourceContext.getBundleIds()).as("BundlesIds array must not be null.").isNotNull();
		assertThat(resourceContext.getBundleIds().length).as("BundlesIds array must not contain any bundleId.").isZero();

		// check the resource context monitors.
		try {
			resourceContext.getMonitors();
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check it is not possible to add bundle.
		try {
			resourceContext.addBundle(0);
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check it is not possible to add monitor.
		try {
			resourceContext.addResourceMonitor(new FakeResourceMonitor());
			fail("A ResourceContextException is expected here.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check a ResourceContextEvent has been sent due to deletion.
		ResourceContextEvent lastEvent = resourceContextListener.getLastEvent();
		assertThat(lastEvent).as("LastEvent must not be null.").isNotNull();
		assertThat(lastEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext);
		assertThat(lastEvent.getType()).as("Last event type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_REMOVED);
	}

	/**
	 * Test case 2 : deletion of a ResourceContext with a destination ResourceContext.
	 * 
	 * This test case validates the deletion of a ResourceContext with a destination.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)} {@link ResourceContext#removeContext(ResourceContext)}
	 */
	@Test
	public void testDeletionOfAResourceContextWithADestinationResourceContext() throws IllegalArgumentException, ResourceContextException {
		final String name1 = "context1";
		final String name2 = "context2";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContexts
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, null);
		System.out.println("resourceContext1:" + resourceContext1);
		System.out.println("resourceContext2:" + resourceContext2);

		// checks the array of existing ResourceContext
		assertThat(resourceMonitoringService.listContext().length).as("ResourceContext list mismatch.").isEqualTo(2);

		// associate bundleId with resourceContext1
		resourceContext1.addBundle(bundleId);

		// associate a monitor

		// delete resourceContext1 and specify resourceContext2 as
		// destination
		resourceContext1.removeContext(resourceContext2);
		// deleting resourceContext1 MUST have generated 6 events (in the
		// following order):
		// - 1 for creating resourceContext1
		// - 1 for creating resourceContext2
		// - 1 for adding bundleId to resourceContext1
		// - 1 for removing bundleId from resourceContext1
		// - 1 for adding bundleId to resourceContext2
		// - 1 for removing resourceContext1

		// checks that context2 is the only existing ResourceContext that
		// remains.
		ResourceContext[] existingContexts = resourceMonitoringService.listContext();
		assertThat(existingContexts.length).as("ResourceContext list mismatch.").isOne();
		assertThat(existingContexts[0].getName()).as("Name mismatch.").isEqualTo(name2);

		// check resourceContext1 (no associated bundle and no associated
		// monitors)
		long[] rc1BundleIds = resourceContext1.getBundleIds();
		assertThat(rc1BundleIds.length).as("BundleIds list mismatch.").isZero();
		try {
			resourceContext1.getMonitors();
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check bundleId belongs to resourceContext2
		long[] rc2BundleIds = resourceContext2.getBundleIds();
		assertThat(rc2BundleIds.length).as("BundleIds list mismatch.").isOne();
		assertThat(rc2BundleIds[0]).as("BundleId mismatch.").isEqualTo(bundleId);

		// Check the received events.
		List<ResourceContextEvent> receivedEvents = resourceContextListener
				.getReceivedEvents();
		System.out.println("nb of receivedEvent : " + receivedEvents.size());
		assertThat(receivedEvents).as("Number of events mismatch.").hasSize(6);

		// The fourth received event is a BUNDLE_REMOVED ResourceContextEvent
		// has been received
		ResourceContextEvent fourthEvent = receivedEvents.get(3);
		assertThat(fourthEvent).as("FourthEvent must not be null.").isNotNull();
		assertThat(fourthEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(fourthEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext1);
		assertThat(fourthEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);

		// check a BUNDLE_ADDED ResourceContextEvent has been received (fifth
		// received event)
		ResourceContextEvent fifthEvent = receivedEvents.get(4);
		assertThat(fifthEvent).as("FifthEvent must not be null.").isNotNull();
		assertThat(fifthEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_ADDED);
		assertThat(fifthEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext2);
		assertThat(fifthEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);

		// check the sixth received event is a RESOURCE_CONTEXT_REMOVED
		// event
		ResourceContextEvent sixthEvent = receivedEvents.get(5);
		assertThat(sixthEvent).as("SixthEvent must not be null.").isNotNull();
		assertThat(sixthEvent.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_REMOVED);
		assertThat(sixthEvent.getContext()).as("ResourceContext mismatch.").isEqualTo(resourceContext1);
		assertThat(sixthEvent.getBundleId()).as("BundleId mismatch.").isEqualTo(-1);
	}

	/**
	 * Test case 3 : deletion of a ResourceContext with a previously deleted ResourceContext as destination.
	 * 
	 * This test case validates the deletion of a ResourceContext with a ResourceContext destination which has been previously deleted.
	 * 
	 * @throws IllegalArgumentException
	 *             , see {@link ResourceMonitoringService#createContext(String, ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#addBundle(long)}, {@link ResourceContext#removeContext(ResourceContext)}
	 */
	@Test
	public void testDeletionOfAResourceContextWithAPreviouslyDeletedResourceContextAsDestination() throws IllegalArgumentException,
			ResourceContextException {
		final String name1 = "name1";
		final String name2 = "name2";
		final long bundleId = context.getBundle().getBundleId();

		// create ResourceContexts
		ResourceContext resourceContext1 = resourceMonitoringService.createContext(name1, null);
		ResourceContext resourceContext2 = resourceMonitoringService.createContext(name2, null);

		// add bundleId to resourceContext1
		resourceContext1.addBundle(bundleId);

		// delete resourceContext2
		resourceContext2.removeContext(null);

		// delete resourceContext1 with resourceContext2 as destination
		// resourceContext2 has been previously deleted
		resourceContext1.removeContext(resourceContext2);

		// try to add bundleId to resourceContext1 => expect a
		// ResourceContextException
		try {
			resourceContext1.addBundle(bundleId);
			fail("A ResourceContextException is expected.");
		} catch (ResourceContextException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check that there is no existing ResourceContext
		assertThat(resourceMonitoringService.listContext().length).as("ResourceContexts list mismatch.").isZero();

		// check events
		List<ResourceContextEvent> events = resourceContextListener
				.getReceivedEvents();
		// 6 events MUST be emitted (in this order):
		// - creation of context1
		// - creation of context2
		// - adding of bundle 1 to context1
		// - deletion of context2
		// - removing of bundle1 from context1
		// - deletion of context1
		assertThat(events).as("Number of events mismatch.").hasSize(6);

		// event RESOURCE_CONTEXT_CREATED for context1
		ResourceContextEvent event = events.get(0);
		assertThat(event.getType()).as("Event type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_CREATED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name1);

		// event RESOURCE_CONTEXT_CREATED for context2
		event = events.get(1);
		assertThat(event.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_CREATED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name2);

		// event BUNDLE_ADDED for bundle1 and context1
		event = events.get(2);
		assertThat(event.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.BUNDLE_ADDED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name1);
		assertThat(event.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);

		// event RESOURCE_CONTEXT_REMOVED for context2
		event = events.get(3);
		assertThat(event.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_REMOVED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name2);

		// event BUNDLE_REMOVED from context1
		event = events.get(4);
		assertThat(event.getType()).as("Type mismatch: ResourceContextEvent.BUNDLE_REMOVED is expected.").isEqualTo(ResourceContextEvent.BUNDLE_REMOVED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name1);
		assertThat(event.getBundleId()).as("BundleId mismatch.").isEqualTo(bundleId);

		// event RESOURCE_CONTEXT_REMOVED for context2
		event = events.get(5);
		assertThat(event.getType()).as("Type mismatch.").isEqualTo(ResourceContextEvent.RESOURCE_CONTEXT_REMOVED);
		assertThat(event.getContext().getName()).as("Name mismatch.").isEqualTo(name1);
	}
}
