package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponRequest;
import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponResponse;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.CouponDefinition;
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
public class CouponsController {

    private final RegisterCouponUseCase registerCouponUseCase;

    CouponsController(RegisterCouponUseCase registerCouponUseCase) {
        this.registerCouponUseCase = registerCouponUseCase;
    }

    @PostMapping
    ResponseEntity<RegisterCouponResponse> registerCoupon(@RequestBody RegisterCouponRequest registerCouponRequest) {
        var registerCouponCommand = new RegisterCouponUseCase.RegisterCouponCodeCommand(new CouponDefinition(
                new CouponCode(registerCouponRequest.code()),
                registerCouponRequest.maxUsagesAllowed(),
                registerCouponRequest.countryCode()));
        var registerCouponId = registerCouponUseCase.registerCoupon(registerCouponCommand);
        return ResponseEntity.ok().body(new RegisterCouponResponse(registerCouponId));
    }
}
