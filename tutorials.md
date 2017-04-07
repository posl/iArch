---
layout: default
title: Tutorials
permalink: /tutorials/
priority: 10
---

<h1 markdown="0" id="page-title">Tutorials</h1>

Welcome to the iArch-U tutorials.

- TOC
{:toc}

# Preparing the sample project

First, please clone [the repository](https://github.com/posl/iArch/) of iArch-U.

```
$ git clone https://github.com/posl/iArch.git
$ cd iArch
```

Here you will find `StudentList` folder, the sample project you use in this tutorials.
Then please launch Eclipse, create new java project, uncheck "Use default location", and select `StudentList` folder as the project's location.


# iArch-U editors and a view

iArch-U is implemented on top of the Eclipse IDE and you can access functionalities provided by iArch-U using integrated editors and a view.
To take a glance at these editors and a view, open the following files contained in the sample project:

- StudentList/Main.Java
- StudentList/arch/student.arch
- StudentList/diagrams/Class.diagram
- StudentList/diagrams/Sequence.diagram

You may notice that there appear some code editors and UML diagrams.
These are displayed in custom editors of iArch-U to help you design and implement programs embracing uncertainty.


## Archface-U editor

![Archface-U editor](../images/archcode.jpg)

In this editor, we can write Archcode, which is code written in the Archface-U DSL, just like you can write Java code in the Java editor.
Archcode fills the gap between programs and models by enforcing both sides to be aligned with the interface described as Archcode.
If there are misalignment between them, errors will be reported.


## iArch-U model editors

![iArch-U class diagram editor](../images/class_diagram_tut_01.jpg)

![iArch-U sequence diagram editor](../images/sequence_diagram_tut_01.jpg)

You can see iArch-U class and sequence diagrams, which are extensions of the UML class and sequence diagrams respectively, by opening corresponding files.
Once you make some changes in these diagrams, Archcode will be changed accordingly.
This will happen not only when a certain component or connector is changed to be uncertain, but also when an uncertain component or connector is changed to be certain.


## Archface-U view

![Archface-U view](../images/archface_u_view_tut.jpg)

`Archface-U View` reports the overview of the project status regarding uncertainty.
It displays the current component status as well as history information collected from the past commit log of a tied repository.
The behaviour list is also displayed under the component list.


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

Archface-U is the interface system, so you are to describe interfaces in Archface-U.

Let's take a look at the code above.
A component declares a class and methods, and a connector describes call relationships between methods.
In a component, the declaration of classes specifies the name, and that of methods does the name, the type of return value, the type of arguments, arguments' name and the class to be belonged.


# Programming

<div markdown="0" class="tutorial-video">
  <div markdown="0" class="video-wrapper tutorial-video-wrapper">
    <iframe src="https://www.youtube.com/embed/1KT7TTGAtQs?rel=0" frameborder="0" allowfullscreen></iframe>
  </div>
</div>

Suppose the request below: the teacher want you to consider the idea that the list displays the attendees by coloring.

In *StudentController* class, we defined the method *filterStudent* which shows only attendees, and also the method *colorStudent* which display the attendees by coloring.
For the implementation of the method *colorStudent* is tentative now, It is uncertain whether it is used in final product.
To manage this uncertainty, you can express the uncertainty whether this system will be implemented with using *colorStudent* method.

In Archface-U, we describe alternative uncertainty with bracketing by **"{ }"** like **"{ A, B }"**.
Optional uncertainty is described with bracketing by **"[ ]"** like **"[ C ]"**.
So you would express the code as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{
	void filterStudent(JTable table);
}
uncertain component uStudentController extends StudentController{
	[ void colorStudent(JTable table); ]
}

interface connector cStudent{
	Main = (Main.actionPerformed -> StudentController.filterStudent -> Main);
}
```

Then you can implement the method *colorStudent* as below.

```
public static void colorStudent(JTable table){
	table.setDefaultRenderer(Object.class, new StudentTableCellRender());
	table.repaint();
}
```

In this code above, the changes from the original is the section *uStudentController*.
Like these, classes and methods which has uncertainty are declared with **"uncertain"** instead of **"interface"**.
It is a good habit that the classes uncertain about implementation are named with prefix **"u"**.
In *uStudentController* , *colorStudent* method has uncertainty whether will be implemented finally, so bracketing with **"[ ]"** presents the method is optional.

---

Afterwards, the teacher wants to see how both of the methods work, in order to decide which implementation to adopt.
Now there is an another uncertainty, so that, whether the implementation will be fixed as 1. or 2.

when the button is clicked,

1. the list will be switched to show only attendees.
2. the list will change the color of the attendees, along with showing those who do not attend.

So you would edit Archface-U as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{}
uncertain component uStudentController extends StudentController{
	{ void filterStudent(JTable table);,
	  void colorStudent(JTable table); }
}

interface connector cStudent{
	Main = (Main.actionPerformed -> Main);
}
uncertain connector ucStudent extends cStudent{
	Main = (Main.actionPerformed ->
		{ uStudentController.colorStudent,
		  uStudentController.filterStudent }  -> Main);
}
```

In *uStudentController*, currently we can't decide which to be implemented at last, so bracketing with **"{ }"** presents the methods are alternative.
In *ucStudent*, there is also the bracket that means it is uncertain which method is called in the method *actionPerformed*.

Additionally, you can generate a model map and a sequence map.

![iArch-U class diagram editor](../images/class_diagram_tut_02.jpg)

![iArch-U sequence diagram editor](../images/sequence_diagram_tut_02.jpg)


# Modeling

<div markdown="0" class="tutorial-video">
  <div markdown="0" class="video-wrapper">
  <iframe src="https://www.youtube.com/embed/V0yOCdouqd8?rel=0" frameborder="0" allowfullscreen></iframe>
  </div>
</div>

Here shows an example of Modeling.
At first, the class map and sequence map are expressed like this.

![iArch-U class diagram editor](../images/class_diagram_tut_01.jpg)

![iArch-U sequence diagram editor](../images/sequence_diagram_tut_01.jpg)

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

![iArch-U class diagram editor](../images/class_diagram_tut_02.jpg)

![iArch-U sequence diagram editor](../images/sequence_diagram_tut_02.jpg)

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
	ucStudent_auto_ub_0 = (Main.actionPerformed ->
		{ uStudentController_auto.filterStudent,
		  uStudentController_auto.colorStudent } -> Main);
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

Until the teacher decides, it remains to be uncertain which of the *implementation 1.* or *2.* is approved.
Here iArch-U Testing Support enables you to carry out usability test for this decision.
Based on annotations added to Archface-U, iArch-U changes the software behavior.
This behavior control is carried out without changing application code.

To use iArch-U Testing Support, make sure [AJDT](http://www.eclipse.org/ajdt/) is installed,
and convert the project into an AspectJ project by right-clicking the project and selecting
**Configure** -> **Convert to AspectJ Project**.

In this tutorial, as a temporary choice, choose `colorStudent`(*implementation 2.*).
So you would edit Archface-U as below.

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{}
uncertain component uStudentController extends StudentController{
	{ void filterStudent(JTable table);,
	  @ExecForce void colorStudent(JTable table); }
}
```

Please select **Generate Aspect** from the iArch menu in toolbar. This operation adds few files into your project directory. Running the program, you will see *implementation 2.* even if `filterStudent`(*implementation 1.*) is called in the application code.

You can also carry out "A/B testing", which is the probabilistic usability testing strategy.
For example, when the Archface-U is written as below, `filterStudent` and `colorStudent` will be executed at the ratio 4:1.
For more detailed useage of annotations, please see [documentation](documentation/).

```
interface component Main{
	void actionPerformed(ActionEvent e);
}

interface component StudentController{}
uncertain component uStudentController extends StudentController{
	{ @ExecWeight(4) void filterStudent(JTable table);,
	  @ExecWeight(1) void colorStudent(JTable table); }
}
```