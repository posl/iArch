# Modelling and uncertainty management

<!-- BEGIN common section -->
<!-- Video: show the IDE entire view -->
This is a tutorial video of the iArch integrated development environment.

In this video, I'm going to explain how you can edit UML models of an example application to incorporate emerging uncertainty by using the iArch model editor.

<!-- [[00:**]] -->
<!-- Video: show the tutorial app in video -->
<!-- Caption Lv1: a demo of the tutorial app -->
Suppose, we are making a GUI application for a school teacher which displays a student list, and this application has a functionality to filter out unattending students.

<!-- [[00:**]] -->
<!-- Caption Lv3: a behaviour of the app -->
When the button is clicked, students who don't have taken the class are filtered out from the list.

<!-- [[00:32]] -->
<!-- Video: show the IDE entire view -->
iArch has a mechanism to control the structure and behaviour of the program and the model.

You can employ that mechanism by describing conditions imposed on a model, by a domain specific language, Archface.

Archface is a name of the language, and the code written in Archface is called Archcode.

<!-- [[00:57]] -->
<!-- Video: show the Archcode -->

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

<!-- Caption Lv1: an example Archcode of this app -->
This is an example Archcode of this application, which has two component definitions and one connector definition.

Main component has actionPerformed method and studentController component has filterStudent method.

And in cStudent connector definition, you can see that Main class's actionPerformed method is called at first, and then studentController's filterStudent method should be called later.

<!-- END common section -->

<!-- [[0*:**]] -->
<!-- Caption Lv1: how to manage uncertainty  -->
These are corresponding UML diagrams, one is a class diagram which corresponds to the component definitions and the other is a sequence diagram which corresponds to the connector definition.

This is an overview of the application and how model's structure and behavioural description are controlled by an Archface definition.

Next, let me explain how we can manage uncertainty which arises during the development process of the program by using this example application.

<!-- [[0*:**]] -->
<!-- Caption Lv3: filtering out unattending students -->
Suppose, the teacher started to think that it might be good to display attending students not by filtering out unattending students, but by colouring background of the attending students' name.
<!-- [[0*:**]] -->
<!-- Caption Lv3: colouring background of the attending students' name -->

<!-- [[01:54]] -->
<!-- Caption Lv3: Add colorStudent method declaration into the class diagram.  -->
To incorporate this idea, we first add colorStudent method declaration into the class diagram.

<!-- [[0*:**]] -->
<!-- Caption Lv3: Record an uncertainty  -->
At this time, we don't have decided yet whether we actually implement colorStudent method, so we record this uncertainty by marking colorStudent method as optional, by selecting it from the context menu this way.

You can see that the corresponding Archcode incorporates optional uncertainty of the model, as colorStudent is enclosed by square brackets.

After a while, we decided to implement colorStudent, with the same parameter type as filterStudent, and we need to touch up the method signature in Archcode accordingly.

<!-- [[0*:**]] -->
<!-- Caption Lv3: Remove an uncertainty -->
After implementing colorStudent method, you can turn this uncertain declaration into certain one, by selecting "Remove Uncertainty" from the context menu like this.

As to the sequence diagram, you can control uncertainty by selecting an appropriate operation from the context menu as well.

If the teacher still doesn't have decided which one he would take, filterStudent or colorStudent, we can express this uncertainty as alternative.

<!-- [[0*:**]] -->
<!-- Caption Lv3: Set an alternative uncertainty -->
Select this method call arrow in the sequence diagram, and click "Set Alternative" operation from the context menu.

Input the alternative method name to this dialogue box, which is colorStudent in this case.

Here you see the sequence diagram has been changed to display alternative uncertainty and the corresponding Archcode has also been changed.

Now we have seen two types of uncertainty and how they would be modelled by iArch.
