all:
	# Compile Java Classes
	javac DHD/*.java
	# Create Jar
	jar cfm DHD.jar Manifest.txt DHD/*.class
