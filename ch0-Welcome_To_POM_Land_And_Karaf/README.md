# Basic directory structure, pom's, and karaf for the toaster project.

This is the base folder structure. Note that when you see "toaster", replace with "your-project"

- Naming of folders is up to the discretion of each project, and this is an example.
- For complex projects, this structure may not be suitable.

This chapter introduces the basic skeletal project folder structure, the maven POMs, karaf features and a local
karaf distribution.  Each of the subdirectories has a basic pom.xml enabling us to build the basic features for
the project.  The features will not do anything but they will be installable in karaf.

# What is karaf and what is a karaf feature bundle

Apache Karaf is an OSGi powered container that natively supports the deployment of OSGi applications.
In OSGi, a bundle can depend on other bundles. So, it means that to deploy an OSGi application, most of the time,
you have to firstly deploy a lot of other bundles required by the application. 

Apache Karaf provides a simple and flexible way to provision applications. In Apache Karaf, the application
provisioning is an Apache Karaf "feature".

A feature describes an application as:

* a name
* a version
* a optional description (eventually with a long description)
* a set of bundles
* optionally a set configurations or configuration files
* optionally a set of dependency features

When you install a feature, Apache Karaf installs all resources described in the feature. It
automatically resolves and installs all bundles, configurations, and dependency features described in the feature.

In this simple application, we will perform following steps to illustrate a karaf feature bundle - 

* Define a feature with dependency.
* Add feature repo in karaf.
* Deploy feature in karaf.

# Defining a karaf feature

The toaster feature bundle is defined in features.xml file under features directory.

In the snippet below, we have added 3 features __odl-toaster-api__, __odl-toaster-impl__, and __odl-toaster-consumer__.
Each of these features may depend on other features so they are listed under each feature. At the time of feature
installation, all dependent features are automatically installed by karaf, provided they are available in karaf.
Don't worry too much at this point, the details of the features.xml file.  

```

    <features name="odl-toaster-${project.version}" xmlns="http://karaf.apache.org/xmlns/features/v1.2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://karaf.apache.org/xmlns/features/v1.2.0 http://karaf.apache.org/xmlns/features/v1.2.0">

        <!-- TODO: for any repo dependencies, add an entry in the features/pom.xml dependency section -->
        <repository>mvn:org.opendaylight.yangtools/features-yangtools/${yangtools.version}/xml/features</repository>
        <repository>mvn:org.opendaylight.controller/features-mdsal/${controller.mdsal.version}/xml/features</repository>

        <!-- TODO: for any feature dependencies, add an entry in the features/pom.xml dependency section -->
    
        <feature name='odl-toaster-api' version='${project.version}' description='OpenDaylight :: toaster :: API'>
            <feature version='${yangtools.version}'>odl-yangtools-common</feature>
            <feature version='${yangtools.version}'>odl-yangtools-binding</feature>
            <bundle>mvn:org.opendaylight.toaster/toaster-api/${project.version}</bundle>
        </feature>

        <feature name='odl-toaster-impl' version='${project.version}' description='OpenDaylight :: toaster :: Impl'>
            <feature version='${controller.mdsal.version}'>odl-mdsal-broker</feature>
            <feature version='${project.version}'>odl-toaster-api</feature>
            <bundle>mvn:org.opendaylight.toaster/toaster-impl/${project.version}</bundle>
        </feature>

        <feature name='odl-toaster-consumer' version='${project.version}' description='OpenDaylight :: toaster :: Consumer'>
            <feature version='${controller.mdsal.version}'>odl-mdsal-broker</feature>
            <feature version='${project.version}'>odl-toaster-api</feature>
            <bundle>mvn:org.opendaylight.toaster/toaster-consumer/${project.version}</bundle>
        </feature>
    </features>

```

# Building the maven toaster project

To build the project, run __mvn clean install__ in the top level directory.
It will make _features.xml_ available in local maven repository. You can find your features.xml file at

 _.m2/repository/org/opendaylight/toaster/0.0.1-SNAPSHOT/toaster-features-0.0.1-SNAPSHOT-features.xml_
 
# Starting up karaf and verifying the toaster features are available

Go to the bin directory of your karaf distribution and run the following commands - 

	cd ./distribution-karaf/target/assembly/bin
	./karaf

To see if your feature is available to install, run following command on karaf console - 

	feature:list
	
It will list all the features available in the karaf instance. You can use grep to reduce the list with command such as - 

	feature:list | grep toaster

You should see your feature in the list, and an 'x' should be display indicating hte feature is installed.

	feature:install toaster-api
	feature:install toaster-impl
	feature:install toaster-consumer
	
In the next chapters, we need to at some code so you can actually see your features so some actual work.

That's all for this chapter, there is nothing more to do, goto next chapter

