/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.op.UntrackOperation;
import org.eclipse.egit.ui.UIText;

/**
 * An action to remove files from a Git repository. The removal does not alter
 * history, only future commits on the same branch will be affected.
 *
 * @see UntrackOperation
 */
public class Untrack extends AbstractResourceOperationAction {
	protected IEGitOperation createOperation(final List<IResource> sel) {
		return sel.isEmpty() ? null : new UntrackOperation(sel);
	}

	@Override
	protected String getJobName() {
		return UIText.Untrack_untrack;
	}
}
