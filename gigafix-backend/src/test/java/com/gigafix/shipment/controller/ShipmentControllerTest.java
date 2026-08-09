package com.gigafix.shipment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.gigafix.shipment.dto.request.CreateShipmentRequest;
import com.gigafix.shipment.dto.response.ShipmentResponse;
import com.gigafix.shipment.enums.ShippingMethod;
import com.gigafix.shipment.enums.ShippingStatus;
import com.gigafix.shipment.exception.DuplicateShipmentException;
import com.gigafix.shipment.exception.ShipmentExceptionHandler;
import com.gigafix.shipment.exception.ShipmentNotFoundException;
import com.gigafix.shipment.service.ShipmentService;
import com.gigafix.order.enums.OrderType;

@WebMvcTest(ShipmentController.class) @Import(ShipmentExceptionHandler.class) @ActiveProfiles("test")
/** 驗證物流 API 的請求驗證、ownership 錯誤與公開端點範圍。 */
class ShipmentControllerTest {
	@Autowired MockMvc mvc; @MockitoBean ShipmentService service;
	@Test void postReturnsCreated() throws Exception {var req=request();when(service.createShipment(1L,10L,req)).thenReturn(response());mvc.perform(post("/api/members/1/orders/10/shipment").contentType(MediaType.APPLICATION_JSON).content("{\"receiverName\":\"王小明\",\"receiverPhone\":\"0912\",\"receiverAddress\":\"台北市\",\"shippingMethod\":\"HOME_DELIVERY\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.orderType").value("GENERAL")).andExpect(jsonPath("$.shippingStatus").value("PREPARING"));}
	@Test void getReturnsOk() throws Exception {when(service.getShipment(1L,10L)).thenReturn(response());mvc.perform(get("/api/members/1/orders/10/shipment")).andExpect(status().isOk()).andExpect(jsonPath("$.receiverAddress").value("台北市"));}
	@Test void invalidFieldsReturnBadRequest() throws Exception {String[] bodies={"{\"receiverName\":\"\",\"receiverPhone\":\"0912\",\"receiverAddress\":\"台北市\",\"shippingMethod\":\"HOME_DELIVERY\"}","{\"receiverName\":\"王\",\"receiverPhone\":\"\",\"receiverAddress\":\"台北市\",\"shippingMethod\":\"HOME_DELIVERY\"}","{\"receiverName\":\"王\",\"receiverPhone\":\"0912\",\"receiverAddress\":\"\",\"shippingMethod\":\"HOME_DELIVERY\"}","{\"receiverName\":\"王\",\"receiverPhone\":\"0912\",\"receiverAddress\":\"台北市\",\"shippingMethod\":null}"};for(String body:bodies)mvc.perform(post("/api/members/1/orders/10/shipment").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());}
	@Test void ownershipReturnsNotFound() throws Exception {when(service.getShipment(1L,10L)).thenThrow(new ShipmentNotFoundException("找不到物流紀錄"));mvc.perform(get("/api/members/1/orders/10/shipment")).andExpect(status().isNotFound());}
	@Test void duplicateReturnsConflict() throws Exception {var req=request();when(service.createShipment(1L,10L,req)).thenThrow(new DuplicateShipmentException(10L));mvc.perform(post("/api/members/1/orders/10/shipment").contentType(MediaType.APPLICATION_JSON).content("{\"receiverName\":\"王小明\",\"receiverPhone\":\"0912\",\"receiverAddress\":\"台北市\",\"shippingMethod\":\"HOME_DELIVERY\"}")).andExpect(status().isConflict());}
	@Test void internalStatusEndpointsAreNotExposed() throws Exception {mvc.perform(patch("/api/members/1/orders/10/shipment/shipped")).andExpect(status().isNotFound());mvc.perform(patch("/api/members/1/orders/10/shipment/delivered")).andExpect(status().isNotFound());}
	private CreateShipmentRequest request(){return new CreateShipmentRequest("王小明","0912","台北市",ShippingMethod.HOME_DELIVERY);}
	private ShipmentResponse response(){return new ShipmentResponse(30L,10L,OrderType.GENERAL,"王小明","0912","台北市",ShippingMethod.HOME_DELIVERY,null,ShippingStatus.PREPARING,null,null,null,null);}
}
