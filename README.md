# TestEE.fi
TestEE.fi (as in "testify") is a lightweight functional testing framework for Java-EE applications. By conveniently integrating a fully blown CDI implentation (TestEE.fi internally uses Weld, the CDI reference implementation) with much loved mocking frameworks like Mockito and EasyMock and proven database setup tools like Flyway and Liquibase it offers a natural way to write tests for your Java-EE application the same way you write your unit tests. And by integrating with test-runtimes like JUnit 4 & 5 or Cucumber JVM it allows you to run your tests directly from your preferred IDE and build tool - at speeds very close to unit-level testing.

## Getting started

Getting started with TestEE.fi is really easy. First you need to add TestEE.fi to your project’s dependencies. In maven this looks like the following snippet:
```
<dependencies>
    <dependency>
        <groupId>fi.testee</groupId>
        <artifactId>testeefi-junit4-all</artifactId>
        <version>0.2.1</version>
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
If you’re using gradle, the snippet would look like this:

```
repositories {
    mavenCentral()
}
dependencies {
	testCompile 'fi.testee:testeefi-junit4-all:0.2.1'
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
