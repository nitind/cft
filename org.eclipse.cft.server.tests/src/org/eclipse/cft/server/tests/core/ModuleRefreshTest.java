/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * and the Apache License v2.0 is available at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.tests.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.wst.server.core.IModule;

/**
 * Test Module refresh scenarios, including deleting an application externally
 * (outside of the Eclipse tools), as well as updating existing applications
 * externally (e.g. scaling memory), and making sure that the module in the
 * tools is updated accordingly when invoking update modules API on the
 * {@link CloudFoundryServer} and {@link CloudFoundryServerBehaviour}
 *
 */
public class ModuleRefreshTest extends AbstractAsynchCloudTest {

	public void testUpdateModulesServerBehaviourExistingCloudApp() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case

		String testName = "refreshExisting";
		String expectedAppName = harness.getWebAppName(testName);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(expectedAppName));
		client.createApplication(expectedAppName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// This will tell the behaviour to fetch the Cloud application from the
		// Cloud space and generate a module
		CloudFoundryApplicationModule updateModule = serverBehavior.updateModuleWithBasicCloudInfo(expectedAppName,
				new NullProgressMonitor());
		assertEquals(expectedAppName, updateModule.getDeployedApplicationName());
		assertEquals(updateModule.getDeployedApplicationName(), updateModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(updateModule.getName(), updateModule.getApplication().getName());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, updateModule.getApplication().getMemory());
		assertEquals(updateModule.getDeploymentInfo().getMemory(), updateModule.getApplication().getMemory());
	}

	public void testUpdateModuleInstances() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case

		String testName = "updateModules";
		String expectedAppName = harness.getWebAppName(testName);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(expectedAppName));
		client.createApplication(expectedAppName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// This will tell the behaviour to fetch the Cloud application from the
		// Cloud space and generate a module
		CloudFoundryApplicationModule updateModule = serverBehavior.updateModuleWithAllCloudInfo(expectedAppName,
				new NullProgressMonitor());
		assertEquals(expectedAppName, updateModule.getDeployedApplicationName());
		assertEquals(updateModule.getDeployedApplicationName(), updateModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(updateModule.getName(), updateModule.getApplication().getName());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, updateModule.getApplication().getMemory());
		assertEquals(updateModule.getDeploymentInfo().getMemory(), updateModule.getApplication().getMemory());
		assertEquals(1, updateModule.getInstanceCount());

		// There is one instance, but since the app was created EXTERNALLY and
		// not started, there should
		// be no instance info
		assertEquals(0, updateModule.getApplicationStats().getRecords().size());
		assertNull(updateModule.getInstancesInfo());

		updateModule = serverBehavior.updateModuleWithAllCloudInfo((String) null, new NullProgressMonitor());
		assertNull(updateModule);

		updateModule = serverBehavior.updateModuleWithAllCloudInfo("wrongName", new NullProgressMonitor());
		assertNull(updateModule);

		updateModule = serverBehavior.updateDeployedModule((IModule) null, new NullProgressMonitor());
		assertNull(updateModule);

	}

	public void testUpdateModulesServerBehaviourWrongCloudApp() throws Exception {
		// Update modules API in behaviour will return a
		// CloudFoundryApplicationModule for an existing Cloud application in
		// the Cloud Space. This associated update modules for the Cloud Foundry
		// Server
		// which the behaviour uses is tested separately in a different test
		// case
		String testName = "updateWrongApp";
		String expectedAppName = harness.getWebAppName(testName);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(expectedAppName));
		client.createApplication(expectedAppName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		// The tool has not refreshed after the app was created with an external
		// client, so there should be no module
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertNull(appModule);

		CloudFoundryApplicationModule wrongModule = serverBehavior.updateModuleWithBasicCloudInfo("wrongApp",
				new NullProgressMonitor());
		assertNull(wrongModule);

		wrongModule = serverBehavior.updateModuleWithAllCloudInfo("wrongApp", new NullProgressMonitor());
		assertNull(wrongModule);
	}

	public void testUpdateModulesCloudServer() throws Exception {

		// Tests the Update modules API in the server that will CREATE or return
		// an existing
		// CloudFoundryApplicationModule ONLY if it is given a CloudApplication.

		String testName = "updateCloudServer";
		String expectedAppName = harness.getWebAppName(testName);

		// Create the app externally AFTER the server connects in the setup to
		// ensure the tools did not pick up the Cloud application during refresh
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(expectedAppName));
		client.createApplication(expectedAppName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);
		// Tooling has not yet been updated so there is no corresponding
		// appModule even though the app exists in the Cloud space
		assertNull(appModule);

		// No actual cloud application passed to update therefore no associated
		// CloudFoundryApplicationModule should be found
		appModule = cloudServer.updateModule(null, expectedAppName, null, new NullProgressMonitor());
		assertNull(appModule);

		appModule = cloudServer.updateModule(null, null, null, new NullProgressMonitor());
		assertNull(appModule);

		// Once again check that existing module does not return anything as no
		// refresh or update has occurred.
		appModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertNull(appModule);

		// Get the actual cloud app directly from the Cloud space
		CloudApplication actualApp = getAppFromExternalClient(expectedAppName);
		ApplicationStats stats = client.getApplicationStats(actualApp.getName());

		// Now create the CloudFoundryApplicationModule
		appModule = cloudServer.updateModule(actualApp, expectedAppName, stats, new NullProgressMonitor());

		assertEquals(expectedAppName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), appModule.getApplication().getName());

		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());

		// It should match what is obtained through getExisting API
		CloudFoundryApplicationModule existingCloudMod = cloudServer.getExistingCloudModule(expectedAppName);

		assertEquals(expectedAppName, existingCloudMod.getDeployedApplicationName());
		assertEquals(existingCloudMod.getDeployedApplicationName(), existingCloudMod.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), existingCloudMod.getApplication().getName());

		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, existingCloudMod.getApplication().getMemory());
		assertEquals(existingCloudMod.getDeploymentInfo().getMemory(), existingCloudMod.getApplication().getMemory());

		CloudFoundryApplicationModule sameExistingApp = cloudServer.getExistingCloudModule(expectedAppName);
		assertNotNull(sameExistingApp);
		assertEquals(sameExistingApp.getDeployedApplicationName(), sameExistingApp.getApplication().getName());

		// Check the mapping is correct
		assertEquals(actualApp.getName(), sameExistingApp.getApplication().getName());

		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, sameExistingApp.getApplication().getMemory());
		assertEquals(sameExistingApp.getDeploymentInfo().getMemory(), sameExistingApp.getApplication().getMemory());

	}

	public void testModuleUpdatesExternalChanges() throws Exception {

		// Tests various module update scenarios due to external changes.
		// This is performed in one test to avoid multiple application creations
		// and deployments during junit setups which are slow
		// Tests the following cases:
		// 1. Push application for the first time - Module should be created and
		// mapped to a CloudApplication
		// 2. Update the application externally and update through behaviour API
		// - Module and mapping to CloudApplication should be updated
		// 3. Update the application externally and refresh all Modules - Module
		// and mapping to CloudApplication should be updated.

		String testName = "updateExternal";
		String appName = harness.getWebAppName(testName);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY,
				stopMode);

		IModule module = getWstModule(project.getName());

		// Push the application
		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertNotNull(appModule);
		// Test that the mapping to the actual application in the Cloud space is
		// present. Since a
		// CloudApplication is not created by the underlying client unless it
		// exists, this also
		// indirectly tests that the CloudApplication was successfully created
		// indicating the application
		// exists in the Cloud space.
		assertNotNull(appModule.getApplication());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// To test update on external changes, verify the current memory
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, appModule.getDeploymentInfo().getMemory());

		// Verify that the CloudApplication in the Cloud space exists through
		// the list of all CloudApplications
		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertTrue("Expected CloudApplication for " + appName + " to exist in the Cloud space", found);

		// Now modify the application externally and verify that when performing
		// a module update
		// that the new changes are picked up by the tooling

		// Create separate external client
		CloudFoundryOperations externalClient = getTestFixture().createExternalClient();
		externalClient.login();

		// Refresh Module through behaviour to check if it picks up changes

		// 1. Test via single-module update
		externalClient.updateApplicationMemory(appName, 737);
		CloudApplication updatedCloudApplicationFromClient = externalClient.getApplication(appName);

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(737, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 2. Test via single-module update and it's instances
		externalClient.updateApplicationMemory(appName, 555);

		updatedCloudApplicationFromClient = externalClient.getApplication(appName);
		appModule = serverBehavior.updateModuleWithAllCloudInfo(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());

		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(555, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

		// 3. Test via module refresh of all modules
		externalClient.updateApplicationMemory(appName, 345);
		updatedCloudApplicationFromClient = externalClient.getApplication(appName);
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		Map<String, ApplicationStats> stats = new HashMap<String, ApplicationStats>();
		allApps.put(updatedCloudApplicationFromClient.getName(), updatedCloudApplicationFromClient);
		stats.put(updatedCloudApplicationFromClient.getName(), cloudServer.getBehaviour()
				.getApplicationStats(updatedCloudApplicationFromClient.getName(), new NullProgressMonitor()));

		cloudServer.updateModules(allApps, stats);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());

		assertEquals(appModule.getDeployedApplicationName(), updatedCloudApplicationFromClient.getName());
		assertEquals(345, updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getApplication().getMemory(), updatedCloudApplicationFromClient.getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), updatedCloudApplicationFromClient.getMemory());

	}

	public void testSingleModuleUpdateExternalAppDeletion() throws Exception {

		String testName = "externalAppDeletion";
		String appName = harness.getWebAppName(testName);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY,
				stopMode);

		IModule module = getWstModule(project.getName());

		// Push the application.
		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());

		// Delete module externally and verify that module refresh picks up the
		// change

		// Create separate external client
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		client.deleteApplication(appName);

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule(appName);

		assertNull(appModule);

		CloudApplication nonexistantApp = null;
		appModule = cloudServer.updateModule(nonexistantApp, appName, null, new NullProgressMonitor());
		assertNull(appModule);

	}

	public void testSingleModuleUpdateNonExistingApp() throws Exception {

		String testName = "nonExistingAPp";
		String appName = harness.getWebAppName(testName);
		IProject project = createWebApplicationProject();

		boolean stopMode = false;

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY,
				stopMode);

		IModule module = getWstModule(project.getName());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule((String) null);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule((IModule) null);

		assertNull(appModule);

		appModule = cloudServer.getExistingCloudModule(module);

		assertNull(appModule);

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());

		assertNull(appModule);

		CloudApplication nonexistantApp = null;
		appModule = cloudServer.updateModule(nonexistantApp, appName, null, new NullProgressMonitor());
		assertNull(appModule);

		appModule = cloudServer.updateModule(null, null, null, new NullProgressMonitor());
		assertNull(appModule);

	}

	public void testSingleModuleUpdateExternalCreation() throws Exception {

		String testName = "externalApp";
		String appName = harness.getWebAppName(testName);

		// After deployment the module must exist and be mapped to an existing
		// CloudApplication
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());
		assertNull(appModule);

		// Create separate external client
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(appName));
		client.createApplication(appName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());

		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());
	}

	public void testAllModuleUpdateExternalCreation() throws Exception {

		String testName = "externalApps";
		String appName = harness.getWebAppName(testName);

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(appName);
		assertNull(appModule);

		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());
		assertNull(appModule);

		// Create separate external client
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();

		List<String> urls = new ArrayList<String>();
		urls.add(harness.generateAppUrl(appName));
		client.createApplication(appName, new Staging(), CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, urls,
				new ArrayList<String>());

		CloudApplication application = client.getApplication(appName);
		Map<String, CloudApplication> allApps = new HashMap<String, CloudApplication>();
		Map<String, ApplicationStats> stats = new HashMap<String, ApplicationStats>();

		allApps.put(application.getName(), application);
		stats.put(application.getName(),
				cloudServer.getBehaviour().getApplicationStats(application.getName(), new NullProgressMonitor()));

		cloudServer.updateModules(allApps, stats);

		cloudServer.updateModules(allApps, stats);
		appModule = cloudServer.getExistingCloudModule(appName);

		assertEquals(appModule.getDeployedApplicationName(), appModule.getApplication().getName());
		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());

		// It should match what is obtained through update cloud module
		appModule = serverBehavior.updateModuleWithBasicCloudInfo(appName, new NullProgressMonitor());
		assertNotNull(appModule);
		assertNotNull(appModule.getApplication());
		assertEquals(appName, appModule.getDeployedApplicationName());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, appModule.getApplication().getMemory());
		assertEquals(appModule.getDeploymentInfo().getMemory(), appModule.getApplication().getMemory());
	}

	public void testScheduleRefreshHandlerAllModules() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String testName = "refreshAll";
		IProject project = createWebApplicationProject();

		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);
		deployApplicationWithModuleRefresh(expectedAppName, project, startApp, harness.getDefaultBuildpack());

		// Test the server-wide refresh of all modules without specifying a
		// selected module.
		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);
		cloudServer.getBehaviour().asyncUpdateAll();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_UPDATE_COMPLETED);

		refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		cloudServer.getBehaviour().asyncUpdateAll();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_UPDATE_COMPLETED);

		refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		cloudServer.getBehaviour().asyncUpdateAll();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_UPDATE_COMPLETED);
	}

	public void testScheduleRefreshHandlerRefreshApplication() throws Exception {
		// Tests both the CloudFoundryServerBehaviour refresh handler as well as
		// the test harness refresh listener
		String testName = "refreshApp";
		IProject project = createWebApplicationProject();

		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplicationWithModuleRefresh(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(
				appModule.getDeployedApplicationName(), cloudServer, CloudServerEvent.EVENT_MODULE_UPDATED);

		cloudServer.getBehaviour().asyncUpdateDeployedModule(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_MODULE_UPDATED);
	}

	public void testScheduleRefreshHandlerAllModuleInstances() throws Exception {

		String testName = "refreshAll2";
		IProject project = createWebApplicationProject();

		boolean startApp = true;

		String expectedAppName = harness.getWebAppName(testName);
		deployApplicationWithModuleRefresh(expectedAppName, project, startApp, harness.getDefaultBuildpack());

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		cloudServer.getBehaviour().asyncUpdateAll();

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_UPDATE_COMPLETED);
	}

	public void testScheduleRefreshHandlerDeploymentChange() throws Exception {

		String testName = "deploymentChange";
		IProject project = createWebApplicationProject();

		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);

		CloudFoundryApplicationModule appModule = deployApplicationWithModuleRefresh(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(null, cloudServer,
				CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);

		cloudServer.getBehaviour().asyncUpdateModuleAfterPublish(appModule.getLocalModule());

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);
	}

	public void testApplicationRefreshedEvent() throws Exception {
		// Tests the general event handler

		String testName = "refreshEvent";
		IProject project = createWebApplicationProject();

		String expectedAppName = harness.getWebAppName(testName);

		boolean startApp = true;
		deployApplicationWithModuleRefresh(expectedAppName, project, startApp, harness.getDefaultBuildpack());

		final IModule module = cloudServer.getExistingCloudModule(expectedAppName).getLocalModule();

		ModulesRefreshListener refreshListener = ModulesRefreshListener.getListener(expectedAppName, cloudServer,
				CloudServerEvent.EVENT_MODULE_UPDATED);

		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				ServerEventHandler.getDefault().fireModuleUpdated(cloudServer, module);
			}
		};

		asynchExecuteOperation(runnable);

		assertModuleRefreshedAndDispose(refreshListener, CloudServerEvent.EVENT_MODULE_UPDATED);
	}

	public void testModuleRefreshDuringServerConnect1() throws Exception {
		String testName = "refreshOnConnect";
		IProject project = createWebApplicationProject();
		String expectedAppName = harness.getWebAppName(testName);

		boolean startApp = true;
		deployApplicationWithModuleRefresh(expectedAppName, project, startApp, harness.getDefaultBuildpack());

		// Cloud module should have been created.
		CloudFoundryApplicationModule refreshedModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertEquals(expectedAppName, refreshedModule.getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());

		ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		serverBehavior.connect(new NullProgressMonitor());

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_UPDATE_COMPLETED);

		refreshedModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertEquals(expectedAppName, refreshedModule.getDeployedApplicationName());
	}

	public void testModuleRefreshDuringServerConnect2() throws Exception {
		// Deploy and start an application.
		// Disconnect through the server behaviour. Verify through an external
		// client that the app
		// remains deployed and in started mode.
		// Reconnect, and verify that the application is still running (i.e.
		// disconnecting
		// the server should not stop the application).

		String testName = "refreshOnConnect2";
		String expectedAppName = harness.getWebAppName(testName);

		IProject project = createWebApplicationProject();

		// Note that deploying application fires off an app change event AFTER
		// the deployment is
		// successful. To make sure that the second event listener further down
		// does not accidentally receive the app
		// change event,
		// wait for the app change event from the deploy first, and then
		// schedule the second listener to
		// listen to the expected refresh event
		boolean startApp = true;
		deployApplicationWithModuleRefresh(expectedAppName, project, startApp, harness.getDefaultBuildpack());

		// Cloud module should have been created.
		CloudFoundryApplicationModule refreshedModule = cloudServer.getExistingCloudModule(expectedAppName);
		assertEquals(expectedAppName, refreshedModule.getDeployedApplicationName());

		// Disconnect and verify that there are no cloud foundry application
		// modules
		serverBehavior.disconnect(new NullProgressMonitor());
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());

		// Check that app still exists via external client (i.e. the disconnect did not delete the actual app in CF)
		CloudApplication deployedApp = getAppFromExternalClient(expectedAppName);
		assertEquals(expectedAppName, deployedApp.getName());
		assertTrue(deployedApp.getState() == AppState.STARTED);

		// Register a module refresh listener before connecting again to be
		// notified when
		// modules are refreshed
		ModulesRefreshListener listener = getModulesRefreshListener(null, cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		serverBehavior.connect(new NullProgressMonitor());

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_UPDATE_COMPLETED);

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		assertEquals(expectedAppName, appModule.getDeployedApplicationName());
	}

}
