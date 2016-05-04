/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software Inc. and IBM Corporation 
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
 *     IBM Corporation - Add additional async invocation method
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.cft.server.ui.internal.ICoreRunnable;
import org.eclipse.cft.server.ui.internal.IEventSource;
import org.eclipse.cft.server.ui.internal.IPartChangeListener;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.PartChangeEvent;
import org.eclipse.cft.server.ui.internal.WizardPartChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page that manages multiple UI parts, and handles errors from each
 * part. In terms of errors, UI parts are each treated as atomic units, meaning
 * that any error generated by any part is considered to be from the part as a
 * whole, rather from individual controls in that part.
 * 
 * <p/>
 * In order for the page to manage errors from parts, the page MUST be added as
 * a listener to each UI Part that is created.
 * 
 */
public abstract class PartsWizardPage extends WizardPage implements IPartChangeListener {

	protected Map<IEventSource<?>, IStatus> partStatus = new HashMap<IEventSource<?>, IStatus>();

	protected PartsWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected IStatus getNextNonOKStatus() {
		// Check if there are other errors that haven't yet been resolved
		IStatus status = null;
		for (Entry<IEventSource<?>, IStatus> entry : partStatus.entrySet()) {
			status = entry.getValue();
			if (status != null && !status.isOK()) {
				break;
			}
		}
		return status;
	}

	public void handleChange(PartChangeEvent event) {
		IStatus status = event.getStatus();
		if (status == null) {
			status = Status.OK_STATUS;
		}

		// If the part indicates its OK, remove it from the list of tracked
		// parts, as any error it would have previously
		// generated has now been fixed.
		if (status.isOK()) {
			partStatus.remove(event.getSource());

			// Check if there are other errors that haven't yet been resolved
			for (Entry<IEventSource<?>, IStatus> entry : partStatus.entrySet()) {
				status = entry.getValue();
				break;
			}

		}
		else if (event.getSource() != null) {
			partStatus.put(event.getSource(), status);
		}

		boolean updateButtons = !(event instanceof WizardPartChangeEvent)
				|| ((WizardPartChangeEvent) event).updateWizardButtons();

		update(updateButtons, status);
	}

	@Override
	public boolean isPageComplete() {
		IStatus status = getNextNonOKStatus();
		return status == null;
	}

	/**
	 * This should be the ONLY way to notify the wizard page whether the page is
	 * complete or not, as well as display any error or warning messages.
	 * 
	 * <p/>
	 * 
	 * The wizard page will only be complete if it receives an OK status.
	 * 
	 * <p/>
	 * 
	 * It is up to the caller to correctly set the OK state of the page in case
	 * it sets a non-OK status, and the non-OK status gets resolved.
	 * 
	 * @param updateButtons true if force the wizard button states to be
	 * refreshed. NOTE that if true, it is up to the caller to ensure that the
	 * wizard page has been added to the wizard , and the wizard page is
	 * visible.
	 * @param status if status is OK, the wizard can complete. False otherwise.
	 */
	protected void update(boolean updateButtons, IStatus status) {
		if (status == null) {
			status = Status.OK_STATUS;
		}

		if (status.isOK()) {
			setErrorMessage(null);
		}
		else if (status.getSeverity() == IStatus.ERROR) {
			setErrorMessage(status.getMessage() != null ? status.getMessage()
					: Messages.PartsWizardPage_ERROR_UNKNOWN);
		}
		else if (status.getSeverity() == IStatus.INFO) {
			setMessage(status.getMessage(), DialogPage.INFORMATION);
		}
		else if (status.getSeverity() == IStatus.WARNING) {
			setMessage(status.getMessage(), DialogPage.WARNING);
		}

		// Container or page may not be available when update request is received
		if (updateButtons && getWizard() != null && getWizard().getContainer() != null
				&& getWizard().getContainer().getCurrentPage() != null) {
			getWizard().getContainer().updateButtons();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {

			if (getPreviousPage() == null) {
				// delay until dialog is actually visible
				if (!getControl().isDisposed()) {
					performWhenPageVisible();
				}
			}
			else {
				performWhenPageVisible();
			}
			IStatus status = getNextNonOKStatus();
			if (status != null) {
				update(true, status);
			}
		}
	}

	protected void performWhenPageVisible() {
		// Do nothing by default;
	}

	/** Runs the specific runnable without using the wizard container progress context */
	protected void runAsync(final ICoreRunnable runnable, String operationLabel) {
		if (runnable == null) {
			return;
		}
		
		if (operationLabel == null) {
			operationLabel = ""; //$NON-NLS-1$
		}

		Job job = new Job(operationLabel)  {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				CoreException cex = null;
				try {
					
					runnable.run(monitor);
				}
				catch (OperationCanceledException e) {
					// Not an error. User can still enter manual values
				}
				catch (CoreException ce) {
					cex = ce;
				}
				// Do not update the wizard with an error, as users can still
				// complete the wizard with manual values.
				if (cex != null) {
					CloudFoundryPlugin.logError(cex);
				}

				return Status.OK_STATUS;
				
			}
			
		};

		job.setSystem(false);
		job.schedule();

	}

	/**
	 * Runs the specified runnable asynchronously in a worker thread. Caller is
	 * responsible for ensuring that any UI behaviour in the runnable is
	 * executed in the UI thread, either synchronously (synch exec through
	 * {@link Display} or asynch through {@link Display} or {@link UIJob}).
	 * @param runnable
	 * @param operationLabel
	 */
	protected void runAsynchWithWizardProgress(final ICoreRunnable runnable, String operationLabel) {
		if (runnable == null) {
			return;
		}
		if (operationLabel == null) {
			operationLabel = ""; //$NON-NLS-1$
		}

		// Asynch launch as a UI job, as the wizard messages get updated before
		// and after the forked operation
		UIJob job = new UIJob(operationLabel) {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				CoreException cex = null;
				try {
					// Fork in a worker thread.
					CFUiUtil.runForked(runnable, getWizard().getContainer());
				}
				catch (OperationCanceledException e) {
					// Not an error. User can still enter manual values
				}
				catch (CoreException ce) {
					cex = ce;
				}
				// Do not update the wizard with an error, as users can still
				// complete the wizard with manual values.
				if (cex != null) {
					CloudFoundryPlugin.logError(cex);
				}

				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();
	}

}
