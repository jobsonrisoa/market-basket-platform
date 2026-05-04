Feature: Register user

  Scenario: Register a new customer user successfully
    Given there is no user registered with email "john@example.com"
    When I register a user with name "John Doe", email "john@example.com" and password "StrongPass123"
    Then the user should be created successfully
    And the response should contain the user email "john@example.com"
    And the response should contain the role "CUSTOMER"

  Scenario: Prevent duplicated email registration
    Given there is already a user registered with email "john@example.com"
    When I register a user with name "John Doe", email "john@example.com" and password "StrongPass123"
    Then the registration should fail
    And the error message should be "Email already registered"

  Scenario: Prevent registration with invalid email
    Given there is no user registered with email "invalid-email"
    When I register a user with name "John Doe", email "invalid-email" and password "StrongPass123"
    Then the registration should fail
    And the error message should be "Invalid email"

  Scenario: Prevent registration with weak password
    Given there is no user registered with email "john@example.com"
    When I register a user with name "John Doe", email "john@example.com" and password "123"
    Then the registration should fail
    And the error message should be "Password is too weak"