/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package steps;

import cucumber.api.java.en.Then;
import fi.testee.cucumber.beans.ManagedBean;
import fi.testee.cucumber.beans.MockBean;
import fi.testee.cucumber.beans.SessionBean;
import fi.testee.mocking.annotation.InjectMock;

import javax.ejb.EJB;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;

public class TestSteps {
    @Inject
    private ManagedBean managedBean;
    @EJB
    private SessionBean sessionBean;
    @InjectMock
    private MockBean mockBean;

    @Then("^Managed bean got injected$")
    public void assertManagedBean() {
        assertThat(managedBean, notNullValue());
    }

    @Then("^Session bean got injected$")
    public void assertSessionBean() {
        assertThat(sessionBean, notNullValue());
    }

    @Then("^CucumberSetup was post constructed")
    public void postconstructed() {
        verify(mockBean).postConstruct();
    }

}
