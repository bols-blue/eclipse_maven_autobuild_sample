/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Seelmann - initial implementation posted to
 *    http://www.eclipse.org/forums/index.php?t=msg&th=11863&start=2
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
 
@RunWith(SWTBotJunit4ClassRunner.class)
public class ContextMenuHelper {


	 
		private static SWTWorkbenchBot	bot;
	 
		@BeforeClass
		public static void beforeClass() throws Exception {
			bot = new SWTWorkbenchBot();
			bot.viewByTitle("Welcome").close();
		}
	 
	 
		@Test
		public void canCreateANewJavaProject() throws Exception {
			bot.menu("File").menu("New").menu("Project...").click();
	 
			SWTBotShell shell = bot.shell("New Project");
			shell.activate();
			bot.tree().expandNode("Java").select("Java Project");
			bot.button("Next >").click();
	 
			bot.textWithLabel("Project name:").setText("MyFirstProject");
	 
			bot.button("Finish").click();
			// FIXME: assert that the project is actually created, for later
		}
	 
	 
		@AfterClass
		public static void sleep() {
			bot.sleep(2000);
		}

}