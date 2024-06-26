package itss.group22.bookexchangeeasy.controller;

import io.swagger.v3.oas.annotations.Operation;
import itss.group22.bookexchangeeasy.dto.user.AddressUnitDTO;
import itss.group22.bookexchangeeasy.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping("/provinces")
    @Operation(summary = "Lấy danh sách tất cả tỉnh / thành")
    public ResponseEntity<List<AddressUnitDTO>> getProvinces() {
        return ResponseEntity.ok(addressService.getProvinces());
    }

    @GetMapping("/districts")
    @Operation(summary = "Lấy các quận / huyện thuộc một tỉnh / thành")
    public ResponseEntity<List<AddressUnitDTO>> getDistrictsByProvinceId(
            @RequestParam(name = "province-id", required = true) String provinceId) {
        return ResponseEntity.ok(addressService.getDistrictsByProvinceId(provinceId));
    }

    @GetMapping("/communes")
    @Operation(summary = "Lấy các phường / xã thuộc một quận / huyện")
    public ResponseEntity<List<AddressUnitDTO>> getCommunesByDistrictId(
            @RequestParam(name = "district-id", required = true) String districtId) {
        return ResponseEntity.ok(addressService.getCommunesByDistrictId(districtId));
    }
}
