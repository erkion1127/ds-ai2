plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:model"))
    implementation(project(":modules:embeddings"))
    implementation(project(":modules:vectorstore"))
    
    api("dev.langchain4j:langchain4j-core:${property("langchain4jVersion")}")
    api("dev.langchain4j:langchain4j-ollama:${property("ollamaVersion")}")
    api("dev.langchain4j:langchain4j:${property("langchain4jVersion")}")
    
    implementation("org.springframework:spring-context:6.1.13")
}