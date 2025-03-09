# Integration of Allure report tool for End-to-End Testing Transparency for release process

## Decision

Integrate [Allure report tool](https://allurereport.org/) into Tractusx-edc project's end-to-end testing framework to provide transparent and detailed test results. 
The reports will be published to GitHub Pages and its URL will be included in the GitHub release notes for easy access and 
review by stakeholders detailed test results, ensuring clarity on what has been tested.

## Rationale

- **Enhanced Transparency:** 
Allure provides detailed and visually appealing test reports that offer insights into test execution, including 
description, steps, attachments, and history, making it easier to understand what was tested and the outcomes.
- **Compatibility with JUnit5:** 
As we use JUnit5, Allure's native support for this testing framework ensures seamless integration without requiring 
significant changes to our existing setup.
- **Static HTML Reports:** 
Allure generates standalone HTML reports that are ideal for hosting on GitHub Pages, requiring no additional tools for 
clients to view them.
- **Client Accessibility:** 
Publishing the reports to GitHub Pages and linking them in GitHub releases allows clients to easily access and review 
the test coverage and results, fostering trust and clear communication.

## Approach

1) Setup and Integration:
- Integrate the necessary Allure dependencies into the project to support JUnit5 tests.
- Configure test settings so that Allure result files are generated during test execution, but delay report generation
until a release is initiated.
- Enhance test code with Allure-specific annotations such as `@Step` and `@Attachment` to enrich the reports with
detailed execution steps and supporting artifacts.
2) Create GitHub Actions workflow to automate the process during release publishing:
- Run the end-to-end tests.
- Generate the Allure report.
- Deploy the report to GitHub Pages.
- Publish a direct link to the Allure report hosted on GitHub Pages in the release notes.


