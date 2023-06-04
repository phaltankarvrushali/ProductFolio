package io.swagger.api;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.configuration.PropertiesUtil;
import io.swagger.database.ImageRepository;
import io.swagger.database.ProductRepository;
import io.swagger.database.UserRepository;
import io.swagger.database.entities.Product;
import io.swagger.database.entities.User;
import io.swagger.model.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
public class ImageController {

    private static final StatsDClient statsd = new NonBlockingStatsDClient("webapp", "localhost", 8125);

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    ImageRepository imageRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @Value("${s3.bucketName}")
    String bucketname;
    /*
        @Value("${aws.accessKey}")
        String accessKey;

        @Value("${aws.secretKey}")
        private String secretKey;
    */
    @Value("${aws.region}")
    private String awsRegion;

    private static final String fileName = "fileName";
    private static final String s3BucketPath = "s3BucketPath";

    @RequestMapping(value = "/v4/product/{product_id}/image",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<Image>> getImagesList(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                     String authorization, @ApiParam(value = "Product Id ", required = true) @PathVariable("product_id") String productId) {
        statsd.increment("getListOfImages");
        ResponseEntity responseEntity = validationStep(authorization, productId, null, "GET");

        if (!HttpStatus.CONTINUE.equals(responseEntity.getStatusCode())) {
            return responseEntity;
        }

        logger.info("Get images for product {}", productId);
        List<io.swagger.database.entities.Image> retrievedImagesList = imageRepository.getByProductId(productId);

        List<Image> responseImageList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(retrievedImagesList)) {
            for (io.swagger.database.entities.Image image : retrievedImagesList) {
                responseImageList.add(responseMapper(image));
            }
        }
        else
        {
            logger.error("No Images found");
            return new ResponseEntity("No Images Found for the product", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Image>>(responseImageList, HttpStatus.OK);
    }

    @RequestMapping(value = "/v4/product/{product_id}/image/{image_id}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Image> getImage(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                          String authorization, @ApiParam(value = "Product Id ", required = true) @PathVariable("product_id") String productId, @ApiParam(value = "Image Id ", required = true) @PathVariable("image_id") String imageId) {
        statsd.increment("getImage");
        ResponseEntity responseEntity = validationStep(authorization, productId, null, "GET");

        if (!HttpStatus.CONTINUE.equals(responseEntity.getStatusCode())) {
            return responseEntity;
        }

        logger.info("get Image for id {}", imageId);
        io.swagger.database.entities.Image image = imageRepository.get(imageId);

        if (null == image) {
            return new ResponseEntity("Image Doesn't exist", HttpStatus.NOT_FOUND);
        }

        if (!String.valueOf(image.getProductId()).equals(productId)) {
            return new ResponseEntity("Image doesnot belong to the product", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<Image>(responseMapper(image), HttpStatus.OK);
    }

    @RequestMapping(value = "/v4/product/{product_id}/image/{image_id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteImage(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                            String authorization, @ApiParam(value = "Product Id ", required = true) @PathVariable("product_id") String productId, @ApiParam(value = "Image Id ", required = true) @PathVariable("image_id") String imageId) {

        statsd.increment("deleteImage");
        ResponseEntity responseEntity = validationStep(authorization, productId, null, "DELETE");

        logger.info("Deleting image for image Id: {}", imageId);
        if (!HttpStatus.CONTINUE.equals(responseEntity.getStatusCode())) {
            return responseEntity;
        }

        io.swagger.database.entities.Image image = imageRepository.get(imageId);

        if (null == image) {
            return new ResponseEntity("Image Doesn't exist", HttpStatus.NOT_FOUND);
        }

        if (!String.valueOf(image.getProductId()).equals(productId)) {
            return new ResponseEntity("Image doesnot belong to the product", HttpStatus.BAD_REQUEST);
        }

        try {
            deleteImageFromS3BucketStep(image);
            imageRepository.delete(image);
        }
        catch (Exception e)
        {
            return new ResponseEntity("Bucket Not Found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    private void deleteImageFromS3BucketStep(io.swagger.database.entities.Image image) {
        String bucketPath = image.getS3BucketPath();
        URI uri = URI.create(bucketPath);
        String bucketName = uri.getHost();
        String objectKey = uri.getPath().substring(1);

        Map<String, String> s3Properties = PropertiesUtil.getS3Properties();
        AmazonS3 client = getS3Client(s3Properties);

        DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, objectKey);
        client.deleteObject(deleteRequest);
    }

    @RequestMapping(value = "/v4/product/{product_id}/image",
            produces = {"application/json"},
            //consumes = { "application/octet-stream" },
            method = RequestMethod.POST)
    public ResponseEntity<Image> createImages(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                              String authorization, @ApiParam(value = "Product Id ", required = true) @PathVariable("product_id") String productId,
                                              @ApiParam(value = "Image File to be uploaded", required = true) @Valid @RequestBody MultipartFile file) {
        statsd.increment("createImage");
        ResponseEntity responseEntity = validationStep(authorization, productId, file, "POST");

        if (!HttpStatus.CONTINUE.equals(responseEntity.getStatusCode())) {
            return responseEntity;
        }

        Product product = (Product) responseEntity.getBody();
        io.swagger.database.entities.Image image = new io.swagger.database.entities.Image();
        try {
            Map<String, String> resultMap = uploadImageStep(file);

            image.setFileName(resultMap.get(fileName));
            image.setProductId(product.getId());
            image.setS3BucketPath(resultMap.get(s3BucketPath));
            image.setDateCreated(String.valueOf(OffsetDateTime.now()));

            imageRepository.save(image);
        } catch (Exception e) {
            return new ResponseEntity("S3 Bucket not found", HttpStatus.NOT_FOUND);
        }

        logger.info("Created new Image with Image Id: {}", image.getImageId());

        return new ResponseEntity<Image>(responseMapper(image), HttpStatus.CREATED);
    }

    private ResponseEntity validationStep(String authorization, String productId, MultipartFile file, String method) {
        if ("POST".equalsIgnoreCase(method) && (null == file)) {
            return new ResponseEntity("Missing Image file", HttpStatus.BAD_REQUEST);
        }
        if("POST".equalsIgnoreCase(method))
        {
            String fileName = file.getOriginalFilename();
            String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);

            if(!(fileType.equalsIgnoreCase("png") || fileType.equalsIgnoreCase("jpeg")
                    || fileType.equalsIgnoreCase("jpg")))
            {
                return new ResponseEntity("Unsupported file format", HttpStatus.BAD_REQUEST);
            }
        }

        return productValidationStep(authorization, productId);
    }

    private ResponseEntity productValidationStep(String authorization, String productId) {
        Product product = productRepository.get(Long.valueOf(productId));

        ResponseEntity responseEntity = userValidationStep(authorization);

        if (!responseEntity.getStatusCode().equals(HttpStatus.CONTINUE)) {
            return responseEntity;
        }

        User user = (User) responseEntity.getBody();

        io.swagger.database.entities.Product retrievedProduct = productRepository.get(Long.valueOf(productId));

        if (null == retrievedProduct) {
            return new ResponseEntity("No Product exists with given Id", HttpStatus.NOT_FOUND);
        }

        if (Long.valueOf(retrievedProduct.getOwnerUserId()) != user.getId()) {
            return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity(product, HttpStatus.CONTINUE);
    }

    private ResponseEntity userValidationStep(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        String[] creds = getCredentials(authorization);

        if (StringUtils.isEmpty(userRepository.getUserWithUsername(creds[0]))) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userRepository.getUserWithUsername(creds[0]));
        User user = userRepository.get(userId);

        ResponseEntity responseEntity = authenticate(creds, user);

        return responseEntity;
    }

    private ResponseEntity authenticate(String[] creds, User retrieveUser) {

        if (null == retrieveUser) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        String pass = creds[1];

        if (!StringUtils.hasText(pass)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(pass, retrieveUser.getPassword())) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity(retrieveUser, HttpStatus.CONTINUE);
    }

    private Map<String, String> uploadImageStep(MultipartFile file) throws IOException {
        Map<String, String> s3Properties = PropertiesUtil.getS3Properties();
        Map<String, String> resultMap = new HashMap<>();

        AmazonS3 client = getS3Client(s3Properties);

        UUID uuid = UUID.randomUUID();
        String randomString = uuid.toString().substring(0, 8);
        String imageName = "Image_" + randomString;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketname)
                .build();
        String fileName1 = file.getOriginalFilename();
        String fileType = fileName1.substring(fileName1.lastIndexOf(".") + 1);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/"+fileType);
        metadata.setContentLength(file.getBytes().length);

        PutObjectResult putObjectResult = client.putObject(bucketname, imageName, new ByteArrayInputStream(file.getBytes()), metadata);

        String bucketPath = String.format("s3://%s/%s", bucketname, imageName);


        resultMap.put(fileName, imageName);
        resultMap.put(s3BucketPath, bucketPath);

        return resultMap;
    }

    private AmazonS3 getS3Client(Map<String, String> s3Properties) {
        //AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion)
                // .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    private String[] getCredentials(String encodedString) {
        String[] auth = encodedString.split(" ");

        byte[] decodedBytes = Base64.getDecoder().decode(auth[1]);
        String decodedString = new String(decodedBytes);
        String[] creds = decodedString.split(":");

        return creds;
    }

    private Image responseMapper(io.swagger.database.entities.Image image) {
        Image responseImage = new Image();

        responseImage.setImageId(String.valueOf(image.getImageId()));
        responseImage.setDateCreated(image.getDateCreated());
        responseImage.setFileName(image.getFileName());
        responseImage.setProductId(String.valueOf(image.getProductId()));
        responseImage.setS3BucketPath(image.getS3BucketPath());

        return responseImage;
    }
}
