Feature: Change user password 
@gluuQA
Scenario: Change user password 
	When 	I sign in as administrator 
	Then 	I should see gluu home page 
	When 	I go to user add page 
	And 	I add a user named 'QaUserPasswordChanged' 
	And 	With first name 'QaUserPasswordChangedFN' 
	And 	With last name 'QaUserPasswordChangedLN' 
	And 	With display name 'QaUserPasswordChangedDN' 
	And 	With email 'QaUserPasswordChanged@gmail.com' 
	And 	With password 'QaUserPassword' 
	And 	With status 'Active' 
	And 	I save the user 
	When 	I go to users manage page 
	And 	I search for user with pattern 'QaUserPasswordChanged' 
	Then 	I should see a user named 'QaUserPasswordChanged' 
	When 	I start to update that user 
	And 	I set his password to 'QaUserPasswordChanged'
	And 	I sign out
	Then 	I should be able to login as 'QaUserPasswordChanged' with password 'QaUserPasswordChanged'
	And 	I sign out
	Then 	I sign in as administrator
	When 	I go to users manage page 
	And 	I search for user with pattern 'QaUserPasswordChanged' 
	Then 	I should see a user named 'QaUserPasswordChanged' 
	When 	I start to update that user 
	And 	I delete the current user
	When 	I search for user with pattern 'QaUserPasswordChanged' 
	Then 	I should not see a user named 'QaUserPasswordChanged'
	And 	I sign out 
	