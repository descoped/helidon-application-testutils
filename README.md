# Application Server Test Utils

## Planning

Features:

* Prepare TestServer-suppliers during TestPlan start, in which will offer Lazy instantiation of each TestServer
* A specialized TestServer is provided per test-method given test scope: Container, Class, Method
* Each TestServer has its own Deployment. The MVP is limited to *one deployment* per Test Class
* A TestServer is kept running during the Test Scope Lifecycle, meaning method level tests will start/stop a test server per invocation, or class, or container (suite)
* Make a DI-ObjectFactory (spi) for lookup of DI-objects in e.g. TestServerFactory


TestServerExecutionListener (spi): 

walk TestPlan: 
    scan all test classes and methods for Default, ConfigurationProfile and ConfigurationOverride
    scan for all Deployment methods (bound to class.<static>method)
    build TestServer Supplier factory

TestServerExtension:

A test case will run an Extension with @ExtendWith(extension)
The Extension implements the execution phases that should be intercepted during the test-execution lifecycle

    ParamterResolver:
        Use ObjectFactory to lookup instances

    BeforeAll:
        Find nearest TestConfigurationBinding (method, class, container)
        Determine TestServerSupplier



TODO:

* (refactoring) Do NOT pass Factory as Constructor param to another Factory
* TestConfigurationBinding use Source instead of specific class/method

