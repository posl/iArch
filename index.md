---
layout: default_index
title: iArch-U
permalink: /
---

# What is iArch-U?

Uncertainty can appear in all aspects of software development: uncertainty in requirements analysis, design decisions, implementation and testing.
As the research on uncertainty is so young, there are many issues to be tackled.
Modularity for uncertainty is one of them.
If uncertainty can be dealt with modularly, we can add or delete uncertain concerns to/from models, code and tests whenever these concerns arise or are fixed to certain concerns.

The _iArch-U_ Integrated Development Environment (IDE) has been developed for that purpose by the members of [Principles of Software Languages (POSL) research group](http://posl.ait.kyushu-u.ac.jp/index.html).
iArch-U supports development with uncertainty management using interface component called _Archface-U_.
Archface-U is a new interface designed to enable continuous development with ensuring traceability between software design and implementation.
It is an interface between UML models and Java code described on the iArch-U IDE.

iArch-U also has modelling and coding features which facilitate design and implementation involving uncertainty.
iArch-U is an [Eclipse](https://eclipse.org/) plug-in, so you can view and edit project assets within an environment tightly integrated with Eclipse.
It also has testing support and model inspection features for testing the software with uncertainty.

# System requirements

- Java SE 7~
- Eclipse Mars.2~
- Graphiti SDK Plus 0.12.2~
- Xtext Complete SDK 2.9.1~

These requirements are not strict.
You might be able to run iArch-U with older versions of middleware and/or libraries.


# Installation

1. Download the iArch-U package archive.
1. Expand the archive and copy expanded `plugins` directory into your Eclipse application directory.
1. Start Eclipse.
1. Select `Help` -> `Install New Software`
  - Install _Graphiti SDK Plus_ and _Xtext Complete SDK_ plug-ins from the Eclipse official site.
1. Restart Eclipse.


# Contact

If you have any questions or comments, please email us: [iarch@posl.ait.kyushu-u.ac.jp](mailto:iarch@posl.ait.kyushu-u.ac.jp)


# License

iArch-U is distributed under EPL - [Eclipse Public License v 1.0](https://eclipse.org/org/documents/epl-v10.php)

EPL is compatible with the MIT license.


# Acknowledgements

This research is being conducted as a part of the Grant-in-aid for Scientific Research (A) 26240007 by the Ministry of Education, Culture, Sports, Science and Technology, Japan.
