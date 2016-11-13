iArch
=====

iArch is an integrated development environment for editing and verifying UML models, programs and archcode, which restricts both models and programs to conform to certain requirements.
iArch is an Eclipse plug-in and developed by the members of Kyushu University Ubayashi lab, Archface project.

For detailed information, please refer to the [Website](http://posl.github.io/iArch/).

How to build
------
### Git clone ###
1. Clone this project.
2. Select `File` -> `Import` -> `General` -> `Existing Projects into Workspace`, and select the directory which contains the cloned files.
3. Select all Eclipse projects, and select `Next`, then you will see 9 iArch projects and several example projects which exhibit how to use iArch.

### Generate Xtext code ###
1. Open a file: `jp.ac.kyushu.iarch.archdsl/src/jp.ac.kyushu.iarch.archdsl/ArchDSL.xtext`.
2. Select `Run` -> `Generate Language Infrastructure`.

How to run
------
Select `Run` -> `Launch Runtime Eclipse`.

Included middleware and libraries
------
* Java SE 7~
* Eclipse Mars.2~
* Graphiti SDK Plus 0.12.2~
* Xtext Complete SDK 2.9.1~
* Libraries included in `jp.ac.kyushu.iarch.basefunction/lib`.  
You can also check the licenses in `jp.ac.kyushu.iarch.basefunction/lib/licenses`
    * [dom4j-1.6.1](http://dom4j.sourceforge.net/dom4j-1.6.1/index.html) | [Apache-style open source license](http://dom4j.sourceforge.net/dom4j-1.6.1/faq.html)
    * [jaxen-1.1.6](http://jaxen.org/) | [Apache-style open source license](http://jaxen.org/faq.html)
    * [logback-1.0.13](http://logback.qos.ch/) | [EPL v1.0 or LPGL 2.1](http://logback.qos.ch/license.html)
    * [jgit-4.4.1.201607150455](https://eclipse.org/jgit/) | [Eclipse Distribution License v 1.0](http://www.eclipse.org/org/documents/edl-v10.php)
    * [slf4j-api-1.7.9](http://www.slf4j.org/) | [MIT License](http://www.slf4j.org/license.html)
    * [freemarker-2.3.25](http://freemarker.org/) | [Apache License Version 2.0](http://freemarker.org/docs/app_license.html)

License
------
Eclipse Public License - Version 1.0

