package dev.ldworski.couponservice.adapter.in.web;

import dev.ldworski.couponservice.adapter.in.web.dto.RedeemCouponRequest;
import dev.ldworski.couponservice.domain.model.CouponCode;
import dev.ldworski.couponservice.domain.model.IpAddress;
import dev.ldworski.couponservice.domain.model.UserId;
import dev.ldworski.couponservice.domain.port.in.RedeemCouponUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons/{code}/redemptions")
public class CouponRedemptionsController implements CouponRedemptionsApi {

    private final RedeemCouponUseCase redeemCouponUseCase;

    CouponRedemptionsController(RedeemCouponUseCase redeemCouponUseCase) {
        this.redeemCouponUseCase = redeemCouponUseCase;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    // TODO walidacje na code
    public ResponseEntity<Void> redeemCoupon(
            @PathVariable String code, @RequestBody RedeemCouponRequest redeemCouponRequest) {
        var redeemCouponCommand = new RedeemCouponUseCase.RedeemCouponCommand(
                new CouponCode(code),
                new UserId(redeemCouponRequest.userId()),
                new IpAddress(redeemCouponRequest.ipAddress()));
        redeemCouponUseCase.redeemCoupon(redeemCouponCommand);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
