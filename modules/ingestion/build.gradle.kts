plugins {
    `java-library`
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:model"))
    implementation(project(":modules:embeddings"))
    implementation(project(":modules:vectorstore"))
    
    implementation("org.apache.tika:tika-core:${property("tikaVersion")}")
    implementation("org.apache.tika:tika-parsers-standard-package:${property("tikaVersion")}")
    implementation("org.apache.pdfbox:pdfbox:${property("pdfboxVersion")}")
    implementation("dev.langchain4j:langchain4j-document-parser-apache-tika:${property("langchain4jVersion")}")
    
    implementation("org.springframework:spring-context:6.1.13")
    implementation("commons-codec:commons-codec:1.16.0")
}