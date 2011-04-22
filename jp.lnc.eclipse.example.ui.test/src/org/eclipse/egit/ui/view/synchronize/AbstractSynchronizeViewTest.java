/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.view.synchronize;

import static org.eclipse.egit.ui.UIText.CommitAction_commit;
import static org.eclipse.egit.ui.UIText.CommitDialog_Commit;
import static org.eclipse.egit.ui.UIText.CommitDialog_CommitChanges;
import static org.eclipse.egit.ui.UIText.CommitDialog_SelectAll;
import static org.eclipse.egit.ui.UIText.GitModelWorkingTree_workingTree;
import static org.eclipse.egit.ui.test.ContextMenuHelper.clickContextMenu;
import static org.eclipse.egit.ui.test.TestUtil.waitUntilTreeHasNodeContainsText;
import static org.eclipse.jface.dialogs.MessageDialogWithToggle.NEVER;
import static org.eclipse.team.internal.ui.IPreferenceIds.SYNCHRONIZING_COMPLETE_PERSPECTIVE;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.op.ResetOperation.ResetType;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.test.Eclipse;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractSynchronizeViewTest extends
		LocalRepositoryTestCase {

	protected static final String INITIAL_TAG = "initial-tag";

	protected static final String TEST_COMMIT_MSG = "test commit";

	protected static final String EMPTY_PROJECT = "EmptyProject";

	protected static final String EMPTY_REPOSITORY = "EmptyRepository";

	protected static File repositoryFile;

	@Before public void setupViews() {
		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();
	}

	@BeforeClass public static void setupEnvironment() throws Exception {
		// disable perspective synchronize selection
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(
				SYNCHRONIZING_COMPLETE_PERSPECTIVE, NEVER);

		repositoryFile = createProjectAndCommitToRepository();
		createChildRepository(repositoryFile);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);

		bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
		bot.viewByTitle("Package Explorer").show();

		createTag(INITIAL_TAG);
	}

	@AfterClass public static void restoreEnvironmentSetup() throws Exception {
		new Eclipse().reset();
	}

	protected void changeFilesInProject() throws Exception {
		SWTBot packageExlBot = bot.viewByTitle("Package Explorer").bot();
		SWTBotTreeItem coreTreeItem = selectProject(PROJ1, packageExlBot.tree());
		SWTBotTreeItem rootNode = coreTreeItem.expand().getNode(0)
				.expand().select();
		rootNode.getNode(0).select().doubleClick();

		SWTBotEditor corePomEditor = bot.editorByTitle(FILE1);
		corePomEditor.toTextEditor()
				.insertText("<!-- EGit jUnit test case -->");
		corePomEditor.saveAndClose();

		rootNode.getNode(1).select().doubleClick();
		SWTBotEditor uiPomEditor = bot.editorByTitle(FILE2);
		uiPomEditor.toTextEditor().insertText("<!-- EGit jUnit test case -->");
		uiPomEditor.saveAndClose();
		coreTreeItem.collapse();
	}

	protected void resetRepositoryToCreateInitialTag() throws Exception {
		ResetOperation rop = new ResetOperation(
				lookupRepository(repositoryFile), Constants.R_TAGS +
						INITIAL_TAG, ResetType.HARD);
		rop.execute(new NullProgressMonitor());
	}

	public static void createTag(String tagName)
			throws Exception {
		new Git(lookupRepository(repositoryFile)).tag().setName(tagName)
				.setMessage(tagName).call();
	}

	protected void makeChangesAndCommit(String projectName) throws Exception {
		changeFilesInProject();
		commit(projectName);
	}

	protected void deleteFileAndCommit(String projectName) throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJ1)
				.getFile(new Path("folder/test.txt")).delete(true, null);
		commit(projectName);
	}

	protected void launchSynchronization(String srcRepo, String srcRef,
			String dstRepo, String dstRef, boolean includeLocal)
			throws InterruptedException {
		launchSynchronization(REPO1, PROJ1, srcRepo, srcRef, dstRepo, dstRef,
				includeLocal);
	}

	protected void launchSynchronization(String repo, String projectName,
			String srcRepo, String srcRef, String dstRepo, String dstRef,
			boolean includeLocal) throws InterruptedException {
		showDialog(projectName, "Team", "Synchronize...");

		bot.shell("Synchronize repository: " + repo + File.separator + ".git");

		if (!includeLocal)
			bot.checkBox(
					UIText.SelectSynchronizeResourceDialog_includeUncommitedChanges)
					.click();

		if (!includeLocal && srcRepo != null)
			bot.comboBox(0)
					.setSelection(srcRepo);
		if (!includeLocal && srcRef != null)
			bot.comboBox(1).setSelection(srcRef);

		if (dstRepo != null)
			bot.comboBox(2)
					.setSelection(dstRepo);
		if (dstRef != null)
			bot.comboBox(3).setSelection(dstRef);

		bot.button(IDialogConstants.OK_LABEL).click();

		Job.getJobManager().join(
				ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION, null);
	}

	protected void setGitChangeSetPresentationModel() throws Exception {
		String modelName = util.getPluginLocalizedValue("ChangeSetModel.name");
		setPresentationModel(modelName, "Show " + modelName);
	}

	protected SWTBot setPresentationModel(String model) throws Exception {
		SWTBotView syncView = bot.viewByTitle("Synchronize");
		SWTBotToolbarDropDownButton dropDown = syncView
				.toolbarDropDownButton("Show File System Resources");
		dropDown.menuItem(model).click();
		// hide drop down
		dropDown.pressShortcut(KeyStroke.getInstance("ESC"));

		return syncView.bot();
	}

	protected SWTBot setPresentationModel(String modelName,
			String toolbarDropDownTooltip) throws Exception {
		SWTBotView syncView = bot.viewByTitle("Synchronize");
		SWTBotToolbarDropDownButton dropDown = syncView
				.toolbarDropDownButton(toolbarDropDownTooltip);
		dropDown.menuItem(modelName).click();
		// hide drop down
		dropDown.pressShortcut(KeyStroke.getInstance("ESC"));

		return syncView.bot();
	}

	// based on LocalRepositoryTestCase#createProjectAndCommitToRepository(String)
	protected void createEmptyRepository() throws Exception {
		File gitDir = new File(new File(getTestDirectory(), EMPTY_REPOSITORY),
				Constants.DOT_GIT);
		gitDir.mkdir();
		Repository myRepository = new FileRepository(gitDir);
		myRepository.create();

		// we need to commit into master first
		IProject firstProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(EMPTY_PROJECT);

		if (firstProject.exists())
			firstProject.delete(true, null);
		IProjectDescription desc = ResourcesPlugin.getWorkspace()
				.newProjectDescription(EMPTY_PROJECT);
		desc.setLocation(new Path(new File(myRepository.getWorkTree(),
				EMPTY_PROJECT).getPath()));
		firstProject.create(desc, null);
		firstProject.open(null);

		IFolder folder = firstProject.getFolder(FOLDER);
		folder.create(false, true, null);
		IFile textFile = folder.getFile(FILE1);
		textFile.create(new ByteArrayInputStream("Hello, world"
				.getBytes(firstProject.getDefaultCharset())), false, null);
		IFile textFile2 = folder.getFile(FILE2);
		textFile2.create(new ByteArrayInputStream("Some more content"
				.getBytes(firstProject.getDefaultCharset())), false, null);

		new ConnectProviderOperation(firstProject, gitDir).execute(null);
	}

	protected SWTBotEditor getCompareEditorForFileInGitChangeSet(
			String fileName,
			boolean includeLocalChanges) {
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		SWTBotTreeItem rootTree;
		if (includeLocalChanges)
			rootTree = waitForNodeWithText(syncViewTree,
					GitModelWorkingTree_workingTree);
		else
			rootTree = waitForNodeWithText(syncViewTree, TEST_COMMIT_MSG);

		SWTBotTreeItem projNode = waitForNodeWithText(rootTree, PROJ1);
		SWTBotTreeItem folderNode = waitForNodeWithText(projNode, FOLDER);
		waitForNodeWithText(folderNode, fileName).doubleClick();

		return bot.editorByTitle(fileName);
	}

	protected SWTBotTreeItem waitForNodeWithText(SWTBotTree tree, String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 10000);
		return getTreeItemContainingText(tree.getAllItems(), name).expand();
	}

	protected SWTBotTreeItem waitForNodeWithText(SWTBotTreeItem tree,
			String name) {
		waitUntilTreeHasNodeContainsText(bot, tree, name, 15000);
		return getTreeItemContainingText(tree.getItems(), name).expand();
	}

	protected SWTBotEditor getCompareEditorForFileInWorkspaceModel()
			throws Exception {
		SWTBotTree syncViewTree = setPresentationModel("Workspace").tree();
		SWTBotTreeItem projectTree = waitForNodeWithText(syncViewTree, PROJ1);
		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		waitForNodeWithText(folderTree, FILE1).doubleClick();

		return bot.editorByTitle(FILE1);
	}

	protected SWTBotEditor getCompareEditorForFileInGitChangeSetModel()
			throws Exception {
		SWTBotTree syncViewTree = setPresentationModel("Git Change Set")
				.tree();
		SWTBotTreeItem commitNode = syncViewTree.getAllItems()[0];
		commitNode.expand();
		SWTBotTreeItem projectTree = waitForNodeWithText(commitNode, PROJ1);
		SWTBotTreeItem folderTree = waitForNodeWithText(projectTree, FOLDER);
		waitForNodeWithText(folderTree, FILE1).doubleClick();

		SWTBotEditor editor = bot.editorByTitle(FILE1);
		editor.toTextEditor().setFocus();

		return editor;
	}

	protected SWTBotEditor getCompareEditorForFileInWorspaceModel(
			String fileName) {
		SWTBotTree syncViewTree = bot.viewByTitle("Synchronize").bot().tree();

		SWTBotTreeItem projNode = waitForNodeWithText(syncViewTree, PROJ1);
		SWTBotTreeItem folderNode = waitForNodeWithText(projNode, FOLDER);
		waitForNodeWithText(folderNode, fileName).doubleClick();

		SWTBotEditor editor = bot.editorByTitle(fileName);
		editor.toTextEditor().setFocus();

		return editor;
	}

	private void commit(String projectName) throws InterruptedException {
		showDialog(projectName, "Team", CommitAction_commit);

		bot.shell(CommitDialog_CommitChanges).bot().activeShell();
		bot.styledText(0).setText(TEST_COMMIT_MSG);
		bot.button(CommitDialog_SelectAll).click();
		bot.button(CommitDialog_Commit).click();
		TestUtil.joinJobs(JobFamilies.COMMIT);
	}

	private static void showDialog(String projectName, String... cmd) {
		SWTBot packageExplorerBot = bot.viewByTitle("Package Explorer").bot();
		packageExplorerBot.activeShell();
		SWTBotTree tree = packageExplorerBot.tree();

		// EGit decorates the project node shown in the package explorer. The
		// '>' decorator indicates that there are uncommitted changes present in
		// the project. Also the repository and branch name are added as a
		// suffix ('[<repo name> <branch name>]' suffix). To bypass this
		// decoration we use here this loop.
		selectProject(projectName, tree);

		clickContextMenu(tree, cmd);
	}

	private static SWTBotTreeItem selectProject(String projectName,
			SWTBotTree tree) {
		for (SWTBotTreeItem item : tree.getAllItems()) {
			if (item.getText().contains(projectName)) {
				item.select();
				return item;
			}
		}

		throw new RuntimeException("Poject with name " + projectName +
				" was not found in given tree");
	}

	private SWTBotTreeItem getTreeItemContainingText(SWTBotTreeItem[] items,
			String text) {
		for (SWTBotTreeItem item : items)
			if (item.getText().contains(text))
				return item;

		throw new WidgetNotFoundException(
					"Tree item elment containing text: test commit was not found");
	}

}
