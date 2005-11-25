package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;

public class CopyAction extends WorkspaceAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final IResource[] resources = getSelectedResources();
		final IProject project = resources[0].getProject();
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), project, false, Policy.bind("CopyAction.selectionLabel")); //$NON-NLS-1$
		if (dialog.open() == ContainerSelectionDialog.CANCEL) return;
		Object[] result = dialog.getResult();
		if (result == null || result.length == 0) return;
		final Path path = (Path)result[0];
		IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		final IProject targetProject = targetFile.getProject();
		final File destPath = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation() + File.separator + path.toString());
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
					for (int i = 0; i < resources.length; i++) {
						final IResource resource = resources[i];
						File checkFile = new File(destPath.getPath() + File.separator + resource.getName());
						File srcPath = new File(resource.getLocation().toString());
						File newDestPath = new File(destPath.getPath() + File.separator + resource.getName());
						if (checkFile.exists()) {
							IInputValidator inputValidator = new IInputValidator() {
								public String isValid(String newText) {
									if (newText.equals(resource.getName())) 
										return Policy.bind("CopyAction.nameConflictSame"); //$NON-NLS-1$
									return null;
								}								
							};
							InputDialog inputDialog = new InputDialog(getShell(), Policy.bind("CopyAction.nameConflictTitle"), Policy.bind("CopyAction.nameConflictMessage", resource.getName()), "Copy of " + resource.getName(), inputValidator); //$NON-NLS-1$
							if (inputDialog.open() == InputDialog.CANCEL) return;
							String newName = inputDialog.getValue();
							if (newName == null  || newName.trim().length() == 0) return;
							newDestPath = new File(destPath.getPath() + File.separator + newName);
						}
						if (project.getFullPath().isPrefixOf(path))
							client.copy(srcPath, newDestPath);
						else
							client.doExport(srcPath, newDestPath, true);
					}
					targetProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("CopyAction.copy"), e.getMessage()); //$NON-NLS-1$
				}
			}			
		});
	}

	protected boolean isEnabled() throws TeamException {
		// Only enabled if all selections are from same project.
		boolean enabled = super.isEnabled();
		if (!enabled) return false;
		IResource[] resources = getSelectedResources();
		IProject project = null;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] instanceof IProject) return false;
			if (project != null && !resources[i].getProject().equals(project)) return false;
			project = resources[i].getProject();
		}
		return true;
	}

}
