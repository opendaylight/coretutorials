# Introduce YANG data model and allow restconf access to it

- purpose is to show that with only a yang data model, restconf can be used to read from and write to the config data store
- no java code added to toaster-impl
- show that restconf write access is available to config data store; note that NO write access to the operational datastore is allowed
- do a restconf “put" (flowed by a get) on the configurable toaster parameter 'darkness factor’ from the data model in toaster.yang
- using postman/restconf to a put on http://localhost:8181/restconf/config/toaster:toaster, contents is {"toaster":{"darknessFactor”:2000}}
- perform a “get” to verify the put worked
- introduce the ODL restconf api docs tool which has web based browser access to the data store
- at the karaf console, type "feature:install odl-mdsal-apidocs"
- in a web browser, type http://localhost:8181/apidoc/explorer/index.html, click on the toaster, play with the tool, its quite intuitive
- apidocs seems to work but that karaf log is riddled with java exceptions
