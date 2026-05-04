Feature: Login user

  Scenario: Login successfully
    Given there is already a user registered with email "john@example.com" and password "StrongPass123"
    When I login with email "john@example.com" and password "StrongPass123"
    Then the login should be successful
    And the response should contain an access token

  Scenario: Prevent login with wrong password
    Given there is already a user registered with email "john@example.com" and password "StrongPass123"
    When I login with email "john@example.com" and password "WrongPass123"
    Then the login should fail
    And the error message should be "Invalid credentials"

  Scenario: Prevent login with nonexistent email
    Given there is no user registered with email "missing@example.com"
    When I login with email "missing@example.com" and password "StrongPass123"
    Then the login should fail
    And the error message should be "Invalid credentials"