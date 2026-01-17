package com.security.service;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploadService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(S3UploadService.class);
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucket;
    
    @Value("${aws.s3.product-bucket}")
    private String productBucket; // order-app-product-api
    
    @Value("${aws.region}")
    private String region;
    
    public S3UploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    
   
  //  @Value("${aws.s3.product-bucket}")
 //   private String defaultBucket;
    
    @PostConstruct
    public void init() {
       /* if (defaultBucket == null || defaultBucket.isEmpty()) {
            throw new IllegalStateException("aws.s3.bucket is not configured!");
        }
        if (productBucket == null || productBucket.isEmpty()) {
            throw new IllegalStateException("aws.s3.product-bucket is not configured!");
        }*/
    }
    
    /**
     * ✅ Upload file to DEFAULT bucket (order-app-api)
     * Use for: user profile images, general files
     */
    public String uploadFile(MultipartFile file, String folder) {
        return uploadToSpecificBucket(file, folder, productBucket);
    }

    /**
     * ✅ Upload file to PRODUCT bucket (order-app-product-api)
     * Use for: product images
     */
    public String uploadProductImage(MultipartFile file) {
        return uploadToSpecificBucket(file, "products", productBucket);
    }

    /**
     * ✅ Upload to specific bucket (private method)
     */
    private String uploadToSpecificBucket(MultipartFile file, String folder, String bucketName) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            String key = folder + "/" + uniqueFilename;

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Return public URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

        } catch (IOException e) {
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Delete file from S3 (works for any bucket)
     * Automatically detects bucket from URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract bucket name and key from URL
            // URL format: https://bucket-name.s3.region.amazonaws.com/folder/file.jpg
            String[] urlParts = fileUrl.replace("https://", "").split("/", 2);
            String bucketName = urlParts[0].split("\\.")[0]; // Extract bucket name
            String key = urlParts[1]; // Extract key (folder/file.jpg)

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Check if file exists in S3
     */
    public boolean fileExists(String fileUrl) {
        try {
            String[] urlParts = fileUrl.replace("https://", "").split("/", 2);
            String bucketName = urlParts[0].split("\\.")[0];
            String key = urlParts[1];

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error checking file existence: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Get file size in bytes
     */
    public long getFileSize(String fileUrl) {
        try {
            String[] urlParts = fileUrl.replace("https://", "").split("/", 2);
            String bucketName = urlParts[0].split("\\.")[0];
            String key = urlParts[1];

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.contentLength();

        } catch (Exception e) {
            throw new RuntimeException("Error getting file size: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Copy file from one location to another within S3
     */
    public String copyFile(String sourceUrl, String destinationFolder, String destinationBucket) {
        try {
            // Parse source URL
            String[] urlParts = sourceUrl.replace("https://", "").split("/", 2);
            String sourceBucket = urlParts[0].split("\\.")[0];
            String sourceKey = urlParts[1];

            // Generate new key
            String fileName = sourceKey.substring(sourceKey.lastIndexOf("/") + 1);
            String destinationKey = destinationFolder + "/" + fileName;

            // Copy object
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(destinationBucket)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);

            // Return new URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s", destinationBucket, region, destinationKey);

        } catch (Exception e) {
            throw new RuntimeException("Error copying file: " + e.getMessage(), e);
        }
    }

    /**
     * Faz upload da foto de perfil do usuário para o S3
     * O nome do arquivo será o originalFilename do usuário
     * 
     * @param filename Nome completo do arquivo com extensão (ex: jose_200.png)
     * @param file Arquivo da imagem
     * @return URL pública da imagem no S3
     */
    public String uploadProfileImage(String filename, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        LOGGER.info("Nome do arquivo recebido: " + filename);
        
        LOGGER.info("ENV AWS_REGION=" + System.getenv("AWS_REGION"));
        LOGGER.info("ENV AWS_DEFAULT_REGION=" + System.getenv("AWS_DEFAULT_REGION"));
        
        // Validar tipo de arquivo
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/png") && 
             !contentType.equals("image/jpeg") && 
             !contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Apenas arquivos PNG, JPG e JPEG são permitidos");
        }
        
        // Validar tamanho (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo 5MB");
        }
        
        // ✅ CORREÇÃO: O filename JÁ VEM COM A EXTENSÃO CORRETA
        // NÃO adicionar extensão extra
        String key = "profile-images/" + filename;
        
        LOGGER.info("Key do S3: " + key);
        
        // Fazer upload
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        
        // Retornar URL pública
        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
        LOGGER.info("Upload concluído: " + url);
        
        return url;
    }
    
    public String uploadProductmage(String filename, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        LOGGER.info("Nome do arquivo product recebido: " + filename);
        
        // Validar tipo de arquivo
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/png") && 
             !contentType.equals("image/jpeg") && 
             !contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Apenas arquivos PNG, JPG e JPEG são permitidos");
        }
        
        // Validar tamanho (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo 5MB");
        }
        
        // ✅ CORREÇÃO: O filename JÁ VEM COM A EXTENSÃO CORRETA
        // NÃO adicionar extensão extra
        String key = "product-images/" + filename;
        
        LOGGER.info("Key do S3: " + key);
        
        // Fazer upload
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(productBucket)
                .key(key)
                .contentType(contentType)
                .build();
        
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        
        // Retornar URL pública
        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", productBucket, region, key);
        LOGGER.info("Upload product finished: " + url);
        
        return url;
    }
    
    /**
     * Deleta a foto de perfil do usuário do S3
     */
    public void deleteProfileImage(String filename) {
        try {
            String key = "profile-images/" + filename;
            s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
            LOGGER.info("Imagem deletada: " + key);
        } catch (Exception e) {
            LOGGER.error("Erro ao deletar imagem: " + e.getMessage());
        }
    }
}