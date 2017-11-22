# spark-groovy-skeleton

There are three Gradle targets for running the application:
 
* dev
* qa
* prod

Each have a config file related to them for necessary, well, config information.  Check out /src/main/groovy/conf/config-template.groovy.  If they don't exist, create 3 files in that directory:

* config-dev.groovy
* config-qa.groovy
* config-prod.groovy

And modify the contents as appropriate.

To run the service, type one of the following commands:

* gradle runDev
* gradle runQa
* gradle runProd

And the service will pick up the appropriate config at runtime.  You can also use IntelliJ IDEA's Gradle plugin to run/debug those tasks.
