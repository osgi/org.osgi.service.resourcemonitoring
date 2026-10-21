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

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.resourcemonitoring.ResourceContext;
import org.osgi.service.resourcemonitoring.ResourceContextException;
import org.osgi.service.resourcemonitoring.ResourceMonitor;
import org.osgi.service.resourcemonitoring.ResourceMonitorException;
import org.osgi.service.resourcemonitoring.ResourceMonitorFactory;
import org.osgi.service.resourcemonitoring.ResourceMonitoringService;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * @author $Id$
 */
public class TC4_ResourceMonitorTestCase {

	@InjectBundleContext
	BundleContext		context;

	/**
	 * ResourceMonitoringService.
	 */
	private ResourceMonitoringService	resourceMonitoringService;

	/**
	 * cpu factory.
	 */
	private ResourceMonitorFactory< ? >	cpuFactory;

	/**
	 * resourceContext
	 */
	private ResourceContext				resourceContext;

	/**
	 * resource context name
	 */
	private final String				resourceContextName	= "context1";

	@BeforeEach
	public void setUp() throws Exception {
		ServiceReference<ResourceMonitoringService> serviceReference = context
				.getServiceReference(ResourceMonitoringService.class);
		resourceMonitoringService = context
				.getService(serviceReference);

		StringBuffer filter = new StringBuffer();
		filter.append("(&(");
		filter.append(Constants.OBJECTCLASS);
		filter.append("=");
		filter.append(ResourceMonitorFactory.class.getName());
		filter.append(")(");
		filter.append(ResourceMonitorFactory.RESOURCE_TYPE_PROPERTY);
		filter.append("=");
		filter.append(ResourceMonitoringService.RES_TYPE_CPU);
		filter.append("))");
		try {
			@SuppressWarnings({
					"rawtypes", "unchecked"
			})
			Collection<ServiceReference<ResourceMonitorFactory< ? >>> factoryReferences = (Collection) context
					.getServiceReferences(
					ResourceMonitorFactory.class, filter.toString());
			if (factoryReferences != null) {
				if (factoryReferences.size() > 0) {
					ServiceReference<ResourceMonitorFactory< ? >> factoryReference = factoryReferences
							.iterator()
							.next();
					cpuFactory = context
							.getService(factoryReference);

				}
			}
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			fail("Can not get the already existing OSGi service resource monitor factories.");
		}

		resourceContext = resourceMonitoringService.createContext(resourceContextName, null);

		assertThat(cpuFactory).as("CpuFactory must not be null.").isNotNull();
	}

	@AfterEach
	public void tearDown() throws Exception {

		resourceContext.removeContext(null);
	}

