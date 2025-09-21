plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:model"))
    
    api("dev.langchain4j:langchain4j-core:${property("langchain4jVersion")}")
    api("dev.langchain4j:langchain4j-qdrant:${property("langchain4jVersion")}")
    api("dev.langchain4j:langchain4j-elasticsearch:${property("langchain4jVersion")}")
    
    implementation("co.elastic.clients:elasticsearch-java:${property("elasticsearchVersion")}")
    implementation("io.qdrant:client:${property("qdrantVersion")}")
    
    implementation("org.springframework:spring-context:6.1.13")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
}