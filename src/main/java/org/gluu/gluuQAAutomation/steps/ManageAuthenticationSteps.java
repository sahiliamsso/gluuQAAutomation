package org.gluu.gluuQAAutomation.steps;

import org.gluu.gluuQAAutomation.pages.configuration.authentication.CasProtocolPage;
import org.gluu.gluuQAAutomation.pages.configuration.authentication.DefaultAuthenMethodPage;
import org.gluu.gluuQAAutomation.pages.configuration.authentication.PassportAuthenticationPage;
import org.gluu.gluuQAAutomation.pages.login.HomePage;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@RunWith(SpringRunner.class)
public class ManageAuthenticationSteps extends BaseSteps {
	@Autowired
	private HomePage homePage;

	@Autowired
	private PassportAuthenticationPage authenticationPage;
	@Autowired
	private DefaultAuthenMethodPage defaultAuthenMethodPage;
	@Autowired
	private CasProtocolPage casProtocolPage;

	@When("^I go to strategy page$")
	public void goToAuthenticationManagePage() {
		homePage.goToManageAutheticationMenuPage();
		authenticationPage.selectTab("Passport Authentication Method");
	}

	@When("^I go to default authentication page")
	public void goToDefaultAuthenMethodPage() {
		homePage.goToManageAutheticationMenuPage();
		authenticationPage.selectTab("Default Authentication Method");
	}

	@When("^I go to CAS protocol page")
	public void goToCasProtocolPage() {
		homePage.goToManageAutheticationMenuPage();
		authenticationPage.selectTab("CAS Protocol");
	}

	@And("^I add new strategy named '(.+)' with id '(.+)' and secret '(.+)'$")
	public void addNewStrategy(String name, String id, String secret) {
		authenticationPage.addStrategy(name, id, secret);
	}

	@And("^I enable passport status to '(.+)'$")
	public void setPassportStatus(String status) {
		authenticationPage.setPassportStatus(status);
	}

	@And("^I save the passport status changed$")
	public void save() {
		authenticationPage.save();
	}

	@And("^I should see default acr set to '(.+)'")
	public void checkDefaultAcr(String defaultAcr) {
		defaultAuthenMethodPage.checkDefaultAcr(defaultAcr);
	}

	@And("^I set to default acr to '(.+)'$")
	public void setDefaultAcr(String acr) {
		defaultAuthenMethodPage.setDefaultAcr(acr);
	}

	@And("^I set to oxtrust acr to '(.+)'$")
	public void setOxtrustAcr(String acr) {
		defaultAuthenMethodPage.setOxtrustAcr(acr);
	}

	@And("^I should see oxtrust acr set to '(.+)'")
	public void checkOxtrustAcr(String acr) {
		defaultAuthenMethodPage.checkOxtrustAcr(acr);
	}

	@And("^I save the default method configuration$")
	public void saveConfiguration() {
		defaultAuthenMethodPage.save();
	}

	@Then("^I should see that the cas protocol is enable$")
	public void checkStatus() {
		casProtocolPage.assertStatusIsEnabled();
	}

	@And("^I should see that service type is '(.+)'$")
	public void checkServiceType(String type) {
		casProtocolPage.assertServiceTypeIs(type);
	}

	@And("^I should see that base url end with '/idp/profile/cas'$")
	public void checkBaseurl(String url) {
		casProtocolPage.assertBaseUrlEndWith(url);
	}

	@After
	public void clear() {
		homePage.clear();
	}

}
