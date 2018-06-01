---
layout: default
title: iArch-U
permalink: /
---

# What is iArch-U?

Uncertainty can appear in all aspects of software development: uncertainty in requirements analysis, design decisions, implementation and testing.
As the research on uncertainty is so young, there are many issues to be tackled.
Modularity for uncertainty is one of them.
If uncertainty can be dealt with modularly, we can add or delete uncertain concerns to/from models, code and tests whenever these concerns arise or are fixed to certain concerns.

The _iArch-U_ Integrated Development Environment (IDE) has been developed for that purpose.<!-- (double blind test)
 by the members of [Principles of Software Languages (POSL) research group](http://posl.ait.kyushu-u.ac.jp/index.html)
-->
iArch-U supports development with uncertainty management using interface component called _Archface-U_.
Archface-U is a new interface designed to enable continuous development with ensuring traceability between software design and implementation.
It is an interface between UML models and Java code described on the iArch-U IDE.

iArch-U also has modelling and coding features which facilitate design and implementation involving uncertainty.
iArch-U is an [Eclipse](https://eclipse.org/) plug-in, so you can view and edit project assets within an environment tightly integrated with Eclipse.
It also has testing support and model inspection features for testing the software with uncertainty.

For detailed information, please refer to the [documentation](documentation/). For basic usage of iArch-U, please see the [tutorials](tutorials/).

<div markdown="0" class="tutorial-video">
  <div markdown="0" class="video-wrapper tutorial-video-wrapper">
    <iframe src="https://www.youtube.com/embed/uM6H-vkddNg?rel=0" frameborder="0" allowfullscreen></iframe>
  </div>
</div>

# System requirements

- Java 8~
- Eclipse Neon~
- Graphiti SDK Plus 0.13~
- Xtext Complete SDK 2.10.0~

Also, if you would like to use Testing Support function, please install below.
- AspectJ Development Tools 4.6~ (Development builds)

These requirements are not strict.
You might be able to run iArch-U with older versions of middleware and/or libraries.


# Installation

1. [Download](https://github.com/posl/iArch/releases) the iArch-U package archive.
1. Expand the archive and copy expanded `plugins` directory into Eclipse plugin directory.
  - We recommend that you use not installer but archive(zip or tar) in Eclipse installation process. If you did so, you can find `plugins` directory in the following steps.
    - The location where `eclipse.exe` exists (Windows)
    - Right click `Eclipse.app`, choice "Show Package Contents", and move into `Contants/Eclipse/` (OSX)
1. Start Eclipse.
1. Select `Help` -> `Install New Software`
  - Install _Graphiti SDK Plus_ and _Xtext Complete SDK_ plug-ins from the Eclipse official site.
  - If you use Testing Support function, also install development builds of _AspectJ Development Tools_ from the update URL shown [here](https://eclipse.org/ajdt/downloads/).
1. Restart Eclipse.


<!-- (double blind test)
# Contact

If you have any questions or comments, please email us: [iarch@posl.ait.kyushu-u.ac.jp](mailto:iarch@posl.ait.kyushu-u.ac.jp)
-->

# License

iArch-U is distributed under EPL - [Eclipse Public License v 1.0](https://eclipse.org/org/documents/epl-v10.php)

<!-- (double blind test)
# Acknowledgements

This research is being conducted as a part of the Grant-in-aid for Scientific Research (A) 26240007 by the Ministry of Education, Culture, Sports, Science and Technology, Japan.
-->
