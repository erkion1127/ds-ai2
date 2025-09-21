plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common"))
    api("dev.langchain4j:langchain4j-core:${property("langchain4jVersion")}")
}