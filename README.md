Introduction
============

This project implements a small rest service which handles a request to generate a zip file containing a customized Jamba/VST blank plugin.

This project started as a Spring boot application using kotlin and was ported to [ktor][https://ktor.io/] so that both the backend (rest service) and the frontend (html + javascript) is 100% written in kotlin.

This project is currently not deployed as I believe there is a way to implement it as a pure javascript frontend (no backend) and this is what will be deployed when completed.

I decided to release/open source this internal project as it serves as a more complete demonstration than a basic "Hello World" application.

This project was built with kotlin 1.3.31

List of demonstrated features (backend)
---------------------------------------

* a multiplatform (jvm + javascript) kotlin project (although "experimental" at this stage, it works perfectly)
* a rest service with multiple apis
* handling IOC/dependency injection (built-in with spring boot)
* handling json as response
* handling serving a File as response
* handling of configuration parameters (ex: -P:api.static.web=) including from command line
* handling "Basic Auth" authentication for a part of the api (/admin)
* handling shutdown (buitlt-in/easier with spring boot) to properly stop the server and destroy beans when the jvm terminates (via signal)
* writing test for the rest api (withTestApplication)
* generating dynamic html from kotlin (via kotlinx.html)
* including generated javascript to be served
* serving static files (js/css)
* generating an uber jar (built-in with spring boot) which is a single jar to run the app

List of demonstrated features (frontend)
----------------------------------------

* writing the javascript code in kotlin
* adding an event listener ("change", "click")
* using "dynamic" to add a field to a javascript object (see __computedValue)
* posting a form via the "fetch" api and extracting/processing the json response (including error handling)
* posting a request via the "fetch" api and extracting/processing the json response (including error handling)
* adding dom elements (the example is small so it is not using kotlinx.html but it could)
* downloading a file via javascript

License
=======

Apache 2.0

