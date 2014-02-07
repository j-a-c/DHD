all:
	# Compile Java Classes
	javac DHD/*.java
	# Create Jar
	jar -cvf DHD.jar DHD/*.class
