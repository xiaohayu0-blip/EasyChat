package com.gym.easychatjava.service.impl;

import com.gym.easychatjava.common.BusinessException;
import com.gym.easychatjava.common.ResultCode;
import com.gym.easychatjava.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public String uploadImage(MultipartFile file) {

        if(file==null||file.isEmpty()){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"文件不能为空");
        }

        String contentType=file.getContentType();

        if(contentType==null||!contentType.startsWith("image/")){
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),"只能上传图片文件");
        }

        String ext=switch(contentType){
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        String dir= LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName=dir+"/"+ UUID.randomUUID().toString().replace("-","")+"."+ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "文件上传失败");
        }

        return endpoint + "/" + bucket + "/" + objectName;
    }
}
