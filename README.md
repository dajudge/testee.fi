# TestEE.fi
TestEE.fi (as in "testify") is a lightweight functional testing framework for Java-EE applications.

By conveniently integrating a fully blown CDI implentation (TestEE.fi internally uses Weld, the CDI reference implementation) with much loved mocking frameworks like Mockito and EasyMock and proven database setup tools like Flyway and Liquibase it offers a natural way to write tests for your Java-EE application the same way you write your unit tests. And by integrating with test-runtimes like JUnit 4 & 5 or Cucumber JVM it allows you to run your tests directly from your preferred IDE and build tool - at speeds very close to unit-level testing.

## Getting started

Getting started with TestEE.fi is really easy. First you need to add TestEE.fi to your project’s dependencies. In Maven this looks like the following snippet:
```
<dependencies>
    <dependency>
        <groupId>fi.testee</groupId>
        <artifactId>testeefi-junit4-all</artifactId>
        <version>0.6.1</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.1.7</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```
If you’re using Gradle, the snippet would look like this:

```
repositories {
    mavenCentral()
}
dependencies {
	testCompile 'fi.testee:testeefi-junit4-all:0.6.1'
	testCompile 'ch.qos.logback:logback-classic:1.1.7'
}
```

All you need to do now is to annotate your JUnit 4 test case like this:

```
@RunWith(TestEEfi.class)
public class MyTest {
	@Mock
	private ExternalSystemBean externalSystem;
	@Inject
	private FacadeBean facadeToTest;
	
	@Test
	public void runTest() {
		// Your test code
	}
}
```

## Introduction and tutorials
The series of blog articles "Functional testing Java-EE applications with TestEE.fi" serves the purpose of introductory tutorials
very well:
* [Part 1: Introduction](https://blog.alexstockinger.de/2017/07/28/functional-testing-java-ee-applications-with-testeefi-part-1-introduction/)
* [Part 2: Mocking with Mockito and EasyMock](https://blog.alexstockinger.de/2017/08/05/functional-testing-java-ee-applications-with-testee-fi-part-2-mocking-with-mockito-and-easymock/)
* [Part 3: JDBC and JPA](https://blog.alexstockinger.de/2017/08/11/functional-testing-java-ee-applications-with-testee-fi-part-3-jdbc-and-jpa/)
* [Part 4: Cucumber JVM](https://blog.alexstockinger.de/2017/08/22/functional-testing-java-ee-applications-with-testee-fi-part-4-cucumber-jvm/)
* [Part 5: JAX-RS and REST resources](https://blog.alexstockinger.de/2017/08/31/functional-testing-java-ee-applications-with-testee-fi-part-5-jax-rs-and-rest-resources/)
* [Part 6: JUnit 5](https://blog.alexstockinger.de/2017/09/13/functional-testing-java-ee-applications-with-testee-fi-part-6-junit-5/)
* Part 7: Static resources and Selenium (coming soon)
* Part 8: JMS and MDBs (coming soon)

## Example projects
There's also a number of example projects based on a Gradle build that
demonstrate the usage of TestEE.fi. You can find them in a [separate repository](https://github.com/dajudge/testee.fi-examples). 
