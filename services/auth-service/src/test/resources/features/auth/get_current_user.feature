Feature: Get current authenticated user

  Scenario: Get current user successfully
    Given I am authenticated as user "john@example.com"
    When I request my current user profile
    Then the request should be successful
    And the response should contain the email "john@example.com"

  Scenario: Prevent access without token
    Given I am not authenticated
    When I request my current user profile
    Then the request should be unauthorized