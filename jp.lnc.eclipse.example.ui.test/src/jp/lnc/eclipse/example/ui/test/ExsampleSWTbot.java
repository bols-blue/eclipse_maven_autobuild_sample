package jp.lnc.eclipse.example.ui.test;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
 
@RunWith(SWTBotJunit4ClassRunner.class)
public class ExsampleSWTbot {
 
	private static SWTWorkbenchBot	bot;
 
	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		bot.sleep(2000);
//		bot.viewByTitle("Welcome").close();
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
		bot.sleep(1000);
		String test = bot.activeShell().getToolTipText();
		System.out.println(test);
		bot.button("Yes").click();
		// FIXME: assert that the project is actually created, for later
	}
 
	@AfterClass
	public static void sleep() {
		bot.sleep(2000);
	}
 
}
