
1) Create a new project in your IDE.
2) Copy the 'src' folder into the project's source folder (often called 'src' too).
3) Put the jar file into the project folder and add it to the project's class or build path.
4) Put the 'rgbelevation' folder into the project folder.
5) The 'javadoc' folder can go wherever you like, but the project folder is not a bad idea. View the index.html file inside the 'javadoc' folder for the API of the files in the jar file.

IDEs may vary in their behaviour:
- You may need to refresh the project to see files you have added to it manually
- You may need to explicitly add the jar file to the class or build path in an IDE-specific manner.
- With the rgbelevation folder in the project the Demo program should find the elevation files easily and the SearchUI file requester should open in that folder automatically when you run the program, but this depends on the IDE using the project folder as the current directory at runtime (it probably will, but if not you will have to fix the file paths in Demo and browse to the correct location in SearchUI).