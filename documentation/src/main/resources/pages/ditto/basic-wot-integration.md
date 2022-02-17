---
title: WoT (Web of Things) integration
keywords: WoT, TD, TM, ThingDescription, ThingModel, W3C, Semantic, Model, definition, ThingDefinition, FeatureDefinition
tags: [wot]
permalink: basic-wot-integration.html
---

Eclipse Ditto added support for **optional** WoT (Web of Things) integration in Ditto version `2.4.0`.  
The integration is based on the
[Web of Things (WoT) Thing Description 1.1 - W3C Working Draft February/March 2022](https://www.w3.org/TR/wot-thing-description11/).

{% include warning.html content="As WoT Thing Description version 1.1 was not yet published as \"W3C Recommendation\", 
    when Ditto `2.4.0` was released, the WoT integration is currently marked as **experimental**.<br/>This means that 
    aspects of the implementation, the behavior and the **ditto-wot-module** (Java module) are subject to change
    without the guarantee of being backwards compatible." %}

Because the integration is experimental, it must explicitly be activated via a "feature toggle":  
In order to activate the WoT integration, configure the following environment variable for all Ditto services:

```bash
DITTO_DEVOPS_FEATURE_WOT_INTEGRATION_ENABLED=true
```


## Web of Things

> "The Web of Things seeks to counter the fragmentation of the IoT through standard 
> complementing building blocks (e.g., metadata and APIs) that enable easy integration across IoT platforms and 
> application domains."

[Source](https://www.w3.org/groups/wg/wot)

The W3C WoT (Web of Things) working group provides "building blocks" under the roof of the 
"main international standards organization for the World Wide Web" to 
"\[...\] simplify IoT application development by following the well-known and successful Web paradigm.".

Source: [Web of Things in a Nutshell](https://www.w3.org/WoT/documentation/)

At its core, the specification of the so-called "WoT Thing Description" (TD) defines IoT device's metadata and 
interaction capabilities.  
The idea of such a TD (Thing Description) is that a device (or e.g. a digital twin service acting as intermediate) 
describes in a standardized way which capabilities in form of `properties`, `actions` and `events` a thing provides 
and which input/output can be expected when interacting with the thing.  

Even more, a TD contains so called `forms` for the mentioned interaction capabilities which map those rather abstract
concepts to actual endpoints, e.g. to HTTP endpoints, HTTP verbs and HTTP headers, etc.  
Developer under the roof of the W3C, web APIs are obviously well understood and are incorporated perfectly in the 
"WoT Thing Description" specification.  
But also other protocol bindings may be defined in a `form`, e.g. MQTT or CoAP.

The "WoT Thing Description" specification version 1.0 was already published as 
["W3C Recommendation" in April 2020](https://www.w3.org/TR/wot-thing-description/), the next version 1.1 e.g. adds the 
new concept of "Thing Models" (TM) which can be seen as a template for generating "Thing Descriptions", 
leaving out some mandatory fields in "Thing Descriptions" like for example the `forms` including the protocol bindings.

With this addition of the "Thing Model" concept, the WoT becomes a perfect fit for describing the capabilities of 
[Digital Twins](intro-digitaltwins.html) managed in Ditto, completely optional and even possible as "retrofit" model 
addition for already connected devices / already existing twins.

The benefits of adding such a "Thing Model" reference to digital twins managed in Ditto are:
* possibility to define model for data (Ditto Thing `attributes` + Ditto Feature `properties`), e.g. containing:
    * data type to expect
    * restrictions which apply (like e.g. possible min/max values)
    * default values to assume if data is not available
    * units of data entries
* possibility to define model for messages
    * same as above for data
    * in addition, describing possible input/output and also possible error situations when invoking a message
* capability to provide semantic context (using JSON-LD), e.g. by referencing to existing ontologies like:
  * saref: [https://ontology.tno.nl/saref/](https://ontology.tno.nl/saref/)
  * OM-2 "Ontology of units of measure": [http://www.ontology-of-units-of-measure.org/page/om-2](http://www.ontology-of-units-of-measure.org/page/om-2)
  * or any other JSON-LD described ontology
* improves interoperability
    * different IoT systems and devices may engage with each other in a standardized way
    * models and included data formats are not proprietary, but are defined in an open standard
    * avoids vendor lock-in, even a lock-in to Ditto
* enables introspection of digital twins
    * Ditto managed twins can describe themselves
    * if backed by a WoT Thing Model, the twin will tell you as response exactly 
        * what it is capable of
        * which HTTP endpoints to invoke to access data / send messages
* take advantage of an open standard
    * W3C are experts on the web, the WoT standard relies on already established other standards like e.g. HTTP, JSON, 
      JSON-LD, JsonSchema, JsonPointer, etc. 
    * the standard is in active development
    * many eyes (e.g. well known industry players like Siemens, Intel, Oracle, Fujitsu, ...) review additions, etc.
    * W3C perform a completely open specification process, no decision is made secretly, new versions evolve over years
* use the tooling landscape / open ecosystem evolving around the WoT standard, e.g.:
    * the [Eclipse edi{TD}or](https://eclipse.github.io/editdor/) online-editor for WoT TDs and TMs
    * the [Eclipse thingweb.node-wot](https://github.com/eclipse/thingweb.node-wot) project providing 
      [node.wot NPM modules](https://www.npmjs.com/org/node-wot), e.g. in order to "consume" Thing Descriptions in 
      Javascript
    * a [node-red library for WoT integration](https://flows.nodered.org/node/node-red-contrib-web-of-things)


### WoT Thing Description

A [Thing Description](https://www.w3.org/TR/wot-thing-description11/#introduction-td) describes exactly one instance of 
a device/thing.  
This description contains not only the interaction capabilities (`properties` the devices provide, 
`actions` which can be invoked by the devices and `events` the devices emit), but also contain concrete API endpoints 
(via the `forms`) to invoke in order to actually interact with the devices.

### WoT Thing Model

A [Thing Model](https://www.w3.org/TR/wot-thing-description11/#introduction-tm) can be seen as the model 
(or interface in OOP terminology) for a potentially huge population of instances (Thing Descriptions) all "implementing"
this contract.  
It does not need to contain the instance specific parts which a TD must include (e.g. `security` definitions or `forms`),
it focuses on the possible interactions (`properties`, `actions`, `events`) and their data types / semantics in a more 
abstract way.

#### Thing Model modeling good practices

When writing new WoT Thing Models to describe the capabilities of your devices, here are some good practices and tips
which you should consider:

* put a version in your Thing Model filename:
    * The WoT specification does not require you to put the version of the Thing Model in the filename, however you should
      really do so.
    * If you don't and e.g. just provide the file as `lamp.tm.jsonld`, you will probably just overwrite the file whenever
      you do a change to the model.
    * Think about an application consuming that model - on the one day a Thing would be correctly defined by the model, on
      the next day the Thing would no longer follow the model, even if the URL to the model was not changed. 
* apply semantic versioning:
    * if you only to a "bugfix" to a model, increase the "micro" version: `1.0.X`
    * if you add something new to a model without removing/renaming (breaking) existing definitions, increase the "minor" version: `1.X.0`
    * if you need to break a model (e.g. by removing/renaming something or changing a datatype), increase the "major" version: `X.0.0`
* treat published Thing Models as "immutable":
    * never change a "released" TM once published and accessible via public HTTP endpoint
* provide a `title` and a `description` for your Thing Models
    * you as the model creator can add the most relevant human-readable descriptions
    * also, for `properties`, `actions`, `events` and all defined data types
    * if you need internationalization, add those to `tiles` and `descriptions` 
* provide a semantic context by referencing ontologies in your JSON-LD `@context`
    * at some point, a machine learning or reasoning engine will try to make sense of your things and their data
    * support the machines to be able to understand your things semantics
    * find out more about Linked Data and JSON-LD here: [https://json-ld.org](https://json-ld.org)
    * e.g. make use of [public ontologies](#public-available-ontologies-to-reference)
* use the linked ontologies in order to describe your model in a semantic way, e.g. in the `properties` of a 
  "Temperature sensor" model 
  (see also the [example in the TD specification](https://www.w3.org/TR/wot-thing-description11/#semantic-annotations-example-version-units)):
    ```json
    {
      "properties": {
        "currentTemperature": {
          "@type": "om2:CelsiusTemperature",
          "title": "Current temperature",
          "description": "The last or current measured temperature in '°C'.",
          "type": "number",
          "unit": "om2:degreeCelsius",
          "minimum": -273.15
        }
      }
    }
    ```

#### Public available ontologies to reference

Here collected are some ontologies which you can use in order to provide semantic context:

To describe time related data (e.g. durations, timestamps), you can use: [W3C Time Ontology](https://www.w3.org/TR/owl-time/)  
```json
{
  "@context": {
    "https://www.w3.org/2022/wot/td/v1.1",
    {
      "time": "http://www.w3.org/2006/time#"
    }
  }
}
```

To describe geolocations, you can use: [W3C Basic Geo (WGS84 lat/long) Vocabulary](https://www.w3.org/2003/01/geo/):  
```json
{
  "@context": {
    "https://www.w3.org/2022/wot/td/v1.1",
    {
      "geo": "http://www.w3.org/2003/01/geo/wgs84_pos#"
    }
  }
}
```

To describe units you can choose between:
* [QUDT.org](http://www.qudt.org)  
  ```json
  {
    "@context": {
      "https://www.w3.org/2022/wot/td/v1.1",
      {
        "qudt": "http://qudt.org/schema/qudt/",
        "unit": "http://qudt.org/vocab/unit/",
        "quantitykind": "http://qudt.org/vocab/quantitykind/"
      }
    }
  }
  ```
* [OM 2: Units of Measure](http://www.ontology-of-units-of-measure.org/page/om-2):  
  ```json
  {
    "@context": {
      "https://www.w3.org/2022/wot/td/v1.1",
      {
        "om2": "http://www.ontology-of-units-of-measure.org/resource/om-2/"
      }
    }
  }
  ```

To describe "assets", you can use: [SAREF](https://ontology.tno.nl/saref/)  
```json
{
  "@context": {
    "https://www.w3.org/2022/wot/td/v1.1",
    {
      "saref": "https://w3id.org/saref#"
    }
  }
}
```


## Mapping of WoT concepts to Ditto

Mapping a WoT Thing Description (TD) to a Ditto [Thing](basic-thing.html) can be done in different "complexity levels":
1. the most simple mapping is that a WoT TD describes exactly one Ditto Thing
2. another possible mapping is that a WoT TD describes exactly one Ditto [Feature](basic-feature.html) (being part of a Thing)
3. the most advanced option is that:
    * a WoT TD describes a Ditto Thing
    * and in addition contains "sub things" as Features of the Thing, all described by their own TD

The third option adds the possibility to provide common aspects which many similar devices support as part of the similar 
devices, all implementing the same "contract" (Thing Model) with the same interaction capabilities 
(`properties`, `actions` and `events`), the same data formats and semantic context.

### Thing Description vs. Thing Model

While WoT TD describe the instance (the Ditto Thing), a WoT Thing Model (TM) provides the model of a Thing and can be 
referenced by the Thing by inserting its HTTP endpoint in the [Thing Definition](basic-thing.html#definition).  
Ditto supports adding a valid HTTP(s) URL in the `"definition"`.

The same applies for the [Feature Definition](basic-feature.html#feature-definition) which may also contain the HTTP
endpoints to a valid WoT Thing Model.

Thing Descriptions are generated by the WoT integration in Ditto, based on the Thing Models referenced in a 
[Thing Definition](basic-thing.html#definition) and in [Feature Definitions](basic-feature.html#feature-definition).

### Thing Model describing a Ditto Thing

This table shows an overview of how those elements map to Ditto concepts for the "Thing" level:

| WoT element                                                                                             | Ditto concept                                                                                                      |
|---------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| [Thing](https://www.w3.org/TR/wot-thing-description11/#thing)                                           | [Ditto Thing](basic-thing.html)                                                                                    |
| [Properties](https://www.w3.org/TR/wot-thing-description11/#propertyaffordance)                         | Thing [attributes](basic-thing.html#attributes)                                                                    |
| [Actions](https://www.w3.org/TR/wot-thing-description11/#actionaffordance)                              | Thing [messages](basic-messages.html#elements) with **Direction** *to* (messages in the "inbox") of a Thing ID.    |
| [Events](https://www.w3.org/TR/wot-thing-description11/#eventaffordance)                                | Thing [messages](basic-messages.html#elements) with **Direction** *from* (messages in the "outbox") of a Thing ID. |
| [Composition via `tm:submodel`](https://www.w3.org/TR/wot-thing-description11/#thing-model-composition) | Thing [features](basic-thing.html#features) representing different aspects of a Ditto Thing.                       |


### Thing Model describing a Ditto Feature

This table shows an overview of how those elements map to Ditto concepts for the "Feature" level:

| WoT element                                                                     | Ditto concept                                                                                                                                                                     |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Thing](https://www.w3.org/TR/wot-thing-description11/#thing)                   | Feature.<br/>In Ditto, a Feature is an aspect of a [Ditto Thing](basic-thing.html). As the Feature is defined by its properties and messages it supports, it maps to a WoT Thing. |
| [Properties](https://www.w3.org/TR/wot-thing-description11/#propertyaffordance) | Feature [properties](basic-feature.html#feature-properties)                                                                                                                       |
| [Actions](https://www.w3.org/TR/wot-thing-description11/#actionaffordance)      | Feature [messages](basic-messages.html#elements) with **Direction** *to* (messages in the "inbox") of a Thing ID + Feature ID combination.                                        |
| [Events](https://www.w3.org/TR/wot-thing-description11/#eventaffordance)        | Feature [messages](basic-messages.html#elements) with **Direction** *from* (messages in the "outbox") of a Thing ID + Feature ID combination.                                     |


## Integration in Ditto

The WoT integration in Ditto covers several aspects:
* referencing HTTP(s) URLs to WoT Thing Models in [Thing Definitions](basic-thing.html#definition) and in [Feature Definitions](basic-feature.html#feature-definition)
* generation of WoT Thing Descriptions for Thing and Feature instances based on referenced Thing Models
    * resolving potential [extensions via `tm:extends` and imports via `tm:ref`](https://www.w3.org/TR/wot-thing-description11/#thing-model-extension-import)
    * resolving potential Thing level [compositions via `tm:submodel`](https://www.w3.org/TR/wot-thing-description11/#thing-model-composition)
    * resolving potential [TM placeholders](https://www.w3.org/TR/wot-thing-description11/#thing-model-td-placeholder)
* upon creation of new Things, generation of a "JSON skeleton" following the WoT Thing Model, including referenced TM submodels as Features of the Thing 

### Thing Description generation

WoT Thing Models are intended to be used as templates for generating (instance specific) Thing Descriptions, 
the rules for doing that are specified: 
[Derivation of Thing Description Instances](https://www.w3.org/TR/wot-thing-description11/#thing-model-td-generation)

Prerequisites to use the Thing Description generation:
* the feature toggle (environment variable `DITTO_DEVOPS_FEATURE_WOT_INTEGRATION_ENABLED=true`) is activated
* HTTP content negotiation is applied, setting the `Accept` header to the registered WoT TD
  [IANA content type `application/td+json`](https://www.iana.org/assignments/media-types/application/td+json)
  when retrieving the Thing e.g. via an HTTP `GET /api/2/<namespace>:<thing-name>`

The available configuration of the WoT integration can be found in the 
[things.conf](https://github.com/eclipse/ditto/blob/master/things/service/src/main/resources/things.conf)
config file of the [things](architecture-services-things.html) service at path `ditto.things.wot`.  
There you can e.g. configure which `securityDefinitions` shall be added to the generated TDs and which `base` path 
prefix to create into the TDs, depending on your public Ditto endpoint.

#### Security: TD access / authorization

For accessing Thing Descriptions created for Things and Features no special permission in the Thing's 
[Policy](basic-policy.html) is required.

As the Thing Model must be a publicly available resource and the Thing ID is also known for a user requesting the TD of 
a Thing, there is no additional information to disclose.

Accessing the `properties`, invoking `actions` and subscribing for `events` is of course authorized by the Thing's 
Policy via its [Thing](basic-policy.html#thing), [Feature](basic-policy.html#feature) and 
[Message](basic-policy.html#message) resources.

#### TD generation for Things

Additional prerequisites that a WoT TD is generated for a Ditto Thing:
* the Thing references a valid WoT Thing Model in its [Thing Definition](basic-thing.html#definition) and this TM is
  publicly downloadable via its HTTP(s) URL

Function:
* Ditto checks if the Thing Definition contains a valid HTTP(s) URL
* Ditto downloads the referenced URL and checks if this is a valid WoT Thing Model
* Ditto saves the downloaded TM to a local cache
* Ditto generates a WoT Thing Description and returns it as JSON response
    * defined TM `tm:extends` extensions are resolved by downloading those TMs as well 
    * defined TM `tm:refs` imports are also resolved by downloading those TMs as well 
    * defined TM `tm:submodel`s are added to the `links` of the TD pointing to the TDs of the Features of the Thing
    * metadata available in the Thing or in the Ditto configuration is also included in the generated TD

Using cURL, the Thing Description for a Ditto Thing can be generated and fetched by invoking:

```bash
curl -u ditto:ditto 'http://localhost:8080/api/2/things/io.eclipserpojects.ditto:my-thing' \
  --header 'Accept: application/td+json'
```

#### TD generation for Features

Additional prerequisites that a WoT TD is generated for a Ditto Feature:
* the Feature references at least one valid WoT Thing Model in its
  [Feature Definition](basic-feature.html#feature-definition) and this TM is publicly downloadable via its HTTP(s) URL

Function:
* Ditto checks if the first Feature Definition in the list of definitions contains a valid HTTP(s) URL
    * additional Feature Definitions in the array are interpreted as extended TMs in order to specify the "extension hierarchy"
* Ditto downloads the referenced URL and checks if this is a valid WoT Thing Model
* Ditto saves the downloaded TM to a local cache
* Ditto generates a WoT Thing Description and returns it as JSON response
    * defined TM `tm:extends` extensions are resolved by downloading those TMs as well
    * defined TM `tm:refs` imports are also resolved by downloading those TMs as well
    * metadata available in the Thing or in the Ditto configuration is also included in the generated TD

Using cURL, the Thing Description for a Ditto Feature can be generated and fetched by invoking:

```bash
curl -u ditto:ditto 'http://localhost:8080/api/2/things/io.eclipserpojects.ditto:my-thing/features/my-feature-1' \
  --header 'Accept: application/td+json'
```

#### Resolving Thing Model placeholders

WoT Thing Models may contain [placeholders](https://www.w3.org/TR/wot-thing-description11/#thing-model-td-placeholder)
which **must** be resolved during generation of the TD from a TM.  

In order to resolve TM placeholders, Ditto applies the following strategy:
* when generating a TD for a Thing, it looks in the Things' attribute `"model-placeholders"` (being a JsonObject) in
   order to lookup placeholders
* when generating a TD for a Feature, it looks in the Feature's property `"model-placeholders"` (being a JsonObject) in
   order to lookup placeholders
* when a placeholder was not found in the `"model-placeholders"` of the Thing/Feature, a fallback to the Ditto 
  configuration is done:
    * placeholder fallbacks can be configured in Ditto via the 
      [things.conf](https://github.com/eclipse/ditto/blob/master/things/service/src/main/resources/things.conf) 
      configuration file of the [things](architecture-services-things.html) service at path 
      `ditto.things.wot.to-thing-description.placeholders`.<br/>  
      This map may contain static values, but use and Json type as value (e.g. also a JsonObject), e.g.:
      ```
      FOO = "bar"
      TM_REQUIRED = [
        "#/properties/status",
        "#/actions/toggle"
      ]
      ```

{% include warning.html content="Please be aware that placeholders put into the `\"model-placeholders\"` attribute/property 
    of a Thing/Feature may be used in TM placeholders and therefore are not 
    protected by any authorization check based on the Thing's [Policy](basic-policy.html) as TDs are available for all
    authenticated users which know the Thing ID."
%}

### Thing skeleton generation upon Thing creation

Prerequisites to use the skeleton generation during Thing creation:
* the feature toggle (environment variable `DITTO_DEVOPS_FEATURE_WOT_INTEGRATION_ENABLED=true`) is activated
* the created Thing references a valid WoT Thing Model in its [Thing Definition](basic-thing.html#definition) and this 
  TM is publicly downloadable via its HTTP(s) URL

Function:
* Ditto checks if the Thing Definition contains a valid HTTP(s) URL
* Ditto downloads the referenced URL and checks if this is a valid WoT Thing Model
* Ditto saves the downloaded TM to a local cache
* Ditto uses the Thing Model as template for generating JSON elements:
    * attributes based on the contained TM `properties`
    * features based on the contained TM `tm:submodel`s using the `instanceName` as Feature ID
        * properties based on the submodel `properties`
* when a `default` is specified for the TM property, this default is used as value, otherwise the "neutral element" of 
  the datatype is used as initial value of the property
* if any error happens during the skeleton creation (e.g. a Thing Model can't be downloaded or is invalid),
  the Thing is created without the skeleton model, just containing the specified `"definition"`


## Example

The example can be found on a [dedicated page](basic-wot-integration-example.html) as the JSONs included in the 
example are quite long.
