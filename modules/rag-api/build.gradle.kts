plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:model"))
    implementation(project(":modules:embeddings"))
    implementation(project(":modules:vectorstore"))
    implementation(project(":modules:ingestion"))
    implementation(project(":modules:rag-core"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:${property("langchain4jVersion")}")
    
    // Apache POI for Excel processing
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.poi:poi-scratchpad:5.2.5")
    
    // JPA and MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.jsoup:jsoup:1.16.1")
    
    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // WebClient for HTTP calls
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}