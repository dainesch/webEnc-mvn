# webEnc-mvn

This maven plugin encrypts selected resources of your website using AES. To decode the resources JavaScript
scripts are created that will decrypt and display the resources on the client side.

Once the correct password is provided to the website the script will provide the following functionalities:

* Automatically processes attributes of html elements
* Display encrypted images
* Replace website parts with encrypted html/text
* Allow downloading of decrypted files 

# How to use

#Requirements

The client side JavaScript requires [SJCL](http://bitwiseshiftleft.github.io/sjcl/) compiled with the optional
support for ArrayBuffers (sjcl.codec.bytes). SJCL is not provided with this plugin and must be included
by you.

If you need the ability to save/download files you additionally need [FileSaver.js](https://github.com/eligrey/FileSaver.js/).

* [SJCL](http://bitwiseshiftleft.github.io/sjcl/)
* [FileSaver.js](https://github.com/eligrey/FileSaver.js/)

## Maven config

You need to ass the following to your pom.xml:

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
                    <!-- Output directory. OPTIONAL -->
                    <outputDirectory>${basedir}/src/main/webapp/</outputDirectory>
                    <!-- Directory where the JS scripts will be created. OPTIONAL -->
                    <scriptDirectory>${basedir}/src/main/webapp/js</scriptDirectory>
                </configuration>
            </plugin>
```

The minimal configuration requires the encryption password and an input directory to indicated
which files should be encrypted.

Note: Each time you build the project the encrypted files will change as a separate salt is generated for
each file.

## Client side HTML

Include the desired dependencies:

```html
        <!-- MANDATORY: used to decrypt AES -->
        <script src="./js/sjcl.js" type="text/javascript" ></script>
        <!-- OPTIONAL: If you want to save decrypted files to the computer -->
        <script src="./js/FileSaver.js" type="text/javascript" ></script>
        <!-- MANDATORY: config that will be used to decrypt the files (settings from pom.xml) -->
        <script src="./js/WebEnc.conf.js" type="text/javascript" ></script>
        <!-- MANDATORY: Main script -->
        <script src="./js/WebEnc.js" type="text/javascript" ></script>
```

Initialise the script and process HTML:

```html
<script>
document.addEventListener("DOMContentLoaded", function () {
    
    // init and try to read password from previously set cookie
    WebEnc.init(true);
    console.log("Cookie unlock " + WebEnc.isUnlocked());
    
    // test given password
    if (WebEnc.testPassPhrase("abc123", true)) {
        console.log("Password unlock " + WebEnc.isUnlocked());

        // process the attributes of html elements
        WebEnc.processHTML();

        // manually perform actions
        //WebEnc.displayHTML('/hello.html', document.getElementById("cont"));
        //WebEnc.displayImage('/sam.png', document.getElementById("testImg"));
    }

});
</script>
```

## Supported elements and attributes

### Images
    
    <img src="./test.png" alt="test" data-enc-img="./test.png">

The attribute 'data-enc-img' tells the script that it should decode './test.png' and display it as image on the given current
'img' element. For easy of development of the encrypted sites, you can still indicate the 'src' to see what you are designing.

### Downloads
    
    <a href="./test.pdf" data-enc-save="./test.pdf" data-enc-type="application/pdf">Download link</a>
    <button data-enc-save="./test.pdf" data-enc-type="application/pdf">Download button</a>

Links and buttons can use the 'data-enc-save' attribute to indicate that they should save the decrypted file. As we have no knowledge
of the file type that is encrypted you have to indicated the mime type that the browser should use to save the file with the 'data-enc-type' attribute.

### HTML and text

    <a href="./hello.html" data-enc-link="./hello.html" data-enc-target="cont">Hello page</a>
    <button data-enc-link="./hello.html" data-enc-target="cont">Hello page</button>

Depending on your website design choice you may need to navigate between different encrypted sites (HTML).
Links and button can indicate with 'data-enc-link' the source of the encrypted HTML/text/Javascript and 
with 'data-enc-target' the target HTML element whose content will be replaced with the encrypted data.