# Integration of Allure Report tool for End-to-End Testing Transparency for release process

## Decision

Integrate [Allure Report](https://allurereport.org/) into Tractusx-edc project's end-to-end testing framework to provide test reports on 
milestone builds like releases or release candidates.  
The reports will be published to GitHub Pages and the report URL will be included in the GitHub release notes for easy 
access.

## Rationale

The current release process provides only very limited and developer-oriented means to validate the quality assurance 
measures taken during the release process. To improve the availability of quality assurance information for pure 
adopters of the Tractus-X EDC, a test reporting solution should provide an easy-to-read documentation of what has been 
tested, along with a clear statement that the tests were successful. The data presented in the test report is retrieved 
as static information defined in the code, together with test results achieved during the release build.

Several test reporting tools have been evaluated:
- [Report Portal](https://github.com/reportportal): Offers real-time monitoring and historical data tracking, 
but requires a dedicated server setup and is not ideal for generating static test reports for releases.
- [Extent Reports](https://github.com/extent-framework/extentreports-java): Previously a widely used reporting tool, 
it has been deprecated and replaced by [ChainTest](https://github.com/anshooarora/chaintest). 
However, ChainTest is still under development, making it an uncertain choice at this time.
- [Test Reporter](https://github.com/ctrf-io/github-test-reporter):  A GitHub Action-based tool designed to generate 
customizable test reports and display historical trends. While it provides basic test results and insights, it lacks the 
rich visual features and static HTML report generation needed for hosting on platforms like GitHub Pages.
- [Allure Report](https://github.com/allure-framework) : Provides visually rich, static HTML reports, seamless integration with JUnit5, and easy hosting 
on GitHub Pages, making it a strong candidate for transparent test reporting in the release process.

The evaluation result promotes the selection of Allure Test Report due to the most balanced solution, combining rich 
reports, ease of integration, and client accessibility.

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


