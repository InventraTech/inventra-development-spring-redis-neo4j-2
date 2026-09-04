package com.inventra.api.infrastructure.client.openfoodfacts;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventra.api.infrastructure.client.openfoodfacts.model.OpenFoodFactsProduct;

// API pública da Open Food Facts: sem autenticação, mas exige User-Agent identificando o app.
// status == 1 e "product" presente = achou; status == 0 = barcode não encontrado/inválido.
@Component
public class OpenFoodFactsClient {

    private static final String BASE_URL = "https://world.openfoodfacts.org/api/v2/product";
    private static final String USER_AGENT = "InventraAPI/1.0 (projeto academico Inventra)";
    private static final String FIELDS = "code,status,status_verbose,product_name,brands,image_url,quantity";

    private final RestClient restClient = RestClient.create();

    public Optional<OpenFoodFactsProduct> findByBarcode(String barcode) {
        RawResponse response = restClient.get()
                .uri(BASE_URL + "/{barcode}.json?fields={fields}", barcode, FIELDS)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .body(RawResponse.class);

        if (response == null || response.status() != 1 || response.product() == null) {
            return Optional.empty();
        }

        RawProduct product = response.product();
        return Optional.of(new OpenFoodFactsProduct(
                product.productName(),
                product.brands(),
                product.imageUrl(),
                product.quantity()
        ));
    }

    private record RawResponse(
            int status,
            @JsonProperty("status_verbose") String statusVerbose,
            RawProduct product
    ) {
    }

    private record RawProduct(
            @JsonProperty("product_name") String productName,
            String brands,
            @JsonProperty("image_url") String imageUrl,
            String quantity
    ) {
    }
}
