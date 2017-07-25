# TestEE.fi

## Getting started

**Caveat:** _TestEE.fi is not hosted on maven central, yet, so you need to push it to your own repository or install it into your local repository before using it._

Getting started with TestEE.fi is really easy. First you need to add TestEE.fi to your project’s dependencies. In maven this looks like the following snippet:
```
<dependencies>
	<dependency>
		<groupId>fi.testee</groupId>
		<artifactId>testeefi-junit4-all</artifactId>
		<version>0.0.1-SNAPSHOT</version>
		<scope>test</scope>
	</dependency>
	
	<!-- TestEE.fi uses slf4j, so add logback as logging implementation -->
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
dependencies {
	testCompile 'fi.testee:testeefi-junit4-all:0.0.1-SNAPSHOT'
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
