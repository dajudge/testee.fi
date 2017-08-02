package steps;

import cucumber.api.java.en.Given;
import fi.testee.examples.SomeBean;

import javax.inject.Inject;

public class CucumberSteps {
    @Inject
    private SomeBean someBean;

    @Given("^Cucumber works$")
    public void cucumberWorks() {
        if (someBean == null) {
            throw new AssertionError("someBean should not be null");
        }
    }
}
