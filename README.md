iArch
=====

Kyushu University Ubayashi lab, Archface project

How to install
------
### Git clone ###
1. Clone this project.
2. Select `File` -> `Import` -> `General` -> `Existing Projects into Workspace`, and select cloned projects.
3. Select all projects, and select `Next`, you will find 9 projects and 1 sample project for using iArch.

### Generate xtext code ###
Use eclipse and open file : 
 `jp.ac.kyushu.iarch.archdsl/src/jp.ac.kyushu.iarch.archdsl/ArchDSL.xtext`
 
run->Generate Language Infrastructure

How to run iArch
------
(In eclipse)
 
run->Launch Runtime Eclipse


Required libraries
------
* Java SDK 1.7~
* Graphiti SDK Plus(Incubation) 0.10.2<-Don't update Graphiti!
* Xtext SDK 2.6.0
* Libraries included in `jp.ac.kyushu.iarch.basefunction/lib`
    * [dom4j-1.6.1](http://dom4j.sourceforge.net/dom4j-1.6.1/index.html) | [(Apache-style open source license)](http://dom4j.sourceforge.net/dom4j-1.6.1/faq.html)
    * [jaxen-1.1.6](http://jaxen.org/) | [(Apache-style open source license)](http://jaxen.org/faq.html)
    * [logback-1.0.13](http://logback.qos.ch/) | [(EPL v1.0 or LPGL 2.1)](http://logback.qos.ch/license.html)
    * [jgit-4.4.1.201607150455](https://eclipse.org/jgit/) | [(Eclipse Distribution License v1.0)](http://www.eclipse.org/org/documents/edl-v10.php)
    * [slf4j-api-1.7.9](http://www.slf4j.org/) | [(MIT License)](http://www.slf4j.org/license.html)
    * freemarker
