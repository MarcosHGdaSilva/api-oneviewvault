package br.com.fiap.apioneviewvault;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class Controller {

    record FileResponse(UUID link) {}

    @Autowired
    FileRepository fileRepository;

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) throws MalformedURLException {
        FileStorage fileStorage = fileRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Arquivo não encontrado no bd")
        );

        Path filePath = Paths.get("src/main/resources/static/files/" + fileStorage.getPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Arquivo não encontrado ou não pode ser lido");
        }

        fileRepository.delete(fileStorage);

        String contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileStorage.getPath() + "\"")
                .body(resource);
    }

    @GetMapping("file")
    public List<FileStorage> listFiles() {
        return fileRepository.findAll();
    }

    @GetMapping("/{id}")
    public FileStorage GetById(@PathVariable UUID id) throws MalformedURLException {
        FileStorage fileStorage = fileRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Arquivo não encontrado no bd"));

        Path filePath = Paths.get("src/main/resources/static/files/" + fileStorage.getPath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Arquivo não encontrado ou não pode ser lido");
        }
        
        return fileStorage;
    }

    @PostMapping("file")
    public FileResponse uploadFile(@RequestBody MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("O arquivo está vazio");
        }
        Path path = Path.of("src/main/resources/static/files/");
        if (!Files.exists(path)) {
            try {
            Files.createDirectories(path);
            } catch (Exception e) {
            throw new RuntimeException("Erro ao criar o diretório", e);
            }
        }
        Path destinationFile = path.resolve(Paths.get(System.currentTimeMillis() + file.getOriginalFilename()))
                .normalize().toAbsolutePath();

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, destinationFile);
            System.out.println("Arquivo copiado");

            var imgURL = destinationFile.getFileName().toString();
            FileStorage fileStorage = new FileStorage();
            fileStorage.setPath(imgURL);
            fileStorage.setId(UUID.randomUUID());
            fileRepository.save(fileStorage);
            FileResponse fileResponse = new FileResponse(fileStorage.getId());
        
            return  fileResponse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar o arquivo", e);
        }
    }

}
