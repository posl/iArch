---
layout: default
title: Tutorials
permalink: /tutorials/
---

- TOC
{:toc}

# Preparing the sample project

First, please clone the sample project to your local machine from [StudentList](https://github.com/posl/iArch/tree/master/StudentList).
Then, open the cloned project as an existing Java project in iArch-U, or in Eclipse in other words.


# Brief introduction to iArch-U

This section provides an introduction to the iArch-U IDE.
First, open the following files contained in the sample project:

- StudentList/Main.Java
- StudentList/arch/student.arch
- StudentList/diagrams/Class.diagram
- StudentList/diagrams/Sequence.diagram

Taking a look at the appearance of iArch-U, there are some view sections.
These views are peculiar to iArch-U, helping implement and design embracing uncertainty in your project.


## Archface-U editor

![Archface-U editor](../images/archcode.jpg)

In this view, We can express the Archface-U code.
We should equalize the structure of classes and methods between Archcode in this section and Java source code.
If there are contradiction between them, error will be occured.


## iArch-U model editors

![iArch-U class diagram editor](../images/classDiagram.jpg)

![iArch-U sequence diagram editor](../images/sequenceDiagram.jpg)

The class diagram and the sequence diagram belong to this section.
If there are some structural update in this diagrams, then Archcode will be changed to follow this update.
This change will be operated not only if uncertainty is changed to be certain, but also if certain component or connector is changed to be uncertain.


## Archface-U view

![Archface-U view](../images/archfaceView.jpg)

Archface-U view provides the overview of uncertainty in the project.
Component names, uncertain types, implementation status and recent changes are listed.
Behaviors are also listed under components list.


# Overview of the sample project

In this tutorial, We suppose the GUI application for a teacher which tell the students who attend his/her lecture within the all students list.
We will cope with some uncertainty in application development while proceeding this tutorials.

At first, this application is implemented as below: when the button is clicked in this app, the student list will be switched to show only attendees.

At this point, there is no uncertainty.
So the Archcode can be written as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{
	void filterStudent(JTable table);
}

interface connector cStudent{
	Main = (Main.actionPerformed -> StudentController.filterStudent -> Main);
}
```

Archface-U is the interface system.
So we describe interfaces in Archfile.

Let's take a look at the code above.
A component declares a class and methods, and a connector describes call relationships between methods.
In a component, the declaration of classes specifies the name, and that of methods does the name, the type of return value, the type of arguments, arguments' name and the class to be belonged.


# Programming and uncertainty management

Suppose the request below: the teacher wants to see how it works if the list displays the attendees by coloring, along with showing students who do not attend.
In this request, The uncertainty is whether the list will be fixed as 1. or 2.
When the button is clicked,

1. the list will be switched to show only attendees.
2. the list will change the color of the attendees, along with showing those who do not attend.

We define that, in *StudentController* class, the method which change to show only attendees is named as *"filterStudent"* and the method which display the attendees by coloring as *"colorStudent"*.

To manage the uncertainty caused from this request, we express this uncertainty, whether this system will be implemented with using *colorStudent* method.

In Archface-U, we describe alternative uncertainty with bracketing by **"{}"** like **"{ A, B }"**.
Optional uncertainty is described with bracketing by **"[]"** like **"[ C ]"**.
So we can express the code as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{
	void filterStudent(JTable table);
}
uncertain component uStudentController
	extends StudentController{
		[void colorStudent(JTable table);]
}

interface connector cStudent{
	Main = (Main.actionPerformed -> Main);
}
uncertain connector ucStudent extends cStudent{
	Main = (Main.actionPerformed ->
		{uStudentController.colorStudent,
		StudentController.filterStudent}  -> Main);
}
```

See the StudentController class.
Add the coloring function.

```
public static void colorStudent(JTable table){
  table.setDefaultRenderer(Object.class, new StudentTableCellRender());
  table.repaint();
}
```

In this code above, the changes from the original are two sections: *uStudentController* and *ucStudent*.
Like these, classes and methods which are uncertain whether will be implemented are declared with **"uncertain"** instead of **"interface"**.
It should be good that the classes uncertain about implementation are named with prefix **"u"**.
In *uStudentController* , *colorStudent* method has uncertainty whether will be implemented finally, so bracketing with **"[ ]"** presents the method is optional.
Also in *ucStudent*, currently we can't decide which to be implemented at last, so bracketing with **"{ }"** presents the methods are alternative.

We can also generate a model map and a sequence map.

![iArch-U class diagram editor](../images/classDiagram_2.jpg)

![iArch-U sequence diagram editor](../images/sequenceDiagram_2.jpg)


# Modelling
Here shows an example of Modeling.
At first, the class map and sequence map are expressed like this.

![iArch-U class diagram editor](../images/classDiagram.jpg)

![iArch-U sequence diagram editor](../images/sequenceDiagram.jpg)

The code is written as below.

```
interface component Main{
 void actionPerformed(ActionEvent e);
}

interface component StudentController{
 void filterStudent(JTable table);
}

interface connector cStudent{
 Main = (Main.actionPerformed -> StudentController.filterStudent -> Main);
}
```

Subsequently, we will describe design models containing uncertainty using model editor.
At this time, model type inspection makes sure to describe models following Archface-U.

!iArch-U class diagram editor](../images/classDiagram_2.jpg)

![iArch-U sequence diagram editor](../images/sequenceDiagram_2.jpg)

The archcode at this time is written as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{
	void filterStudent(JTable table);
}

interface connector cStudent{
	Main = (Main.actionPerformed -> StudentController.filterStudent -> Main);
}

uncertain component uStudentController_auto extends StudentController {
	{ void filterStudent();, void colorStudent(); }
}
uncertain connector ucStudent_auto extends cStudent {
	ucStudent_auto_ub_0 = (Main.actionPerformed -> { uStudentController_auto.filterStudent, uStudentController_auto.colorStudent } -> Main);
}
```

We suppose that the Application's GUI is decided as Image below.
At this time, we should delete filterStudent in model editor because definition and calling of filterStudent method is no longer necessary.
iArch-U has the function which notice the dissipation of uncertainty.
Using this function to filterStudent method, if it is deleted from model and, filterStudent Component or Connecter in Archface-U will also be deleted.

Additionally, there is a function which does the opposite.
If the Component or Connector defined as certain is changed as uncertain, this function will apply this change on Archface-U.

This enables to manage uncertainty with ensuring traceability between models and Archface-U.


# Testing Support

It remains to be uncertain which of this GUI view 1. or 2. is approved until the teacher decides.
Prototyping with iArch-U Testing Support enables usability testing, which allow the developer to select temporarily on GUI.
iArch-U changed the software behavior to emulate the temporary selection.
This change is not implemented by changing code directly but by adding a few files.

In this tutorial, as a temporary choice, enabling the Optional Uncertainty in uStudentConroller connector and choosing ColorStudent from Alternative Uncertainty in ucStudent, we can display the view.
