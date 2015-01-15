# Adding Hello World to the toaster-impl feature introduced in chapter 0

Now that your features can be installed in karaf, let's illustrate how your features can be "started" and do more
interesting things.

All programming platforms illustrate a "Hello World" example ... this is ODL's version of "Hello World"

We will add infrastructure to the toaster-impl so that the toaster can actually LOG a "Hello World" msg to the
karaf log file.

TODO: I needed to put the toaster-impl-config.xml file in the src/main/resources folder
TODO: Need a nice description toaster-impl-config.yang and why its needed.
TODO: Need a nice description of ToasterImpl.java
TODO: Need a nice description of ToasterImplModule.java ... its on the end of a super long directory chain ...

Once 'mvn clean install' is finished, enter karaf, search the karaf log for a toaster-impl message.
    ./distribution-karaf/target/assembly/bin
    ./karaf
    log:tail
    
Look for a log entry similar to this:
    ToasterImpl | 166 - org.opendaylight.toaster.impl - 0.0.1.SNAPSHOT | Hello World!
    
QUESTION: what happens when the java function that LOGs the "Hello World" msg returns?

That's all for this chapter, on to your data model, goto the next chapter.

