plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:model"))
    
    api("dev.langchain4j:langchain4j-core:${property("langchain4jVersion")}")
    api("dev.langchain4j:langchain4j-ollama:${property("ollamaVersion")}")
    implementation("org.springframework:spring-context:6.1.13")
}