	/**
	 * Test case 1 : creation of a ResourceMonitor.
	 * 
	 * This test case tests the creation of a ResourceMonitor through a ResourceMonitorFactory.
	 * 
	 * @throws ResourceMonitorException
	 *             , see {@link ResourceMonitorFactory#createResourceMonitor(ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceContext#getMonitors()}
	 */
	@Test
	public void testCreationOfResourceMonitor() throws ResourceMonitorException, ResourceContextException {

		// check existing resource monitors
		ResourceMonitor< ? >[] monitors = resourceContext.getMonitors();
		assertThat(monitors).as("Monitors list must not be null.").isNotNull();
		assertThat(monitors.length).as("Monitors list mismatch.").isZero();

		// create ResourceMonitor
		ResourceMonitor< ? > resourceMonitor = cpuFactory
				.createResourceMonitor(resourceContext);
		assertThat(resourceMonitor).as("ResourceMonitor must not be null.").isNotNull();

		// check ResourceContext
		monitors = resourceContext.getMonitors();
		assertThat(monitors.length).as("ResourceMonitors list mismatch.").isOne();
		assertThat(monitors[0]).as("ResourceMonitor mismatch.").isEqualTo(resourceMonitor);

		// get monitor from ResourceContext
		ResourceMonitor< ? > retrievedMonitor = resourceContext
				.getMonitor(ResourceMonitoringService.RES_TYPE_CPU);
		assertThat(retrievedMonitor).as("ResourceMonitor must not be null.").isNotNull();
		assertThat(retrievedMonitor).as("ResourceMonitor mismatch.").isEqualTo(resourceMonitor);

		// check resource context from monitor
		ResourceContext retrievedRC = resourceMonitor.getContext();
		assertThat(retrievedRC).as("ResourceContext must not be null.").isNotNull();
		assertThat(retrievedRC).as("ResourceContext mismatch.").isEqualTo(resourceContext);

		// check the newly ResourceMonitor
		assertThat(resourceMonitor.isEnabled()).as("ResourceMonitor must not be enabled.").isFalse();
		assertThat(resourceMonitor.isDeleted()).as("ResourceMonitor must not be deleted.").isFalse();
		assertThat(resourceMonitor.getResourceType()).as("Type mismatch.").isEqualTo(cpuFactory.getType());

		// create again a CPU Resource Monitor for this context
		try {
			cpuFactory.createResourceMonitor(resourceContext);
			fail("A ResourceMonitorException is expected here.");
		} catch (ResourceMonitorException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// check the ResourceContext has still one ResourceMonitor
		monitors = resourceContext.getMonitors();
		assertThat(monitors.length).as("ResourceMonitors list mismatch.").isOne();
		assertThat(monitors[0]).as("ResourceMonitor mismatch.").isEqualTo(resourceMonitor);
	}

	/**
	 * Test case 2 : deletion of a ResourceMonitor from a ResourceContext.
	 * 
	 * This test case validates the deletion of ResourceMonitor from a ResourceContext (check that the monitor is removed from the context).
	 * 
	 * @throws ResourceMonitorException
	 *             , see {@link ResourceMonitorFactory#createResourceMonitor(ResourceContext)}
	 * @throws ResourceContextException
	 *             , see {@link ResourceMonitor#delete()} {@link ResourceContext#getMonitors()}
	 */
	@Test
	public void testDeletionOfAResourceMonitorFromAResourceContext() throws ResourceMonitorException, ResourceContextException {
		// create ResourceMonitor
		ResourceMonitor< ? > resourceMonitor = cpuFactory
				.createResourceMonitor(resourceContext);

		// delete ResourceMonitor
		resourceMonitor.delete();

		// check resource context monitors
		ResourceMonitor< ? >[] monitors = resourceContext.getMonitors();
		assertThat(monitors).as("ResourceMonitors list must not be null.").isNotNull();
		assertThat(monitors.length).as("ResourceMonitors list mismatch.").isZero();

		// check ResourceMonitor state
		assertThat(resourceMonitor.isDeleted()).as("ResourceMonitor must have been deleted.").isTrue();
	}

	/**
	 * Test case 3 : enabling and disabling a ResourceMonitor.
	 * 
	 * This test case checks the enabling/disabling of a ResourceMonitor.
	 * 
	 * @throws ResourceMonitorException
	 *             , see {@link ResourceMonitorFactory#createResourceMonitor(ResourceContext)} {@link ResourceMonitor#enable()}
	 *             {@link ResourceMonitor#disable()} {@link ResourceMonitor#delete()}
	 */
	@Test
	public void testEnablingAndDisablingAResourceMonitor() throws ResourceMonitorException {
		// create ResourceMonitor
		ResourceMonitor< ? > resourceMonitor = cpuFactory
				.createResourceMonitor(resourceContext);

		// check the monitor is disabled by default
		assertThat(resourceMonitor.isEnabled()).as("ResourceMonitor must not be enabled.").isFalse();

		// enable it
		resourceMonitor.enable();

		// check the monitor is enabled
		assertThat(resourceMonitor.isEnabled()).as("ResourceMonitor must be enabled.").isTrue();

		// disable it
		resourceMonitor.disable();

		// check the monitor is disabled
		assertThat(resourceMonitor.isEnabled()).as("ResourceMonitor must not be enabled.").isFalse();

		// delete the monitor
		resourceMonitor.delete();

		// try to enable a deleted monitor ==> expect a
		// ResourceMonitorException
		try {
			resourceMonitor.enable();
			fail("A ResourceMonitorException is expected.");
		} catch (ResourceMonitorException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}

		// try to disable it ==> expect a ResourceMonitorException
		try {
			resourceMonitor.disable();
			fail("A ResourceMonitorException is expected.");
		} catch (ResourceMonitorException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}
	}

	/**
	 * Test case 4 : retrieving a resource usage.
	 * 
	 * This use case checks the retrieving of the resource usage a ResourceContext is consuming.
	 * 
	 * @throws ResourceMonitorException
	 *             , see {@link ResourceMonitorFactory#createResourceMonitor(ResourceContext)} {@link ResourceMonitor#getUsage()}
	 *             {@link ResourceMonitor#enable()} {@link ResourceMonitor#disable()} {@link ResourceMonitor#delete()}
	 */
	@Test
	public void testRetrievingResourceUsage() throws ResourceMonitorException {
		// create ResourceMonitor
		ResourceMonitor< ? > resourceMonitor = cpuFactory
				.createResourceMonitor(resourceContext);

		// retrieves the resource usage
		assertThat(resourceMonitor.getUsage()).as("Usage must not be null.").isNotNull();

		// enable it
		resourceMonitor.enable();

		// retrieves the resource usage
		assertThat(resourceMonitor.getUsage()).as("Usage must not be null.").isNotNull();

		// disable it
		resourceMonitor.disable();

		// retrieves the resource usage
		assertThat(resourceMonitor.getUsage()).as("Usage must not be null.").isNotNull();

		// delete the monitor
		resourceMonitor.delete();

		// try to enable a deleted monitor ==> expect a ResourceMonitorException
		try {
			resourceMonitor.getUsage();
			fail("A ResourceMonitorException is expected.");
		} catch (ResourceMonitorException e) {
			System.out.println("Expected exception: " + e.getMessage());
		}
	}

}
