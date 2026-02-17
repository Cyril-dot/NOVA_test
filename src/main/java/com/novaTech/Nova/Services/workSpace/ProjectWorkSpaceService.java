package com.novaTech.Nova.Services.workSpace;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.DocType;
import com.novaTech.Nova.Entities.Project;
import com.novaTech.Nova.Entities.ProjectDocument;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.ProjectDocumentRepo;
import com.novaTech.Nova.Entities.repo.ProjectRepo;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectWorkSpaceService {

    private final WorkSpaceRepo workSpaceRepo;
    private final UserRepo userRepo;
    private final ProjectRepo projectRepo;
    private final ProjectDocumentRepo documentRepo;

    public User getUserById(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Project getProjectId(UUID projectId){
        return projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public ProjectDocument getDocId(UUID docId){
        return documentRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    // create workspace for project in general
    public ProjectWorkSpaceCreationResponse createProjectWorkSpace(WorkSpaceCreationDto creationDto,
                                                                   UUID projectId,
                                                                   UUID userId) {
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new RuntimeException("User not found");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs workSpaceDocs = new WorkSpaceDocs();
        workSpaceDocs.setTitle(creationDto.title());
        workSpaceDocs.setDescription(creationDto.description());
        workSpaceDocs.setUser(user);
        workSpaceDocs.setProject(project);
        workSpaceDocs.setCreatedAt(LocalDateTime.now());

        workSpaceRepo.save(workSpaceDocs);

        log.info("Work space created successfully");
        return projectBuilderResponse(workSpaceDocs);
    }


    private ProjectWorkSpaceCreationResponse projectBuilderResponse(WorkSpaceDocs workSpaceDocs){
        log.info("Building work space creation response");
        return new ProjectWorkSpaceCreationResponse(
                workSpaceDocs.getId(),
                workSpaceDocs.getTitle(),
                workSpaceDocs.getDescription(),
                "Work Space created successfully",
                workSpaceDocs.getProject().getTitle(),
                workSpaceDocs.getProject().getDescription(),
                workSpaceDocs.getCreatedAt()
        );
    }


    // to create project workspace document
    public ActiveProjectWorkSpaceDocs createProjectWorkSpaceDocsTemplate(DocType docType, UUID userId, Long docId, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }


        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
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

        return new ActiveProjectWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                template,
                docType,
                docs.getProject().getTitle()
        );
    }

    // to view
    public ViewProjectWorkSpaceDocs viewProjectWorkSpaceDocs(UUID projectId, UUID userId, Long docId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }


        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        return projectDocsBuilder(docs);
    }

    private ViewProjectWorkSpaceDocs projectDocsBuilder(WorkSpaceDocs spaceDocs){
        // ✅ NULL CHECK ADDED
        String content = "";
        if (spaceDocs.getWorkSpaceData() != null) {
            content = new String(spaceDocs.getWorkSpaceData(), StandardCharsets.UTF_8);
        }

        return new ViewProjectWorkSpaceDocs(
                spaceDocs.getTitle(),
                spaceDocs.getDescription(),
                content,
                spaceDocs.getProject().getTitle()
        );
    }

    public ActiveProjectWorkSpaceDocs workInSpaceDocs(UUID userId, Long docId, UpdateWorkSpaceDocsDto updateDto, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }


        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        String wrappedContent = injectContentIntoTemplate(updateDto.content(), docs.getDocType());

        docs.setWorkSpaceData(wrappedContent.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveProjectWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                wrappedContent,
                docs.getDocType(),
                docs.getProject().getTitle()
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


    // to download a single workspace docs
    public ResponseEntity<byte[]> downloadWorkSpaceDoc(Long docId, UUID userId, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs document = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!document.getUser().getId().equals(user.getId()) && !document.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", document.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        String mimeType = document.getDocType().getMimeType();
        String extension = document.getDocType().getExtension();
        String fileName = document.getTitle() + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentLength(document.getWorkSpaceData().length)
                .body(document.getWorkSpaceData());
    }


    // to delete a workspace doc
    public String deleteDocs(Long docId, UUID userId, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        workSpaceRepo.delete(docs);
        return "Document deleted successfully";
    }


    // continue working on workspace docs from where u left off
    public ActiveProjectWorkSpaceDocs continueWorkOnExistingDocs(UUID userId, UUID projectId, Long docId, UpdateWorkSpaceDocsDto updateDto){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + docId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException("Authorized access, this resource does not belong to user with id {}");
        }

        String existingContent = new String(docs.getWorkSpaceData(), StandardCharsets.UTF_8);

        // Inject the new content into the correct position within the existing document
        String updatedDocument = updateContentWithinTemplate(existingContent, updateDto.content(), docs.getDocType());
        docs.setWorkSpaceData(updatedDocument.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveProjectWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                updatedDocument,
                docs.getDocType(),
                docs.getProject().getTitle()
        );
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



    // zip and download all project workSpace docs
    public ResponseEntity<byte[]> projectWorkSpaceFolder(UUID userId, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findAll();

        // Filter docs belonging to the user
        List<WorkSpaceDocs> userDocs = docs.stream()
                .filter(doc -> doc.getUser().getId().equals(user.getId()) && doc.getProject().getId().equals(project.getId()) && doc.getProject().getUser().getId().equals(user.getId()))
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


    // to view project docs by type
    public List<ProjectWorkSpaceDocs> viewDocsByType(UUID userId, UUID projectId, DocType docType){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByProjectIdAndDocType(projectId, docType);
        if (docs == null){
            log.error("No project docs found for this project with id , {}", project.getId());
            return List.of();
        }

        return docsViewByType(docs);
    }

    private List<ProjectWorkSpaceDocs> docsViewByType(List<WorkSpaceDocs> docs){
        return docs.stream()
                // ✅ NULL CHECK ADDED
                .filter(doc -> doc.getWorkSpaceData() != null)
                .map(doc -> new ProjectWorkSpaceDocs(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getDescription(),
                        new String(doc.getWorkSpaceData(), StandardCharsets.UTF_8),
                        doc.getDocType(),
                        doc.getProject().getTitle()
                ))
                .collect(Collectors.toList());
    }


    // view most recent project space docs
    public List<ProjectWorkSpaceDocs> viewMostRecentProjectSpaceDocs(UUID userId, UUID projectId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findTop10ByProjectIdOrderByCreatedAtDesc(project.getId());
        if (docs == null){
            log.error("No project docs found for this project with id , {}", project.getId());
            return List.of();
        }

        return docsViewByType(docs);
    }


    // to search for docs
    public List<ProjectWorkSpaceDocs> searchProjectWorkSpaceDocs(UUID userId, UUID projectId, String keyword){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.searchByProjectAndKeyword(project.getId(), keyword);
        if (docs == null){
            log.error("No project docs found for this project with id , {}", project.getId());
            return List.of();
        }

        return docsViewByType(docs);
    }



    // project document space docs
    public ProjectDocSpaceDocsCreationResponse createWorkForDocs(UUID userId, UUID projectId,UUID documentId, WorkSpaceCreationDto dto){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        // to see if project belongs to the user
        if (!project.getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        // to check if the document exists
        ProjectDocument document = getDocId(documentId);
        if (document == null){
            log.error("Document with id {} not found", documentId);
            throw new RuntimeException("Document not found");
        }

        WorkSpaceDocs docs = new WorkSpaceDocs();
        docs.setProjectDocument(document);
        docs.setUser(user);
        docs.setProject(project);
        docs.setTitle(dto.title());
        docs.setDescription(dto.description());
        docs.setCreatedAt(LocalDateTime.now());

        workSpaceRepo.save(docs);

        return new ProjectDocSpaceDocsCreationResponse(
                docs.getTitle(),
                docs.getDescription(),
                docs.getProject().getTitle(),
                docs.getProjectDocument().getFileName()
        );
    }


    // get document workspace template docs
    public ActiveProjectDocumentWorkSpaceDocs getDocTemplate(DocType docType, UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id , {} not found,", userId);
            throw new UsernameNotFoundException("User does not exit. Check credentails and try again.");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
        }

        String template = "";
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
        docs.setUpdatedAt(LocalDateTime.now());
        docs.setLastViewed(LocalDateTime.now());
        docs.setDocType(docType);
        workSpaceRepo.save(docs);

        return new  ActiveProjectDocumentWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                template,
                docType,
                docs.getProject().getTitle(),
                docs.getProjectDocument().getFileName(),
                docs.getProjectDocument().getDescription(),
                docs.getProjectDocument().getUploadedBy().getUsername()
        );

    }


    // to view space docs
    public ViewWorkSpaceDocsData viewProjectDocSpaceDocs(UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {}, not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
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

    // to work in space Docs
    public ActiveProjectDocumentWorkSpaceDocs workInSpaceDocsforProjectDocument(UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId, UpdateWorkSpaceDocsDto updateDto){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {}, not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
        }

        String wrappedContent = injectContentIntoTemplate(updateDto.content(), docs.getDocType());
        docs.setWorkSpaceData(wrappedContent.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveProjectDocumentWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                wrappedContent,
                docs.getDocType(),
                docs.getProject().getTitle(),
                docs.getProjectDocument().getFileName(),
                docs.getProjectDocument().getDescription(),
                docs.getProjectDocument().getUploadedBy().getUsername()
        );
    }


    // to download file
    public ResponseEntity<byte[]> downloadProjectDocumentSpaceDocs(UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {}, not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
        }

        String mimeType = docs.getDocType().getMimeType();
        String extension = docs.getDocType().getExtension();
        String fileName = docs.getTitle() + extension;

        docs.setLastViewed(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentLength(docs.getWorkSpaceData().length)
                .body(docs.getWorkSpaceData());

    }


    // to delete project document work space docs
    public String deleteProjectDocSpaceDocs(UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {}, not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
        }

        workSpaceRepo.delete(docs);
        return "Document deleted successfully";
    }



    public ActiveProjectDocumentWorkSpaceDocs updateProjectDocSpaceDocs(UUID userId, UUID projectId, UUID projectDocId, Long spaceDocId, UpdateWorkSpaceDocsDto updateDto){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {}, not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist" + userId);
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        WorkSpaceDocs docs = workSpaceRepo.findById(spaceDocId)
                .orElseThrow(() -> new RuntimeException("Workspace with id " + spaceDocId + " not found"));

        if (!docs.getUser().getId().equals(user.getId()) && !docs.getProject().getId().equals(project.getId())){
            log.error("User with id {} does not belong to the user", docs.getUser().getId());
            throw new UnauthorizedException(" Authorized access, this resource does not belong to user with id {}" + user.getId());
        }

        // Read the existing full document from the DB
        String existingFullDocument = new String(docs.getWorkSpaceData(), StandardCharsets.UTF_8);

        // Inject the new content into the correct position within the existing document
        String updatedDocument = updateContentWithinTemplate(existingFullDocument, updateDto.content(), docs.getDocType());

        docs.setWorkSpaceData(updatedDocument.getBytes(StandardCharsets.UTF_8));
        docs.setLastViewed(LocalDateTime.now());
        docs.setUpdatedAt(LocalDateTime.now());
        workSpaceRepo.save(docs);

        return new ActiveProjectDocumentWorkSpaceDocs(
                docs.getId(),
                docs.getTitle(),
                docs.getDescription(),
                updatedDocument,
                docs.getDocType(),
                docs.getProject().getTitle(),
                docs.getProjectDocument().getFileName(),
                docs.getProjectDocument().getDescription(),
                docs.getProjectDocument().getUploadedBy().getUsername()
        );
    }


    // zip folder download of all
    public ResponseEntity<byte[]> projectDocumentWorkSpaceFolder(UUID userId, UUID projectId, UUID projectDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findAll();
        // Filter docs belonging to the user and project and project document
        List<WorkSpaceDocs> userDocs = docs.stream()
                .filter(doc -> doc.getUser().getId().equals(user.getId()) && doc.getProject().getId().equals(project.getId()) && doc.getProjectDocument().getId().equals(projectDocId) && doc.getProjectDocument().getId().equals(document.getId()) && doc.getProjectDocument().getProject().getUser().getId().equals(user.getId()))
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


    // to view most recent project documents
    public List<ProjectDocumentWorkSpaceDocs> viewMostRecentProjectDocuments(UUID userId, UUID projectId, UUID projectDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findTopByProjectDocumentOrderByCreatedAtDesc(document);
        if (docs.isEmpty()){
            return List.of();
        }

        return  projectDocView(docs);
    }

    private List<ProjectDocumentWorkSpaceDocs> projectDocView(List<WorkSpaceDocs> docs){
        return docs.stream()
                // ✅ NULL CHECK ADDED
                .filter(doc -> doc.getWorkSpaceData() != null)
                .map(doc -> new ProjectDocumentWorkSpaceDocs(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getDescription(),
                        new String(doc.getWorkSpaceData(), StandardCharsets.UTF_8),
                        doc.getDocType(),
                        doc.getProject().getTitle(),
                        doc.getProject().getDescription(),
                        doc.getProjectDocument().getFileName(),
                        doc.getProjectDocument().getDescription()
                ))
                .collect(Collectors.toList());
    }


    // now to view project document by docType
    public List<ProjectDocumentWorkSpaceDocs> viewProjectDocumentByDocType(UUID userId, UUID projectId, UUID projectDocId, DocType docType){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findByProjectDocumentIdAndDocType(document.getId(), docType);
        if (docs.isEmpty()){
            return List.of();
        }

        return projectDocView(docs);
    }


    // to view last viewed project documents
    public List<ProjectDocumentWorkSpaceDocs> viewLastViewedProjectDocuments(UUID userId, UUID projectId, UUID projectDocId){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.findTopByProjectDocumentOrderByLastViewedDesc(document);
        if (docs.isEmpty()){
            return List.of();
        }

        return projectDocView(docs);
    }

    // now to search for project document that is general search
    public List<ProjectDocumentWorkSpaceDocs> generalProjectDocumentSearch(UUID userId, UUID projectId, UUID projectDocId, String keyword){
        User user = getUserById(userId);
        if (user == null){
            log.error("User with id {} not found", userId);
            throw new UsernameNotFoundException("User with id {} does not exist");
        }

        Project project = getProjectId(projectId);
        if (project == null){
            log.error("Project with id {} not found", projectId);
            throw new RuntimeException("Project not found");
        }

        ProjectDocument document = getDocId(projectDocId);
        if (document == null){
            log.error("Document with id {} not found", projectDocId);
            throw new RuntimeException("Document not found");
        }

        // to see if project document belongs to the user and also belongs to the project
        if (!project.getUser().getId().equals(userId) && !document.getProject().getId().equals(project.getId()) && !document.getProject().getId().equals(projectId) && !document.getProject().getUser().getId().equals(userId)){
            log.error("Unauthorized project acccess, project with id {} does not belong to user", userId);
            throw new UnauthorizedException("Unauthorized project access, project with id {} does not belong to user");
        }

        List<WorkSpaceDocs> docs = workSpaceRepo.searchByProjectDocument(document.getId(), keyword);
        if (docs == null) {
            log.error("No project docs found for this project with id , {}", project.getId());
            return List.of();
        }

        return projectDocView(docs);
    }



}