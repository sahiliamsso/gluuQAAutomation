Feature: Add trust reletionship 
@gluuQA
Scenario: Add trust reletionship
	When 	I sign in as administrator 
	Then 	I should see gluu home page 
	When 	I go to tr add page
	Then 	I set 'QaTRAddDN' as display name
	And 	I set 'QaTRAddDesc' as description
	And 	I set 'Single SP' as entity type
	And 	I set 'File' as metadata location
	And 	I set the metadata
	And 	I configure sp with 'SAML2SSO' profile
	And 	I release the following attributes 'Username Email'
	And 	I save the current tr
	When 	I go to tr list page
	And 	I search for tr named 'QaTRAddDN'
	Then 	I should see a tr with display name 'QaTRAddDN' in the list
	When 	I delete the tr named 'QaTRAddDN' 
	And 	I go to tr list page 
	And 	I search for tr named 'QaTRAddDN' 
	Then 	I should not see a tr with display name 'QaTRAddDN' in the list 
	And 	I sign out