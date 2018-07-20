package org.gluu.gluuQAAutomation.steps;

import org.gluu.gluuQAAutomation.pages.login.HomePage;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import cucumber.api.java.After;
import cucumber.api.java.en.When;

@RunWith(SpringRunner.class)
public class ManageAuthenticationSteps extends BaseSteps {
	@Autowired
	private HomePage homePage;

	@When("^I go to strategy page$")
	public void goToAuthenticationManagePage() {
		homePage.goToManageAutheticationMenuPage();
	}

	@After
	public void clear() {
		homePage.clear();
	}

}
