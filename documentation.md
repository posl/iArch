---
layout: default
title: Documentation
permalink: /documentation/
priority: 20
---

<h1 markdown="0" id="page-title">Documentation</h1>

This is the documentation for iArch 1.1.

- TOC
{:toc}

# iArch-U

## OverView

![OverView](../images/iArch_features.svg)

_iArch-U_ is the IDE, which supports the "uncertainty-aware software development".
iArch-U has the interface component _Archface-U_ as the central mechanism to support uncertainty-aware software development.
The word "Archface-U" is coined from "Architecture", "Interface", and "Uncertainty".

The common restrictions for the entire development process are expressed in Archface-U, especially at design and implementation.
iArch-U continuously checks UML model against Archface-U to keep consistency, and also checks code against it.
That enables consistent development throughout design and implementation phase.

Archface-U also contains description of uncertainty occurred in development process.
In the context of Archface-U, "uncertainty" is an unfixed design or an unfixed way to implementation.
Thus the following situations should be described in Archface-U, for example.

* Unfixed specifications which are caused by ambiguous requirements
* Unfixed algorithms to realize a functionality
* Temporary alternative implementations you cannot judge which is the best yet

Describing uncertainty in Archface-U is the sufficient way to manage uncertainty.
Additionally, iArch-U assures consistency between model, code, and Archface-U, considering uncertainty.
That enables you to continue the development process while involving uncertainty.
Here we provide iArch-U as the sufficient solution for the "uncertainty-aware software development".

We broadly classify features of iArch-U like the following.

* Type checking for model
* Type checking for source code
* Testing support based on Archface-U
* Uncertainty management support, that works with Git VCS

## GUI

![ScreenShot](../images/ide_overview_2x.png)

iArch is distributed as an Eclipse plugin. That provides graphical views to manage uncertainty.
Detailed information is in this documentation.


## Terminology

- _Archface_ is a Domain Specific Language (DSL) to bridge the gap between UML models and Java code.
- _Archcode_ is code written in Archface.
- _Archfile_ is a file storing Archcode.
- _Archface-U_ is an extended version of Archface being able to express uncertainty.
- _iArch_ is an Integrated Development Environment (IDE) to handle Archface.
- _iArch-U_ is an extended version of iArch supporting Archface-U.


# Archface-U

Archface-U is a new interface designed to enable continuous development with ensuring traceability between software design and implementation.
It is an interface between UML models and Java code described on the iArch-U IDE.
Traceability is maintained by the type checking process so that the UML models and Java code are designed and implemented to conform to Archface.

Archface-U is based on component-and-connector architecture and consists of two kinds of interface: _component_ and _connector_.
The former exposes a set of architectural points that should be shared among requirements models, design models and programs, and the latter defines how to coordinate them.


## Syntax

