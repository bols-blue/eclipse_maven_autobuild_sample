/*******************************************************************************
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package jp.lnc.eclipse.example.ui.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { //AllRepositoriesViewTests.class, 	//
	ExsampleSWTbot.class
	//Eclipse.class
	})
public class AllLocalTests {
	// empty class, don't need anything here
}
