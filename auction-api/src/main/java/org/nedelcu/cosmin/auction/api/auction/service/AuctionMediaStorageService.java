package org.nedelcu.cosmin.auction.api.auction.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuctionMediaStorageService {

    private final Path uploadRoot;

    public AuctionMediaStorageService(@Value("${app.media.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<String> storeAuctionImages(Long auctionId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        try {
            Path auctionDir = uploadRoot.resolve("auction-images").resolve(String.valueOf(auctionId));
            Files.createDirectories(auctionDir);

            List<String> storedPaths = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String extension = extractExtension(file.getOriginalFilename());
                String filename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
                Path target = auctionDir.resolve(filename);

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }

                storedPaths.add("/media/auction-images/" + auctionId + "/" + filename);
            }

            return storedPaths;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded auction images", ex);
        }
    }

    public void deleteStoredImages(List<String> storedPaths) {
        for (String storedPath : storedPaths) {
            if (storedPath == null || storedPath.isBlank() || !storedPath.startsWith("/media/")) {
                continue;
            }

            String relativePath = storedPath.substring("/media/".length());
            Path target = uploadRoot.resolve(relativePath).normalize();

            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Best effort cleanup for rolled back create operations.
            }
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFKC).trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }

        return normalized.substring(dotIndex + 1)
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
    }
}
