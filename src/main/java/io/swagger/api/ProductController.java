package io.swagger.api;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.swagger.annotations.ApiParam;
import io.swagger.database.ImageRepository;
import io.swagger.database.ProductRepository;
import io.swagger.database.UserRepository;
import io.swagger.database.entities.Image;
import io.swagger.database.entities.User;
import io.swagger.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProductController {
    private static final StatsDClient statsd = new NonBlockingStatsDClient("webapp", "localhost", 8125);

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @Autowired
    ProductRepository productRepository;

    @Autowired
    ImageController imageController;

    @Autowired
    ImageRepository imageRepository;

    @Autowired
    UserRepository userRepository;

    @RequestMapping(value = "/v4/product/{productId}",
            method = RequestMethod.PATCH)
    public ResponseEntity<io.swagger.model.Product> updateProduct(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                                  String authorization,@ApiParam(value = "Unique identifier of the product ",required=true ) @PathVariable("productId") String productId, @ApiParam(value = "Product to be Created or Updated" ,required=true )  @Valid @RequestBody io.swagger.database.entities.Product product) {
        statsd.increment("updateProduct");
        if (!StringUtils.hasText(authorization)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        String[] creds = getCredentials(authorization);
        if(StringUtils.isEmpty(userRepository.getUserWithUsername(creds[0])))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userRepository.getUserWithUsername(creds[0]));
        User user = userRepository.get(userId);

        ResponseEntity responseEntity = authenticate(creds, user);

        if(responseEntity.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        io.swagger.database.entities.Product retrievedProduct = productRepository.get(Long.valueOf(productId));

        if(null == retrievedProduct)
        {
            return new ResponseEntity("No Product exists with given Id", HttpStatus.NOT_FOUND);
        }

        if(Long.valueOf(retrievedProduct.getOwnerUserId()) != user.getId())
        {
            return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
        }

        if(null != product.getId() || StringUtils.hasText(product.getDateAdded())
                || StringUtils.hasText(product.getDateLastUpdated()) || null != product.getOwnerUserId())
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        if(StringUtils.isEmpty(product.getName()) && StringUtils.isEmpty(product.getDescription())
                && StringUtils.isEmpty(product.getSku()) && StringUtils.isEmpty(product.getManufacturer())
                && StringUtils.isEmpty(product.getQuantity()))
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        if(StringUtils.hasText(product.getName()))
        {
            retrievedProduct.setName(product.getName());
        }
        if(StringUtils.hasText(product.getDescription()))
        {
            retrievedProduct.setDescription(product.getDescription());
        }
        if(StringUtils.hasText(product.getSku()))
        {
            String existingProductId = productRepository.getProductWithsku(product.getSku());
            if(StringUtils.hasText(existingProductId) && Integer.valueOf(existingProductId)!=Math.toIntExact(retrievedProduct.getId()))
            {
                logger.error("SKU already exist, use new SKU");
                return new ResponseEntity("SKU already exist", HttpStatus.BAD_REQUEST);
            }
            retrievedProduct.setSku(product.getSku());
        }
        if(StringUtils.hasText(product.getManufacturer()))
        {
            retrievedProduct.setManufacturer(product.getManufacturer());
        }
        if(StringUtils.hasText(product.getQuantity()))
        {
            try {
                if (Integer.valueOf(product.getQuantity()) < -1 || Integer.valueOf(product.getQuantity()) > 101) {
                    logger.error("Quantity must be greater than zero and less than 100");
                    return new ResponseEntity("Quantity must be greater than zero and less than 100", HttpStatus.BAD_REQUEST);
                }
            } catch(NumberFormatException e) {
                return new ResponseEntity("Quantity must be a number", HttpStatus.BAD_REQUEST);
            }
            retrievedProduct.setQuantity(product.getQuantity());
        }
        retrievedProduct.setDateLastUpdated(String.valueOf(OffsetDateTime.now()));
        productRepository.save(retrievedProduct);
        logger.info("Product with product Id {} updated",productId);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/v4/product/{productId}",
            method = RequestMethod.PUT)
    public ResponseEntity<io.swagger.model.Product> patchProduct(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                                 String authorization,@ApiParam(value = "Unique identifier of the User ",required=true ) @PathVariable("productId") String productId, @ApiParam(value = "Product to be Created or Updated" ,required=true )  @Valid @RequestBody Product product) {
        statsd.increment("patchProduct");
        if (!StringUtils.hasText(authorization)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        String[] creds = getCredentials(authorization);
        if(StringUtils.isEmpty(userRepository.getUserWithUsername(creds[0])))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userRepository.getUserWithUsername(creds[0]));
        User user = userRepository.get(userId);

        ResponseEntity responseEntity = authenticate(creds, user);

        if(responseEntity.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        io.swagger.database.entities.Product retrievedProduct = productRepository.get(Long.valueOf(productId));

        if(null == retrievedProduct)
        {
            return new ResponseEntity("No Product exists with given Id", HttpStatus.NOT_FOUND);
        }

        if(Long.valueOf(retrievedProduct.getOwnerUserId()) != user.getId())
        {
            return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
        }

        if(StringUtils.isEmpty(product.getName()) || StringUtils.isEmpty(product.getDescription())
                || StringUtils.isEmpty(product.getSku()) || StringUtils.isEmpty(product.getManufacturer())
                || StringUtils.isEmpty(product.getQuantity()))
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        if(null != product.getId() || StringUtils.hasText(product.getDateAdded())
                || StringUtils.hasText(product.getDateLastUpdated()) || null != product.getOwnerUserId())
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        try {
            if (Integer.valueOf(product.getQuantity()) <= 0 || Integer.valueOf(product.getQuantity()) >= 100) {
                return new ResponseEntity("Quantity must be greater than zero and less than 100", HttpStatus.BAD_REQUEST);
            }
        } catch(NumberFormatException e) {
            return new ResponseEntity("Quantity must be a number", HttpStatus.BAD_REQUEST);
        }
        String existingProductId = productRepository.getProductWithsku(product.getSku());
        if(StringUtils.hasText(existingProductId) && Integer.valueOf(existingProductId)!=Math.toIntExact(retrievedProduct.getId()))
        {
            return new ResponseEntity("SKU already exist", HttpStatus.BAD_REQUEST);
        }
        retrievedProduct.setDescription(product.getDescription());
        retrievedProduct.setManufacturer(product.getManufacturer());
        retrievedProduct.setName(product.getName());
        retrievedProduct.setSku(product.getSku());
        retrievedProduct.setQuantity(product.getQuantity());
        retrievedProduct.setDateLastUpdated(String.valueOf(OffsetDateTime.now()));

        logger.info("Product with Product Id {} patched successfully", productId);
        productRepository.save(retrievedProduct);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/v4/product/{productId}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Product> deleteProduct(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                 String authorization, @ApiParam(value = "Unique identifier of the User ",required=true ) @PathVariable("productId") String productId) {

        statsd.increment("deleteProduct");
        if (!StringUtils.hasText(authorization)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        String[] creds = getCredentials(authorization);

        if(StringUtils.isEmpty(userRepository.getUserWithUsername(creds[0])))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userRepository.getUserWithUsername(creds[0]));
        User user = userRepository.get(userId);



        ResponseEntity responseEntity = authenticate(creds, user);

        if(responseEntity.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        io.swagger.database.entities.Product retrievedProduct = productRepository.get(Long.valueOf(productId));

        if(null == retrievedProduct)
        {
            return new ResponseEntity("No Product exists with given Id", HttpStatus.NOT_FOUND);
        }

        if(Long.valueOf(retrievedProduct.getOwnerUserId()) != user.getId())
        {
            return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);
        }

        List<Image> imageList = imageRepository.getByProductId(String.valueOf(retrievedProduct.getId()));

        if(!CollectionUtils.isEmpty(imageList))
        {
            for(Image image : imageList) {
                imageController.deleteImage(authorization, String.valueOf(retrievedProduct.getId()), String.valueOf(image.getImageId()));
            }
        }

        logger.info("Product {} deleted successfully", retrievedProduct.getId());
        productRepository.delete(retrievedProduct);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/v4/product",
            method = RequestMethod.POST)
    public ResponseEntity<Product> createProduct(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                 String authorization, @ApiParam(value = "Product to be Created or Updated", required = true) @Valid @RequestBody Product product) {

        statsd.increment("createProduct");
        if (!StringUtils.hasText(authorization)) {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        String[] creds = getCredentials(authorization);

        if(StringUtils.isEmpty(userRepository.getUserWithUsername(creds[0])))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(userRepository.getUserWithUsername(creds[0]));
        User user = userRepository.get(userId);

        ResponseEntity responseEntity = authenticate(creds, user);

        if(responseEntity.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
        {
            return new ResponseEntity("Unauthorised", HttpStatus.UNAUTHORIZED);
        }

        if(StringUtils.isEmpty(product.getName()) || StringUtils.isEmpty(product.getDescription())
                || StringUtils.isEmpty(product.getSku()) || StringUtils.isEmpty(product.getManufacturer())
                || StringUtils.isEmpty(product.getQuantity()))
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        try {
            if (Integer.valueOf(product.getQuantity()) <= 0 || Integer.valueOf(product.getQuantity()) >= 100) {
                return new ResponseEntity("Quantity must be greater than zero and less than 100", HttpStatus.BAD_REQUEST);
            }
        } catch(NumberFormatException e) {
            return new ResponseEntity("Quantity must be a number", HttpStatus.BAD_REQUEST);
        }

        if(null != product.getId() || StringUtils.hasText(product.getDateAdded())
                || StringUtils.hasText(product.getDateLastUpdated()) || null != product.getOwnerUserId())
        {
            return new ResponseEntity("Invalid Request Body.", HttpStatus.BAD_REQUEST);
        }

        String existingProductId = productRepository.getProductWithsku(product.getSku());
        if(StringUtils.hasText(existingProductId))
        {
            return new ResponseEntity("SKU already exist", HttpStatus.BAD_REQUEST);
        }

        io.swagger.database.entities.Product product1 = new io.swagger.database.entities.Product();

        product1.setDescription(product.getDescription());
        product1.setName(product.getName());
        product1.setManufacturer(product.getManufacturer());
        product1.setSku(product.getSku());
        product1.setDateAdded(String.valueOf(OffsetDateTime.now()));
        product1.setDateLastUpdated(String.valueOf(OffsetDateTime.now()));
        product1.setOwnerUserId(String.valueOf(user.getId()));
        product1.setQuantity(product.getQuantity());

        logger.info("Product Created successfully with Id {}", product1.getId());
        productRepository.save(product1);

        return new ResponseEntity<Product>(responseMapper(product1),HttpStatus.CREATED);
    }

    @RequestMapping(value = "/v4/product/{productId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Product> getProduct(@ApiParam(value = "Unique identifier of the product ", required = true) @PathVariable("productId") String productId) {
        statsd.increment("getProduct");
        try {
            io.swagger.database.entities.Product retrievedProduct = null;
            retrievedProduct = productRepository.get(Long.valueOf(productId));

            Product responseProduct = null;
            if (null != retrievedProduct) {
                responseProduct = responseMapper(retrievedProduct);
            } else {
                logger.error("No Product with given Id exists");
                return new ResponseEntity("No Product exists with given Id", HttpStatus.NOT_FOUND);
            }

            logger.info("Retrieved Product successfully for Id: {}", responseProduct.getId());

            return new ResponseEntity<Product>(responseProduct, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Bad Gateway", HttpStatus.BAD_GATEWAY);
        }
    }

    private Product responseMapper(io.swagger.database.entities.Product product) {
        Product responseProduct = new Product();

        responseProduct.setId(product.getId());
        responseProduct.setName(product.getName());
        responseProduct.setDescription(product.getDescription());
        responseProduct.setManufacturer(product.getManufacturer());
        responseProduct.setSku(product.getSku());
        responseProduct.setDateAdded(product.getDateAdded());
        responseProduct.setDateLastUpdated(product.getDateLastUpdated());
        responseProduct.setOwnerUserId(product.getOwnerUserId());
        responseProduct.setQuantity(product.getQuantity());

        return responseProduct;
    }

    private String[] getCredentials(String encodedString)
    {
        String[] auth = encodedString.split(" ");

        byte[] decodedBytes = Base64.getDecoder().decode(auth[1]);
        String decodedString = new String(decodedBytes);
        String[] creds = decodedString.split(":");

        return creds;
    }

    private ResponseEntity authenticate(String[] creds, User retrieveUser)
    {

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

        return new ResponseEntity(HttpStatus.OK);
    }
}
