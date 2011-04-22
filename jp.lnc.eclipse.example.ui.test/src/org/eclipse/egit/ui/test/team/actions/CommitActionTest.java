/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - tag API changes
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Commit action
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class CommitActionTest extends LocalRepositoryTestCase {
	private static File repositoryFile;

	private static SWTBotPerspective perspective;

	@BeforeClass
	public static void setup() throws Exception {
		repositoryFile = createProjectAndCommitToRepository();
		Repository repo = lookupRepository(repositoryFile);
		// TODO delete the second project for the time being (.gitignore is
		// currently not hiding the .project file from commit)
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ2).delete(
				false, null);

		TagBuilder tag = new TagBuilder();
		tag.setTag("SomeTag");
		tag.setTagger(RawParseUtils.parsePersonIdent(TestUtil.TESTAUTHOR));
		tag.setMessage("I'm just a little tag");
		tag.setObjectId(repo.resolve(repo.getFullBranch()), Constants.OBJ_COMMIT);
		TagOperation top = new TagOperation(repo, tag, false);
		top.execute(null);
		touchAndSubmit(null);

		perspective = bot.activePerspective();
		bot.perspectiveById("org.eclipse.pde.ui.PDEPerspective").activate();
		waitInUI();
	}

	@AfterClass
	public static void shutdown() {
		perspective.activate();
	}

	@Before
	public void prepare() throws Exception {
		Repository repo = lookupRepository(repositoryFile);
		if (!repo.getBranch().equals("master")) {
			BranchOperation bop = new BranchOperation(repo, "refs/heads/master");
			bop.execute(null);
		}
	}

	@Test
	public void testOpenCommitWithoutChanged() throws Exception {
		clickOnCommit();
		bot.shell(UIText.CommitAction_noFilesToCommit).close();
	}

	@Test
	public void testCommitSingleFile() throws Exception {
		setTestFileContent("I have changed this");
		clickOnCommit();
		SWTBotShell commitDialog = bot.shell(UIText.CommitDialog_CommitChanges);
		assertEquals("Wrong row count", 1, commitDialog.bot().table()
				.rowCount());
		assertTrue("Wrong file", commitDialog.bot().table().getTableItem(0)
				.getText(1).endsWith("test.txt"));
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Author).setText(
				TestUtil.TESTAUTHOR);
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Committer)
				.setText(TestUtil.TESTCOMMITTER);
		commitDialog.bot().styledTextWithLabel(UIText.CommitDialog_CommitMessage)
				.setText("The new commit");
		commitDialog.bot().button(UIText.CommitDialog_Commit).click();
		// wait until commit is completed
		Job.getJobManager().join(JobFamilies.COMMIT, null);
		testOpenCommitWithoutChanged();
	}

	@Test
	public void testAmendWithChangeIdPreferenceOff() throws Exception {
		Activator.getDefault()
			.getPreferenceStore()
			.setValue(UIPreferences.COMMIT_DIALOG_CREATE_CHANGE_ID, true);
		setTestFileContent("Another Change");
		clickOnCommit();
		SWTBotShell commitDialog = bot.shell(UIText.CommitDialog_CommitChanges);
		assertEquals("Wrong row count", 1, commitDialog.bot().table()
				.rowCount());
		assertTrue("Wrong file", commitDialog.bot().table().getTableItem(0)
				.getText(1).endsWith("test.txt"));
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Author).setText(
				TestUtil.TESTAUTHOR);
		commitDialog.bot().textWithLabel(UIText.CommitDialog_Committer)
				.setText(TestUtil.TESTCOMMITTER);
		String commitMessage = commitDialog.bot().styledTextWithLabel(UIText.CommitDialog_CommitMessage)
			.getText();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
		String newCommitMessage = "Change to be amended \n\n" + commitMessage;
		commitDialog.bot().styledTextWithLabel(UIText.CommitDialog_CommitMessage).
			setText(newCommitMessage);
		commitDialog.bot().button(UIText.CommitDialog_Commit).click();
		// wait until commit is completed
		Job.getJobManager().join(JobFamilies.COMMIT, null);

		clickOnCommit();
		Activator.getDefault()
			.getPreferenceStore()
			.setValue(UIPreferences.COMMIT_DIALOG_CREATE_CHANGE_ID, false);
		bot.shell(UIText.CommitAction_noFilesToCommit).bot().button(
				IDialogConstants.YES_LABEL).click();
		commitDialog = bot.shell(UIText.CommitDialog_CommitChanges);
		commitMessage = commitDialog.bot().styledTextWithLabel(
				UIText.CommitDialog_CommitMessage).getText();
		assertTrue(commitMessage.indexOf("Change-Id") > 0);
	}

	private void clickOnCommit() throws Exception {
		SWTBotTree projectExplorerTree = bot.viewById(
				"org.eclipse.jdt.ui.PackageExplorer").bot().tree();
		getProjectItem(projectExplorerTree, PROJ1).select();
		String menuString = util.getPluginLocalizedValue("CommitAction_label");
		ContextMenuHelper.clickContextMenu(projectExplorerTree, "Team",
				menuString);
	}
}
