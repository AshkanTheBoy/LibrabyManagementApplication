plugins {
	java
	application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
	// https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
	implementation("org.xerial:sqlite-jdbc:3.49.0.0")
}

tasks.test {
	useJUnitPlatform()
}

application {
	mainClass.set("App")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

