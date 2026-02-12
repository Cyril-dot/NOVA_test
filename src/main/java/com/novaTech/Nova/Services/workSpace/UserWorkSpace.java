package com.novaTech.Nova.Services.workSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Entities.repo.WorkSpaceRepo;
import com.novaTech.Nova.Entities.workSpace.WorkSpaceDocs;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserWorkSpace {
    private final UserRepo userRepo;
    private final WorkSpaceRepo workSpaceRepo;

    // find user by id
    public User getUserById(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User with email " + email + " not found"));
    }

    // to create a workspace
    public WorkSpaceCreationResponse createWorkspaceForUser(UUID userId, WorkSpaceCreationDto creationDto){
        log.info("Creating user work space for user id {},", userId);
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found, incorrect user id");
        }


        WorkSpaceDocs workSpaceDocs = new WorkSpaceDocs();
        workSpaceDocs.setUser(user);
        workSpaceDocs.setTitle(creationDto.title());
        workSpaceDocs.setDescription(creationDto.description());
        workSpaceDocs.setCreatedAt(LocalDateTime.now());

        workSpaceRepo.save(workSpaceDocs);

        return workSpaceBuilderResponse(workSpaceDocs);
    }

    private WorkSpaceCreationResponse workSpaceBuilderResponse(WorkSpaceDocs workSpaceDocs) {
        log.info("Building work space creation response");
        return  new WorkSpaceCreationResponse(
                workSpaceDocs.getId(),
                workSpaceDocs.getTitle(),
                workSpaceDocs.getDescription(),
                workSpaceDocs.getUser().getUsername(),
                workSpaceDocs.getUser().getEmail(),
                workSpaceDocs.getCreatedAt(),
                "Work Space created successfully"
        );
    }


    // to work in the docs, that is create a template for the specified doctype
    public ActiveWorkSpaceDocs getWorkSpaceDocsTemplate(DocType docType, UUID userId, Long docId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to access this workspace");
        }

        String template = "";

        // now we are going to be using a switch case statement for this
        switch (docType) {
            case HTML:
                template = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>My Document</title>
                        </head>
                        <body>
                            <h1>Hello, World!</h1>
                        </body>
                        </html>
                        """;
                break;
            case JS:
                template = """
                        // JavaScript Template
                        console.log("Hello, World!");
                        
                        function main() {
                            // Your code here
                        }
                        
                        main();
                        """;
                break;
            case CSS:
                template = """
                        /* CSS Template */
                        body {
                            font-family: Arial, sans-serif;
                            margin: 0;
                            padding: 0;
                            background-color: #f4f4f4;
                        }
                        
                        h1 {
                            color: #333;
                        }
                        """;
                break;
            case JAVA:
                template = """
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("Hello, World!");
                            }
                        }
                        """;
                break;
            case PYTHON:
                template = """
                        # Python Template
                        def main():
                            print("Hello, World!")
                        
                        if __name__ == "__main__":
                            main()
                        """;
                break;
            case C_SHARP:
                template = """
                        using System;
                        
                        namespace HelloWorld
                        {
                            class Program
                            {
                                static void Main(string[] args)
                                {
                                    Console.WriteLine("Hello World!");
                                }
                            }
                        }
                        """;
                break;
            case C_PLUS_PLUS:
                template = """
                        #include <iostream>
                        
                        int main() {
                            std::cout << "Hello World!";
                            return 0;
                        }
                        """;
                break;
            case RUBY:
                template = """
                        # Ruby Template
                        puts "Hello, World!"
                        """;
                break;
            case PHP:
                template = """
                        <?php
                        echo "Hello, World!";
                        ?>
                        """;
                break;
            case SWIFT:
                template = """
                        import Swift
                        print("Hello, World!")
                        """;
                break;
            case GO:
                template = """
                        package main
                        
                        import "fmt"
                        
                        func main() {
                            fmt.Println("Hello, World!")
                        }
                        """;
                break;
            case R:
                template = """
                        # R Template
                        print("Hello, World!")
                        """;
                break;
            case KOTLIN:
                template = """
                        fun main() {
                            println("Hello, World!")
                        }
                        """;
                break;
            case SCALA:
                template = """
                        object Hello extends App {
                            println("Hello, World!")
                        }
                        """;
                break;
            case TYPESCRIPT:
                template = """
                        // TypeScript Template
                        const message: string = "Hello, World!";
                        console.log(message);
                        """;
                break;
            case SQL:
                template = """
                        -- SQL Template
                        SELECT * FROM table_name;
                        """;
                break;
            case NO_SQL:
                template = """
                        // NoSQL Template (e.g., MongoDB)
                        db.collection.find({});
                        """;
                break;
            case MARKDOWN:
                template = """
                        # Hello, World!
                        
                        This is a markdown template.
                        """;
                break;
            case TEXT:
                template = "Hello, World!";
                break;
            default:
                template = "";
                log.warn("Unknown DocType: {}", docType);
        }

        docs.setWorkSpaceData(template.getBytes(StandardCharsets.UTF_8));
        // Assuming WorkSpaceDocs has a field for DocType, if not, you might need to add it or just return it.
        docs.setDocType(docType);
        docs.setUpdatedAt(LocalDateTime.now());
        docs.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                template,
                docType
        );
    }


    // to view the working docs
    public ViewWorkSpaceDocsData viewDoc(UUID userId, Long docId){
        User user = getUserById(userId);
        if (user == null){
            log.info("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found, id doesnt exist");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (docs.getUser().getId() != user.getId()){
            log.info("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        docs.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(docs);


        return docsBuilder(docs);
    }

    private ViewWorkSpaceDocsData docsBuilder(WorkSpaceDocs spaceDocs){
        return new ViewWorkSpaceDocsData(
                spaceDocs.getTitle(),
                spaceDocs.getDescription(),
                new String(spaceDocs.getWorkSpaceData(), StandardCharsets.UTF_8)
        );
    }

    // Update workspace content and work in it
    public ActiveWorkSpaceDocs workInSpaceDocs(UUID userId, Long docId, UpdateWorkSpaceDocsDto updateDto) {
        User user = getUserById(userId);
        if (user == null) {
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this workspace");
        }

        // Inject the user's content into the appropriate template location
        String wrappedContent = injectContentIntoTemplate(updateDto.content(), docs.getDocType());

        docs.setWorkSpaceData(wrappedContent.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                wrappedContent,         // return the fully wrapped content
                docs.getDocType()
        );
    }


    // Helper method to inject content into the appropriate template location
    private String injectContentIntoTemplate(String content, DocType docType) {
        return switch (docType) {
            case HTML -> """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>My Document</title>
                </head>
                <body>
                    %s
                </body>
                </html>
                """.formatted(content);

            case JS -> """
                // JavaScript Template
                console.log("Hello, World!");
                
                function main() {
                    %s
                }
                
                main();
                """.formatted(content);

            case CSS -> """
                /* CSS Template */
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f4f4f4;
                }
                
                %s
                """.formatted(content);

            case JAVA -> """
                public class Main {
                    public static void main(String[] args) {
                        %s
                    }
                }
                """.formatted(content);

            case PYTHON -> """
                # Python Template
                def main():
                    %s
                
                if __name__ == "__main__":
                    main()
                """.formatted(content);

            case C_SHARP -> """
                using System;
                
                namespace HelloWorld
                {
                    class Program
                    {
                        static void Main(string[] args)
                        {
                            %s
                        }
                    }
                }
                """.formatted(content);

            case C_PLUS_PLUS -> """
                #include <iostream>
                
                int main() {
                    %s
                    return 0;
                }
                """.formatted(content);

            case RUBY -> """
                # Ruby Template
                %s
                """.formatted(content);

            case PHP -> """
                <?php
                %s
                ?>
                """.formatted(content);

            case SWIFT -> """
                import Swift
                %s
                """.formatted(content);

            case GO -> """
                package main
                
                import "fmt"
                
                func main() {
                    %s
                }
                """.formatted(content);

            case R -> """
                # R Template
                %s
                """.formatted(content);

            case KOTLIN -> """
                fun main() {
                    %s
                }
                """.formatted(content);

            case SCALA -> """
                object Hello extends App {
                    %s
                }
                """.formatted(content);

            case TYPESCRIPT -> """
                // TypeScript Template
                %s
                """.formatted(content);

            case SQL -> """
                -- SQL Template
                %s
                """.formatted(content);

            case NO_SQL -> """
                // NoSQL Template (e.g., MongoDB)
                %s
                """.formatted(content);

            case MARKDOWN -> """
                # Document
                
                %s
                """.formatted(content);

            case TEXT -> content;

            default -> {
                log.warn("Unknown DocType: {}", docType);
                yield content;
            }
        };
    }


    // here to download a single workspace docs
    public ResponseEntity<byte[]> downloadFile(WorkSpaceDocs document, UUID userId) {
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found, id doesnt exist");
        }

        String mimeType = document.getDocType().getMimeType();
        String extension = document.getDocType().getExtension();
        String fileName = document.getTitle() + extension;

        document.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(document);


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentLength(document.getWorkSpaceData().length)
                .body(document.getWorkSpaceData());
    }


    // to delete to workspace docs
    public String deleteDocs(UUID userId, Long docId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found, id doesnt exist");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (docs.getUser().getId() != user.getId()){
            log.info("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        workSpaceRepo.delete(docs);

        log.info("Workspace docs with id {} deleted successfully", docId);
        return "Workspace docs deleted successfully";
    }


    // to update from where u left of
    private String updateContentWithinTemplate(String existingFullDocument, String newContent, DocType docType) {
        return switch (docType) {
            case HTML -> {
                // Replace everything between <body> and </body> with the new content
                yield existingFullDocument.replaceAll(
                        "(?s)(<body>)(.*?)(</body>)",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case JAVA -> {
                // Replace everything inside main() method body
                yield existingFullDocument.replaceAll(
                        "(?s)(public static void main\\(String\\[\\] args\\) \\{)(.*?)(\\}\\s*\\})",
                        "$1\n        " + newContent + "\n    $3"
                );
            }

            case PYTHON -> {
                // Replace everything inside def main():
                yield existingFullDocument.replaceAll(
                        "(?s)(def main\\(\\):)(.*?)(\\nif __name__)",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case JS, TYPESCRIPT -> {
                // Replace everything inside the main() function body
                yield existingFullDocument.replaceAll(
                        "(?s)(function main\\(\\) \\{)(.*?)(\\})",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case C_SHARP -> {
                // Replace inside the innermost Main method
                yield existingFullDocument.replaceAll(
                        "(?s)(static void Main\\(string\\[\\] args\\)\\s*\\{)(.*?)(\\}\\s*\\}\\s*\\})",
                        "$1\n            " + newContent + "\n        $3"
                );
            }

            case C_PLUS_PLUS -> {
                // Replace between the opening brace of main and return 0;
                yield existingFullDocument.replaceAll(
                        "(?s)(int main\\(\\) \\{)(.*?)(return 0;)",
                        "$1\n    " + newContent + "\n    $3"
                );
            }

            case KOTLIN -> {
                yield existingFullDocument.replaceAll(
                        "(?s)(fun main\\(\\) \\{)(.*?)(\\})",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case SCALA -> {
                yield existingFullDocument.replaceAll(
                        "(?s)(object Hello extends App \\{)(.*?)(\\})",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case GO -> {
                yield existingFullDocument.replaceAll(
                        "(?s)(func main\\(\\) \\{)(.*?)(\\})",
                        "$1\n    " + newContent + "\n$3"
                );
            }

            case PHP -> {
                yield existingFullDocument.replaceAll(
                        "(?s)(\\<\\?php)(.*?)(\\?>)",
                        "$1\n" + newContent + "\n$3"
                );
            }

            case CSS -> {
                // Append new CSS rules after the base body styles
                yield existingFullDocument.replaceAll(
                        "(?s)(h1 \\{.*?\\})(.*?)$",
                        "$1\n\n" + newContent
                );
            }

            case RUBY, SWIFT, R -> {
                // These are simple single line templates, just append after the comment/import line
                String[] lines = existingFullDocument.split("\n", 2);
                yield lines[0] + "\n" + newContent;
            }

            case SQL -> {
                // Replace after the comment header
                yield existingFullDocument.replaceAll(
                        "(?s)(-- SQL Template\n)(.*)",
                        "$1" + newContent
                );
            }

            case NO_SQL -> {
                yield existingFullDocument.replaceAll(
                        "(?s)(// NoSQL Template \\(e\\.g\\., MongoDB\\)\n)(.*)",
                        "$1" + newContent
                );
            }

            case MARKDOWN -> {
                // Keep the heading, replace everything after it
                yield existingFullDocument.replaceAll(
                        "(?s)(# Document\n\n)(.*)",
                        "$1" + newContent
                );
            }

            case TEXT -> newContent; // Plain text just fully replaces

            default -> {
                log.warn("Unknown DocType for content update: {}", docType);
                yield newContent;
            }
        };
    }


    public ActiveWorkSpaceDocs updateWorkSpaceExistingDocs(UUID userId, Long docId, UpdateWorkSpaceDocsDto updateDto) {
        User user = getUserById(userId);
        if (user == null) {
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this workspace");
        }

        // Read the existing full document from the DB
        String existingFullDocument = new String(docs.getWorkSpaceData(), StandardCharsets.UTF_8);

        // Inject the new content into the correct position within the existing document
        String updatedDocument = updateContentWithinTemplate(existingFullDocument, updateDto.content(), docs.getDocType());

        docs.setWorkSpaceData(updatedDocument.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                updatedDocument,
                docs.getDocType()
        );
    }


    // to zip all user workspace files and download them
    public ResponseEntity<byte[]> workSpaceFolder(UUID userId) {
        User user = getUserById(userId);
        if (user == null) {
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found, id doesnt exist");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findAll();

        // Filter docs belonging to the user
        List<WorkSpaceDocs> userDocs = docs.stream()
                .filter(doc -> doc.getUser().getId().equals(user.getId()))
                .toList();

        if (userDocs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (WorkSpaceDocs doc : userDocs) {
                String extension = doc.getDocType().getExtension();
                String fileName = doc.getTitle() + extension;
                byte[] fileData = doc.getWorkSpaceData();

                ZipEntry zipEntry = new ZipEntry(fileName);
                zipEntry.setSize(fileData.length);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(fileData);
                zipOut.closeEntry();
            }

            zipOut.finish();

            byte[] zipBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"workspace_" + userId + ".zip\"")
                    .contentLength(zipBytes.length)
                    .body(zipBytes);

        } catch (IOException e) {
            log.error("Failed to create zip for user {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }



    // to view workSpaces by doctype
    public List<UserWorkSpaceDocs> viewDocsByType(UUID userId, DocType docType){
        User user = getUserById(userId);
        if (user == null) {
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByUserIdAndDocType(user.getId(), docType);
        if (docs == null){
            log.error("No docs found for user id ,{} under doc type {},",userId, docType);
            return List.of();
        }
        docs.forEach(doc -> {
            doc.setLastViewed(LocalDateTime.now());
        });
        workSpaceRepo.saveAll(docs); // Explicit save to be sure

        return docsViewByType(docs);
    }

    private List<UserWorkSpaceDocs> docsViewByType(List<WorkSpaceDocs> docs){
        return docs.stream()
                .map(doc -> new UserWorkSpaceDocs(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getDescription(),
                        new String(doc.getWorkSpaceData(), StandardCharsets.UTF_8),
                        doc.getUser().getUsername()
                ))
                .collect(Collectors.toList()); // This was missing
    }

    // to search for workspace docs , general search
    public List<UserWorkSpaceDocs> searchDocs(UUID userId, String keyword){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }
        List<WorkSpaceDocs> docs = workSpaceRepo.searchByUserAndKeyword(user.getId(), keyword);
        if (docs == null){
            log.error("No docs");
            return List.of();
        }

        docs.forEach(doc -> {
            doc.setLastViewed(LocalDateTime.now());
        });
        workSpaceRepo.saveAll(docs);

        return docsViewByType(docs);
    }


    // to view most recent docs
    public List<UserWorkSpaceDocs> viewRecentDocs(UUID userId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }
        List<WorkSpaceDocs> docs = workSpaceRepo.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        if (docs == null){
            log.error("No docs");
            return List.of();
        }
        return docsViewByType(docs);
    }

    // to view recently accessed docs
    public List<UserWorkSpaceDocs> viewRecentlyAccessedDocs(UUID userId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User not found");
        }
        List<WorkSpaceDocs> docs = workSpaceRepo.findTop10ByUserIdOrderByLastViewedDesc(user.getId());
        if (docs == null){
            log.error("No docs");
            return List.of();
        }

        return docsViewByType(docs);
    }

}
