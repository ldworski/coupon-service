package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponRequest;
import dev.ldworski.couponservice.adapter.in.web.dto.RegisterCouponResponse;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.port.in.RegisterCouponUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
public class CouponsController implements CouponsApi {

    private final RegisterCouponUseCase registerCouponUseCase;

    CouponsController(RegisterCouponUseCase registerCouponUseCase) {
        this.registerCouponUseCase = registerCouponUseCase;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterCouponResponse> registerCoupon(
            @Valid @RequestBody RegisterCouponRequest registerCouponRequest) {
        var registerCouponCommand = new RegisterCouponUseCase.RegisterCouponCodeCommand(
                new CouponCode(registerCouponRequest.code()),
                registerCouponRequest.countryCode(),
                registerCouponRequest.maxUsagesAllowed());
        var registerCouponId = registerCouponUseCase.registerCoupon(registerCouponCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterCouponResponse(registerCouponId.value()));
    }
}
