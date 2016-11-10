# Programming and uncertainty management

<!-- Video: show the IDE entire view -->
This is a tutorial video of the iArch integrated development environment.

In this video, I'm going to explain how you can program using iArch while managing emerging uncertainty, by showing and editing some example code.

<!-- [[00:21]] -->
<!-- Video: show the tutorial app in video -->
<!-- Caption Lv1: a demo of the tutorial app -->
Suppose, we are making a GUI application for a school teacher which displays a student list, and this application has a functionality to filter out unattending students.

<!-- [[00:32]] -->
<!-- Caption Lv3: a behaviour of the app -->
When the button is clicked, students who don't have taken the class are filtered out from the list.

<!-- [[00:32]] -->
<!-- Video: show the IDE entire view -->
iArch has a mechanism to control the structure and behaviour of the program and the model.

You can employ that mechanism by describing conditions imposed on a program, by a domain specific language, Archface.

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

This is an overview of the application and how program's structure and behaviour are controlled by an Archface definition.

<!-- [[01:26]] -->
<!-- Caption Lv1: how to manage uncertainty  -->
Next, let me explain how we can manage uncertainty which arises during the development process of the program by using this example application.

<!-- [[01:37]] -->
<!-- Caption Lv3: filtering out unattending students -->
Suppose, the teacher started to think that it might be good to display attending students not by filtering out unattending students, but by colouring background of the attending students' name.

<!-- [[01:44]] -->
<!-- Caption Lv3: colouring background of the attending students' name -->

<!-- [[01:53]] -->
The uncertainty here is that this application might be going to have colorStudent method instead of filterStudent method that it already has.

<!-- [[02:02]] -->
<!-- Caption Lv1: express this uncertainty -->
So, we express this uncertainty by editing Archcode.

<!-- [[02:09]] -->
<!-- Caption Lv2: Add an uncetain component. -->
First, let me add colorStudent method definition like this.

```
uncertain component uStudentController
	extends StudentController{
		void colorStudent(JTable table);
}
```

Well, we have an error occurred, but this is a correct behaviour because the current implementation doesn't have colorStudent method now.

<!-- [[02:26]] -->
<!-- Caption Lv2: Express the optional uncertainty -->
Next, we want to declare this colorStudent method to be optional.

You can express optional uncertainty by enclosing method signature with square brackets.

```
uncertain component uStudentController
	extends StudentController{
		[ void colorStudent(JTable table); ]
}
```

<!-- [[02:39]] -->
<!-- Caption Lv2: The first keyword of component definition is uncertain. -->
Please note that the first keyword of the component definition is "uncertain", not "interface".

<!-- [[02:51]] -->
<!-- Caption Lv2: Write colorStudent method. -->
Next, I'm going to write colorStudent method.

Ok, now we have implemented the actual colorStudent method, so we can remove optional uncertainty from Archcode.

This can be done by manually editing Archcode, but there is another way to do this.

Point the first line of the method definition in the Java code editor, and select "Remove Uncertainty" from the context menu.

You see a slight change in Archcode, that colorStudent is no longer enclosed by square brackets.

<!-- [[03:02]] -->
<!-- Caption Lv2: Express the alternative uncertainty -->
At this moment, the teacher still doesn't have decided which one he would take, filterStudent or colorStudent, so we want to express this uncertainty in Archcode.

```
uncertain connector ucStudent extends cStudent{
}
```

```
uncertain connector ucStudent extends cStudent{
	Main = (Main.actionPerformed ->
		{uStudentController.colorStudent,
		StudentController.filterStudent}  -> Main);
}
```

This is another type of uncertainty, alternative.

Alternative can be declared by enclosing target methods with curly brackets.

Now we have seen two types of uncertainty and how they would be managed by iArch.
