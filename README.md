# webEnc-mvn
Maven plugin that encrypts resources and provides JavaScript to decrypt them on the client side


```xml
            <plugin>
                <groupId>lu.dainesch</groupId>
                <artifactId>WebEncPlugin</artifactId>
                <version>1.0</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>webEnc</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <!-- Password used for encryption. REQUIRED -->
                    <password>abc123</password> 
                    <!-- Number of iterations for the PBKDF. OPTIONAL -->
                    <iterations>65536</iterations>
                    <!-- AES strength. OPTIONAL -->
                    <keySize>256</keySize>
                    <!-- Input directory. REQUIRED -->
                    <inputDirectory>${basedir}/src/main/webapp/input</inputDirectory>
                    <!-- Outprut directory. OPTIONAL -->
                    <outputDirectory>${basedir}/src/main/webapp/</outputDirectory>
                    <!-- Directory where the JS scripts are created. OPTIONAL -->
                    <scriptDirectory>${basedir}/src/main/webapp/js</scriptDirectory>
                </configuration>
            </plugin>
```

More to come ...