Archface-U syntax to define a _component_ is roughly the same as the Java syntax for interface declaration, except that it needs an additional `component` keyword, and the syntax for _connector_ definition is based on the syntax of [Finite State Process (FSP)](http://www.doc.ic.ac.uk/~jnm/LTSdocumention/FSP-notation.html).

```java
interface component ComponentName {
	ReturnType methodName(ArgumentType argumentName, ...);
	...
}

interface connector ConnectorName {
	ClassName = (ClassName.methodName -> ClassName.methodName -> ...);
	...
}
```

In a _component_ definition, you can list method signatures whose enclosing class should have.
In a _connector_ definition, you can declare ordered sequences of methods that should be called in that order when the program is executed.

When you want to introduce uncertainty into Archface-U definition, it can be expressed by enclosing uncertain parts of the program with brackets, along with replacing `interface` keyword with `uncertain`.
There are two types of uncertainty Archface-U supports:

- _Alternative_: there are some component candidates, but which one will be used is still uncertain.
- _Optional_: uncertainty if a component is ultimately integrated into the system.

```java
uncertain component ComponentName {
	{ ReturnType methodName(ArgumentType argumentName, ...);,
	ReturnType methodName(ArgumentType argumentName, ...);, ... }

	[ ReturnType methodName(ArgumentType argumentName, ...); ]
}

uncertain connector ConnectorName {
	ClassName = ({ ClassName.methodName, ClassName.methodName, ... } -> ...);

	ClassName = ([ ClassName.methodName ] -> ...);
}
```

You can declare _alternative_ by bracketing a method with `{}` (curly brackets) and _optional_ by `[]` (square brackets).


# Features

iArch-U provides various functionalities to support the development process employing the Java language.


## Modelling

In the modelling process using iArch-U, we can express two types of uncertainty, _optional_ and _alternative_, by extended class diagrams and sequence diagrams.
Archface-U compiler also runs type checking on models so that we have to draw class diagrams and sequence diagrams conforming to interfaces defined in Archcode.

In the class diagram, uncertain methods are marked by `{}` or `[]`, for _alternative_ and _optional_ respectively, as the same way as in Archface-U.
In the sequence diagram, uncertainty is expressed by complex fragments labelled with `u-alt` or `u-opt`, for _alternative_ and _optional_ respectively, to distinguish them from normal `alt` and `opt` fragments.


### Creating a new model diagram

At first, select `File` -> `New` -> `Other`.
After the dialogue box is displayed, select `Examples` -> `Graphiti` -> `Graphiti Diagram`, and push `Next` button.

![New diagram 1](../images/new_diagram_01.png)

In the next pane, you will be asked to select a diagram type.
If you want to draw a class diagram, select `ClassDiagram`, or if you want to draw a sequence diagram, select `SequenceDiagram`, and push `Next` button.

![New diagram 2](../images/new_diagram_02.png)

Lastly, enter an appropriate diagram name, and push `Finish` button.
This will create a new model file and it will be opened by the model editor.

![New diagram 3](../images/new_diagram_03.png)


### Common operations of the model editor

You can choose a model element from the right-hand side of the model editor and place it on the grid.

![Class diagram](../images/class_diagram.png)

![Sequence diagram](../images/sequence_diagram.png)

Some elements in `Objects` can be placed only within an already placed element.
Elements belonging to `Connections` can be used to connect elements on the grid by selecting them.
You can change the location or the size of model elements by dragging them, and also you can change the name of model elements by clicking them.

In the `Properties` view, you can see information of the selected element.
The name of some types of elements can be changed from the `Properties` view.

![Properties view](../images/properties_view.png)


### Operations for the class diagram

- `Class` corresponds to class.
- `Operation`, `OptionalOperation` and `AlternativeOperation` each corresponds to _certain_, _optional_ and _alternative_ methods respectively.
  - Enter the name in the dialogue box.
  - In case of _alternative_, list method names with separating spaces.
- `Attribute` and `Association` are not supported by iArch-U.


### Operations for the sequence diagram

- `Actor` corresponds to actor.
- `Object` corresponds to class object.
- `Lifeline` corresponds to lifeline, whose terminal point should be on actor or object.
- `Message`, `OptionalMessage` and `AlternativeMessage` each corresponds to _certain_, _optional_ and _alternative_ messages respectively.
  - Can be created by connecting two lifelines.
  - Enter the name in the dialogue box.
  - In case of _alternative_, list method names with separating spaces.


### Editing uncertainty in the model editor

In the model editor, you can choose one of the following four operations from the context menu where you want to edit uncertainty.

- Setting optional uncertainty
- Setting alternative uncertainty
- Removing uncertainty as it turned to be necessary
- Removing uncertainty as it turned to be unnecessary


#### Setting uncertainty

Setting uncertainty introduces optional or alternative uncertainty into a method declaration in the class diagram or a method call in the sequence diagram.

To set optional uncertainty, select `iArch: Set Optional`.
This operation changes not only the model element you choose but also the corresponding Archcode element.

![Slicing optional 1](../images/slicing_01_optional_menu.png)

![Slicing optional 2](../images/slicing_02_optional_method.png)

![Slicing optional 3](../images/slicing_03_optional_message.png)

To set alternative uncertainty, select `iArch: Set Alternative`.
Enter method names with separating spaces in the dialogue box.
This operation changes not only the model element you choose but also the corresponding Archcode element.

![Slicing alternative 1](../images/slicing_04_alternative_menu.png)

![Slicing alternative 2](../images/slicing_05_alternative_dialog.png)

![Slicing alternative 3](../images/slicing_06_alternative_method.png)

![Slicing alternative 3](../images/slicing_07_alternative_message.png)


#### Removing uncertainty

Removing uncertainty is an operation to get rid of now unnecessary uncertainty.
You can choose either to remove uncertainty because the element in question turned to be necessary, and you want to preserve it, or to remove uncertainty because the element is now clearly unnecessary.

To remove uncertainty when the element turned to be necessary, select `iArch: Remove Uncertainty as Necessary`.
This operation changes not only the model element you choose but also the corresponding Archcode element.

To remove uncertainty when the element turned to be unnecessary, select `iArch: Remove Uncertainty as Unnecessary`.
This operation causes following destructive changes:

- If the target method is optional, it will be deleted.
- If the target method is alternative, it will be removed from the choices.


### Type checking

You can check the consistency between model diagrams and Archcode by selecting `Type Check` from the context menu of the model editor.
If the models do not conform to the restrictions imposed by Archcode, it will be displayed in the `Problems` view.

![Diagram type check](../images/diagram_type_check.png)


## Programming

The Archface-U compiler will compile Archcode at the same time as Java code compilation as well as type checking.
If your implementation violates the interface description, the Archface-U compiler will return compilation error as a result of type checking.


### Editing uncertainty in the program editor

You can manage uncertainty in the program editor, as you can in the model editor.
In the program editor, if the cursor is at a method signature line when you invoke the uncertainty editing operation from the context menu, the corresponding component description in Archcode will be changed accordingly.
On the other hand, if the cursor is at a method call line, the corresponding connector description will be changed.


### Type checking

Type checking is executed every time Java code is compiled.
If the code does not conform to the restrictions imposed by Archcode, it will be displayed as a marker at the left side border of the program editor and in the `Problems` view.

![Code type check](../images/code_type_check.png)


## Archface-U editor

You can edit Archcode in the Archface-U editor, which offers syntax error checking and syntax highlighting features.


## Model inspection and metrics calculation

In `Archface-U View`, you can check whether an element described in Archcode is uncertain and is already implemented, as a result of type checking.
In addition, you can check the metrics about the abstraction (number of design points(DP), program points(PP), and the abstraction ratio) of the project.

![Archface-U view](../images/archface_u_view_2.png)


## Testing support

When you try to test code which has uncertainty, you normally have to resolve uncertainty beforehand, but this involves code change and thus incurs extra cost of implementation.
By using this testing support functionality, you do not need to change code, but the compiler generates testing support code which selectively executes one of possible uncertainty resolved code with certain probability, guided by annotations embedded in code.

You can generate testing support code by selecting `Generate Aspect` from the `iArch` menu of the toolbar.
Removing testing support code is done by selecting `Remove Aspect`.


## Management support

When some uncertainty is resolved during the design or coding process, the trace of this uncertainty only remains in the commit history.
To find the source and temporal transitions of uncertainty from Java code commit history is a daunting task.
iArch-U provides a feature to dig change history of Archcode and to display summary of uncertainty changes.

If the project is managed by git, triggered by the type checking for Java code, commit time and other related information for each uncertainty existing in Archcode are displayed in `Archface-U View`.
Additionally, if you double click an uncertainty element in the view, you can examine detailed history in another window.

![Git log dialog](../images/git_log_dialog.png)


## Model check support

When you need to verify your model, the LTS analyser (http://www.doc.ic.ac.uk/~jnm/LTSdocumention/LTSA.html) will help.
The LTS analyser checks FSP code which represents the model.
You can generate FSP code from an Archface-U file by selecting `Generate FSP for LTS` from the `iArch` menu of the toolbar.
Resulting code is displayed in a dialogue, and is saved in the project folder.

![Generated FSP](../images/generated_fsp_dialog.png)


## Generation/Synchronisation support among Archface-U, model diagrams, and Java code

Every time you add/remove the uncertainty to/from the model, both Archface-U and the model diagrams should be updated simultaneously.
However, sometimes the relationship among them is disturbed.
On the other hand, you want to start managing uncertainty on existing (unmanaged) project, or you wish to avoid writing routine phrases for classes you have modelled already.

iArch gives you the functionalities to generate/synchronise Archface-U,  model diagrams, and Java code, by selecting items within the `Synchronize` from the `iArch` menu of the toolbar.

### Model diagrams to Archface-U

When you select `Model -> iArch`, you can generate Archface-U description corresponding to the model diagrams which you chose in the configuration described below.

This feature is the same as the one which is previously provided as `Generate Archface` in the menu, except that the diagram selection by a dialogue is unnecessary.

For example, from a class diagram and a sequence diagram,

![model to Archface-U class diagram](../images/sync_model_arch_1.png)
![model to Archface-U sequence diagram](../images/sync_model_arch_2.png)

you can get the Archface-U like:

![model to Archface-U result](../images/sync_model_arch_3.png)


### Archface-U to model diagrams

When you select `iArch -> Model`, you can generate model diagrams corresponding the Archcode which you chose in the configuration described below.

For example, from an Archface-U,

![Archface-U to model archfile](../images/sync_arch_model_1.png)

you can get class and sequence diagrams like:

![Archface-U to model result 1](../images/sync_arch_model_2.png)
![Archface-U to model result 2](../images/sync_arch_model_3.png)
![Archface-U to model result 3](../images/sync_arch_model_4.png)


### Java code to Archface-U

When you select `Code -> iArch`, you can generate Archface-U description corresponding to the Java code which you chose in the configuration described below.

For example, from 4 classes in the project, you can get the Archface-U like:

![code to Archface-U result](../images/sync_code_arch_1.png)


### Archface-U to Java code

When you select `iArch -> Code`, you can generate Java code corresponding the Archcode which you chose in the configuration described below.

For example, from an Archface-U,

![Archface-U to model archfile](../images/sync_arch_code_1.png)

you can get Java code skeltons like:

![Archface-U to model result 1](../images/sync_arch_code_2.png)
![Archface-U to model result 2](../images/sync_arch_code_3.png)


## Other features

### Configuring type checking target

When you select `Check Archface Configuration` from the `iArch` menu of the toolbar, a dialogue box to select Archcode, Java code and model diagrams in the project will be displayed.
Selected elements are to be type checked when you save Java code.

![Select all](../images/select_all_dialog.png)


# Architecture

## Extension points

iArch-U provides its functionalities through these Eclipse extension points.

- org.eclipse.ui.menus: adds menu items to the toolbar and context menus.
- org.eclipse.ui.views: adds Archface-U View, which manages uncertainty.
- org.eclipse.ui.startup: registers a functionality to perform type checking automatically on saving files.
- org.eclipse.ui.ide.markerResolution: adds functionalities to fix programs based on results of the type checking.


## Package structure

iArch-U consists of these Java packages.

- archdsl: defines the Archface-U language using Xtext.
- basefunction: defines functions which are used by model editors and other plug-in functionalities.
- checkplugin: provides functionalities such as type checking, testing support and so on.
- model: defines models for model editors.
- classdiagram: provides class diagram editor using Graphiti.
- sequencediagram: provides sequence diagram editor using Graphiti.
