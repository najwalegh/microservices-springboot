package com.microservices.orderservice.service;

import com.microservices.orderservice.dto.InventoryResponse;
import com.microservices.orderservice.dto.OrderLineItemsDto;
import com.microservices.orderservice.dto.OrderRequest;
import com.microservices.orderservice.event.OrderPlacedEvent;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.model.OrderLineItems;
import com.microservices.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// instead of create the constructor manually this annotation, will create the constructor automatically based on the parameters during the compiletime
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;
    public String placeOrder(OrderRequest orderRequest){
        Order order=new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItemsList=orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItemsList);

        List <String> skuCodes=order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        // call inventory service and place order if product is in stock
        InventoryResponse[] inventoryResponsesArray  =webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                                .retrieve()
                                .bodyToMono(InventoryResponse[].class)
                                .block();

        System.out.println("inventoryResponsesArray = " + Arrays.toString(inventoryResponsesArray));
        boolean allProductsInStock;
        if (inventoryResponsesArray.length == 0) {
            allProductsInStock = false; // or any other value you want to assign
        } else {
            allProductsInStock = Arrays.stream(inventoryResponsesArray)
                    .allMatch(InventoryResponse::isInStock);
        }



        if(allProductsInStock) {
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
            return  "order placed succefully";
        }else {
            throw new RuntimeException("Product is out of stock please try after some time");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems=new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
