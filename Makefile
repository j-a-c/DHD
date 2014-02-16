all:
	# Compile Java Classes
	javac DHD/*.java -Xlint:unchecked
	# Create Jar
	jar cfm DHD.jar Manifest.txt DHD/*.class
