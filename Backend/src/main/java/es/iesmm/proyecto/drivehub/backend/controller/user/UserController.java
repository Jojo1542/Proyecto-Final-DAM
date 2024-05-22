package es.iesmm.proyecto.drivehub.backend.controller.user;

import es.iesmm.proyecto.drivehub.backend.model.http.request.user.UserLocationUpdateRequest;
import es.iesmm.proyecto.drivehub.backend.model.http.request.user.UserModificationRequest;
import es.iesmm.proyecto.drivehub.backend.model.http.response.common.CommonResponse;
import es.iesmm.proyecto.drivehub.backend.model.user.UserModel;
import es.iesmm.proyecto.drivehub.backend.model.user.driver.license.DriverLicense;
import es.iesmm.proyecto.drivehub.backend.service.location.LocationService;
import es.iesmm.proyecto.drivehub.backend.service.user.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final LocationService locationService;

    @GetMapping("/me")
    @ResponseBody
    public UserModel me(@AuthenticationPrincipal UserDetails userDetails) {
        return (UserModel) userDetails;
    }

    @PostMapping("/update/main")
    public ResponseEntity<CommonResponse> updateData(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UserModificationRequest request) {
        UserModel user = (UserModel) userDetails;
        try {
            userService.updateUserByRequest(user, request);
            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            // Si algún campo no es válido, se tirará esta excepción que tendrá el mensaje de error en el campo
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    /*
     * Metodos usados únicamente por los administradores
     */
    @GetMapping("/list/all")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('GET_ALL_USERS')")
    public List<UserModel> listAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('GET_ALL_USERS')")
    public Page<UserModel> listAllUsersPaged(@RequestParam("page") int page,
                                             @RequestParam("size") int size,
                                             Pageable pageable) {
        return userService.findAllPaged(pageable);
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SEE_USER_DETAILS')")
    public UserModel getUser(@PathVariable Long id) {
        return userService.findById(id).orElse(null);
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('UPDATE_USER')")
    public ResponseEntity<CommonResponse> updateUser(@PathVariable Long id, @RequestBody UserModel request) {
        try {
            userService.updateUserByAdmin(id, request);

            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('DELETE_USER')")
    public ResponseEntity<CommonResponse> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteById(id);
            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    /*
     * Metodos de las licencias de conducir
     */
    @GetMapping("/licenses")
    public List<DriverLicense> getDriverLicenses(@AuthenticationPrincipal UserDetails userDetails) {
        UserModel user = (UserModel) userDetails;
        return userService.findDriverLicensesByDriver(user.getId());
    }

    @PostMapping("/licenses/add")
    public ResponseEntity<CommonResponse> addDriverLicense(@AuthenticationPrincipal UserDetails userDetails, @RequestBody DriverLicense license) {
        UserModel user = (UserModel) userDetails;
        try {
            userService.addDriverLicenseToDriver(user.getId(), license);
            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    @DeleteMapping("/licenses/remove/{licenseId}")
    public ResponseEntity<CommonResponse> removeDriverLicense(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String licenseId) {
        UserModel user = (UserModel) userDetails;
        try {
            userService.removeDriverLicenseFromDriver(user.getId(), licenseId);
            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('DRIVER_FLEET') or hasRole('DRIVER_CHAUFFEUR')")
    public ResponseEntity<CommonResponse> updateLocation(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UserLocationUpdateRequest request) {
        UserModel user = (UserModel) userDetails;
        try {
            System.out.println(request);
            locationService.save(user, request);
            return ResponseEntity.ok(
                    CommonResponse.builder()
                            .success(true)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonResponse.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }
}
