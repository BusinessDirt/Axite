val log4j2Version = "2.25.3"

dependencies {
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4j2Version"))
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4j2Version")
}