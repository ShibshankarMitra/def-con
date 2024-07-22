# Project Title

### enterprise-labor-management-demo

## Description

A home-grown LMS solution across the entire DC enterprise

## Getting Started

### Dependencies

* JDK-17 is the minimum required SDk for this project
* As this is a gradle project, a subsequent version gradle wrapper will be automatically installed.

### Local Project Configuration

* Clone this repository at any preferred location in file system. Open project with any preferred IDE of your choice.
* Make sure that gradle.properties is updated with valid artifactory_user and artifactory_password. Otherwise, gradle install /build will fail.
* Install and configure SonarLint in your IDE. Follow [guidelines](https://thd.atlassian.net/wiki/spaces/SCD/pages/2410224308/SonarCube+Integration+in+IntelliJ+Idea+using+SonarLint).

### Environment set up

* As this is a Spring-Cloud-GCP Project, we need to set up cloud authentication from local using ADC (Application default credentials). Follow [guidelines](https://cloud.google.com/docs/authentication/provide-credentials-adc#google-idp).
* Replace GKE-specific "GKE_STDOUT" appender with "CONSOLE" appender in src/main/resources/logback-spring.xml.
``` 
<appender-ref ref="CONSOLE"/>
```


### Executing program

* Once the Gradle project import is successful, a new SpringBoot run configuration will be generated, which is executable.

## Design

[ELM-discovery-design-confluence](https://thd.atlassian.net/wiki/spaces/SCD/pages/2319650235/Discovery+Design+misc.)

## Authors

Contributors

* [SHIBSANKAR_MITRA@homedepot.com](https://github.com/K57888B_thdgit)
* [JYOTHI_KOTHAPALLI@homedepot.com](https://github.com/KSE8ZDC_thdgit)
* [HARSHIT_MALVIYA1@HOMEDEPOT.COM](https://github.com/KXK8DHW_thdgit)

## Reviewers

* [ARYA_ASOK_KUMAR_SOBHA1@homedepot.com](https://github.com/ZJC17Q5_thdgit)
* [NITIN_KUMAR@homedepot.com](https://github.com/NXK8080_thdgit)


## Version History

* 0.0.1-SNAPSHOT
    * Initial Snapshot Release

## Acknowledgments

Inspiration, code snippets, technical guidelines.
* [spring-cloud-gcp](https://spring.io/projects/spring-cloud-gcp/)
* [Gcp with java getting started content](https://cloud.google.com/java/getting-started)
* [Pub-Sub with SpringBoot](https://cloud.google.com/pubsub/docs/publish-receive-messages-client-library#pubsub-client-libraries-java)
* [BigQuery with SpringBoot](https://cloud.google.com/bigquery/docs/reference/libraries)
