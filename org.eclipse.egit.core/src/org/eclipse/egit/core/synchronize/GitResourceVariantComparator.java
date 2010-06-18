/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.synchronize.GitBlobResourceVariant;
import org.eclipse.egit.core.synchronize.GitFolderResourceVariant;
import org.eclipse.egit.core.synchronize.GitResourceVariant;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

class GitResourceVariantComparator implements IResourceVariantComparator {

	private final GitSynchronizeDataSet gsd;
	private final ResourceVariantByteStore store;

	public GitResourceVariantComparator(GitSynchronizeDataSet dataSet, ResourceVariantByteStore store) {
		gsd = dataSet;
		this.store = store;
	}

	public boolean compare(IResource local, IResourceVariant remote) {
		if (!local.exists() || remote == null) {
			return false;
		}

		if (local instanceof IFile) {
			if (remote.isContainer()) {
				return false;
			}

			InputStream stream = null;
			InputStream remoteStream = null;
			try {
				remoteStream = remote.getStorage(
						new NullProgressMonitor()).getContents();
				stream = getLocal(local);
				byte[] remoteBytes = new byte[8096];
				byte[] bytes = new byte[8096];

				int remoteRead = remoteStream.read(remoteBytes);
				int read = stream.read(bytes);
				if (remoteRead != read) {
					return false;
				}

				while (Arrays.equals(bytes, remoteBytes)) {
					remoteRead = remoteStream.read(remoteBytes);
					read = stream.read(bytes);
					if (remoteRead != read) {
						// didn't read the same amount, it's uneven
						return false;
					} else if (read == -1) {
						// both at EOF, check their contents
						return Arrays.equals(bytes, remoteBytes);
					}
				}
			} catch (IOException e) {
				logException(e);
				return false;
			} catch (CoreException e) {
				logException(e);
				return false;
			} finally {
				closeStream(stream);
				closeStream(remoteStream);
			}
		} else if (local instanceof IContainer) {
			if (!remote.isContainer()) {
				return false;
			}

			GitFolderResourceVariant gitVariant = (GitFolderResourceVariant) remote;
			return local.getFullPath().equals(
					gitVariant.getResource().getFullPath());
		}
		return false;
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		GitResourceVariant gitBase = (GitResourceVariant) base;
		GitResourceVariant gitRemote = (GitResourceVariant) remote;
		IResource resourceBase = gitBase.getResource();
		IResource resourceRemote = gitRemote.getResource();

		if (!resourceBase.exists() || !resourceRemote.exists()) {
			return false;
		}

		if (base.isContainer()) {
			if (remote.isContainer()) {
				return resourceBase.getFullPath().equals(
						resourceRemote.getFullPath());
			}
			return false;
		} else if (remote.isContainer()) {
			return false;
		}

		GitBlobResourceVariant baseBlob = (GitBlobResourceVariant) base;
		GitBlobResourceVariant remoteBlob = (GitBlobResourceVariant) remote;
		return baseBlob.getId().equals(remoteBlob.getId());
	}

	public boolean isThreeWay() {
		return true;
	}

	private InputStream getLocal(IResource resource) throws CoreException {
		if (gsd.getData(resource.getProject()).shouldIncludeLocal()) {
			return ((IFile) resource).getContents();
		} else {
			try {
				byte[] bytes = store.getBytes(resource);
				return new ByteArrayInputStream(bytes != null ? bytes : new byte[0]);
			} catch (TeamException e) {
				throw new CoreException(e.getStatus());
			}
		}

	}

	private void logException(Exception e) {
		IStatus error = new Status(IStatus.ERROR, Activator
				.getPluginId(), e.getMessage(), e);
		Activator.getDefault().getLog().log(error);
	}

	private void closeStream(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				logException(e);
			}
		}
	}

}